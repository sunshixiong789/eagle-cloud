package com.eagle.amqp.config;

import com.eagle.amqp.listener.AbstractAmqpListener;
import com.eagle.amqp.listener.AbstractDlqListener;
import com.eagle.amqp.properties.AmqpProperties;
import com.eagle.amqp.support.AmqpMessageDispatcher;
import com.eagle.amqp.support.EagleRepublishRecoverer;
import com.eagle.amqp.support.ExchangeNaming;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.DirectMessageListenerContainer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;

import java.util.ArrayList;
import java.util.List;

/**
 * 启动期遍历容器内所有 {@link AbstractAmqpListener} bean，声明 AMQP 拓扑并为每个 listener
 * 建一个监听容器。
 *
 * <p>之所以不用 {@code @RabbitListener} 注解：exchange / queue 名是运行时由
 * {@code getTopic()} + {@code getConsumerGroup()} 决定的，不是注解能接受的编译期常量。
 * 这个设计同时保留了原来"listener 是普通 {@code @Component}、自注册"的使用方式，
 * 43 个既有子类不需要加任何注解。
 *
 * <p>为每个主 listener 声明的拓扑：
 * <pre>
 * TopicExchange  {prefix}{topic}
 * Queue          {prefix}{topic}.{group}        （带 x-dead-letter-exchange 指向 DLX）
 * Binding        queue → exchange, key = routingKey
 * TopicExchange  {prefix}{topic}.dlx
 * Queue          {prefix}{topic}.{group}.dlq
 * Binding        dlq → dlx, key = queue 名
 * </pre>
 *
 * @author eagle
 */
@Slf4j
@RequiredArgsConstructor
public class AmqpListenerRegistrar implements SmartInitializingSingleton, DisposableBean {

    private final ObjectProvider<AbstractAmqpListener<?>> listeners;
    private final ConnectionFactory connectionFactory;
    private final RabbitAdmin rabbitAdmin;
    private final AmqpProperties properties;
    private final AmqpMessageDispatcher dispatcher;
    private final RabbitTemplate rabbitTemplate;

    private final List<DirectMessageListenerContainer> containers = new ArrayList<>();

    @Override
    public void afterSingletonsInstantiated() {
        listeners.stream().forEach(this::register);
        log.info("[AMQP] {} listener container(s) started", containers.size());
    }

    /**
     * 为单个 listener 声明拓扑并启动监听容器。
     *
     * @param listener 消费者
     */
    private void register(AbstractAmqpListener<?> listener) {
        boolean isDlq = listener instanceof AbstractDlqListener<?>;
        String exchangeName = listener.resolveExchangeName();
        String mainQueueName = listener.resolveQueueName();

        TopicExchange exchange = ExchangeBuilder.topicExchange(exchangeName).durable(true).build();
        rabbitAdmin.declareExchange(exchange);

        String dlxName = ExchangeNaming.deadLetterExchange(exchangeName);
        rabbitAdmin.declareExchange(ExchangeBuilder.topicExchange(dlxName).durable(true).build());

        String listenQueue;
        if (isDlq) {
            // DLQ listener 只声明并监听死信队列本身，绑定到 DLX
            listenQueue = ((AbstractDlqListener<?>) listener).resolveDlqName();
            Queue dlq = buildDeadLetterQueue(listenQueue);
            rabbitAdmin.declareQueue(dlq);
            rabbitAdmin.declareBinding(
                    BindingBuilder.bind(dlq).to(new TopicExchange(dlxName)).with(mainQueueName));
        } else {
            // 主 listener：queue 带 DLX 参数，重试耗尽的消息由 recoverer 投到 DLX
            listenQueue = mainQueueName;
            Queue queue = QueueBuilder.durable(listenQueue)
                    .deadLetterExchange(dlxName)
                    .deadLetterRoutingKey(listenQueue)
                    .build();
            rabbitAdmin.declareQueue(queue);
            Binding binding = BindingBuilder.bind(queue).to(exchange).with(listener.resolveRoutingKey());
            rabbitAdmin.declareBinding(binding);
        }

        DirectMessageListenerContainer container = new DirectMessageListenerContainer(connectionFactory);
        container.setQueueNames(listenQueue);
        container.setPrefetchCount(properties.getConsumer().getPrefetch());
        // ACK 模式为框架默认的 AUTO：dispatcher 正常返回即 ack，抛异常则 nack。
        // dispatcher 自身已消化业务异常（重试 → DLQ），所以走到下面 nack 的只有
        // dispatcher 框架层面的失败（如投递 DLX 失败）。
        //
        // requeue 必须关：默认值是 true，nack 后消息立刻回队头重新投递，
        // 而失败原因（DLX 不可达等）通常不会自愈 —— 会变成"重投 → 再失败 → 再重投"的死循环，
        // 且每轮都把业务 handle() 重跑 maxAttempts 次，持续压垮下游。
        // 关掉之后 nack 的消息由 broker 按队列上的 x-dead-letter-exchange 自动转投 DLX，
        // 这条兜底路径完全由 broker 承担，不需要我们写任何搬运代码。
        container.setDefaultRequeueRejected(false);
        if (!isDlq) {
            // 重试 + 死信投递交给框架。DLQ listener 不挂：死信重试会在 DLQ 里打转。
            container.setAdviceChain(buildRetryInterceptor(listener));
        }
        container.setMessageListener(message -> dispatcher.dispatch(listener, message));
        container.afterPropertiesSet();
        container.start();
        containers.add(container);

        log.info("[AMQP] listener registered: queue={}, routingKey={}, class={}",
                listenQueue, listener.resolveRoutingKey(), listener.getClass().getSimpleName());
    }

    /**
     * 构建重试拦截器：退避重试 + 耗尽后投 DLX，全部由框架完成。
     *
     * <p>这里用的是 Spring AMQP 的 {@link RetryInterceptorBuilder}，其底层在 spring-rabbit 4.1
     * 已改用 <b>Spring Framework 7 内置</b>的 {@code org.springframework.core.retry}，
     * 因此<b>不需要引入 spring-retry 依赖</b>（spring-core 本来就在 classpath 上）。
     *
     * <p>取代了迁移期手写的 {@code handleWithRetry()} 循环与 {@code sendToDeadLetter()} ——
     * 那两段自己实现了退避计算、中断处理和死信搬运，而框架不仅都有，还多给了
     * jitter、异常分类、以及更完整的死信诊断 header。
     *
     * <p>{@code stateless} 而非 {@code stateful}：重试在消费线程内就地完成，
     * 不把消息 requeue 回 broker，因此不依赖 message id 去重，语义与迁移前一致。
     *
     * <p>{@code excludes(AmqpRejectAndDontRequeueException)}：反序列化失败重试多少次都不会变好，
     * 直接跳过重试进 recoverer（{@code AmqpMessageDispatcher} 靠抛这个异常表达"别重试"）。
     *
     * @param listener 主消费者
     * @return 拦截器
     */
    private MethodInterceptor buildRetryInterceptor(AbstractAmqpListener<?> listener) {
        AmqpProperties.Consumer cfg = properties.getConsumer();
        String exchangeName = listener.resolveExchangeName();
        String mainQueueName = listener.resolveQueueName();
        return RetryInterceptorBuilder.stateless()
                // maxRetries 是"重试次数"，而 max-attempts 是"含首次的总次数"，差 1
                .maxRetries(Math.max(0, cfg.getMaxAttempts() - 1))
                .backOffOptions(cfg.getInitialBackoff().toMillis(), cfg.getMultiplier(),
                        cfg.getMaxBackoff().toMillis())
                .configureRetryPolicy(policy -> policy.excludes(AmqpRejectAndDontRequeueException.class))
                .recoverer(new EagleRepublishRecoverer(rabbitTemplate,
                        ExchangeNaming.deadLetterExchange(exchangeName),
                        mainQueueName,
                        cfg.getMaxAttempts()))
                .build();
    }

    /**
     * 声明死信队列，带上保留策略。
     *
     * <p>DLQ 是只进不出的队列 —— 没有上限的话它只会一直涨，直到吃满 broker 磁盘
     * 把正常业务也拖下水。TTL 与 max-length 都由 broker 自己执行，不需要清理任务。
     *
     * @param dlqName DLQ 队列名
     * @return 队列定义
     */
    private Queue buildDeadLetterQueue(String dlqName) {
        QueueBuilder builder = QueueBuilder.durable(dlqName);
        AmqpProperties.Consumer cfg = properties.getConsumer();
        if (cfg.getDlqTtl() != null) {
            builder.ttl((int) cfg.getDlqTtl().toMillis());
        }
        if (cfg.getDlqMaxLength() != null) {
            builder.maxLength(cfg.getDlqMaxLength());
        }
        return builder.build();
    }

    @Override
    public void destroy() {
        containers.forEach(DirectMessageListenerContainer::stop);
        log.info("[AMQP] {} listener container(s) stopped", containers.size());
    }
}
