package com.eagle.amqp.config;

import com.eagle.amqp.exception.AmqpErrorCode;
import com.eagle.amqp.listener.AbstractAmqpListener;
import com.eagle.amqp.listener.AbstractDlqListener;
import com.eagle.amqp.properties.AmqpProperties;
import com.eagle.amqp.support.AmqpMessageDispatcher;
import com.eagle.amqp.support.ExchangeNaming;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.beans.factory.ObjectProvider;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 启动期遍历容器内所有 {@link AbstractAmqpListener} bean，声明 AMQP 拓扑并把每个 listener
 * 注册成一个 {@link SimpleRabbitListenerEndpoint}。
 *
 * <p><b>为什么不用 {@code @RabbitListener} 注解</b>：exchange / queue 名是运行时由
 * {@code getTopic()} + {@code getConsumerGroup()} 决定的，不是注解能接受的编译期常量。
 * 这个设计同时保留了原来「listener 是普通 {@code @Component}、自注册」的使用方式。
 *
 * <p><b>为什么实现 {@link RabbitListenerConfigurer} 而不是自己 new 容器</b>：
 * 手动 {@code new DirectMessageListenerContainer(...)} 会绕开 Boot 的装配路径，
 * 让 {@code spring.rabbitmq.listener.*} 整片配置<b>静默失效</b> —— 使用方照官方文档配
 * prefetch / acknowledge-mode / retry，行为纹丝不动且没有任何报错。
 * {@code RabbitListenerConfigurer} 正是框架为「队列名运行时才确定」准备的扩展点：
 * 容器由 Boot 配置好的 {@code SimpleRabbitListenerContainerFactory} 创建，
 * 生命周期交给 {@code RabbitListenerEndpointRegistry} 托管
 * （因此本类不再需要自己持有容器列表并实现 {@code DisposableBean}），
 * 还额外获得按 endpoint id 单独启停某个消费者的能力。
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
public class AmqpListenerRegistrar implements RabbitListenerConfigurer {

    private final ObjectProvider<AbstractAmqpListener<?>> listeners;
    private final RabbitAdmin rabbitAdmin;
    private final AmqpMessageDispatcher dispatcher;
    private final RabbitListenerContainerFactory<?> eagleAmqpListenerContainerFactory;
    private final RabbitListenerContainerFactory<?> eagleAmqpDlqListenerContainerFactory;

    @Override
    public void configureRabbitListeners(RabbitListenerEndpointRegistrar registrar) {
        List<AbstractAmqpListener<?>> all = listeners.stream().toList();
        validateMainConsumers(all);
        all.forEach(listener -> register(registrar, listener));
        log.info("[AMQP] {} listener endpoint(s) registered", all.size());
    }

    /**
     * 启动期拦住「忘了覆盖 consumerGroup」和「两个主消费者绑同一 queue」。
     */
    public static void validateMainConsumers(List<AbstractAmqpListener<?>> listeners) {
        Map<String, String> queueOwners = new LinkedHashMap<>();
        for (AbstractAmqpListener<?> listener : listeners) {
            if (listener instanceof AbstractDlqListener<?>) {
                continue;
            }
            String queue = listener.resolveQueueName();
            if (queue.endsWith("." + AmqpProperties.DEFAULT_CONSUMER_GROUP)) {
                throw AmqpErrorCode.CONSUMER_INIT_FAILED.toServiceException(
                        new IllegalStateException(
                                listener.getClass().getName()
                                        + " 仍使用默认 consumerGroup=eagle_default，必须覆盖 getConsumerGroup()"));
            }
            String previous = queueOwners.put(queue, listener.getClass().getName());
            if (previous != null) {
                throw AmqpErrorCode.CONSUMER_INIT_FAILED.toServiceException(
                        new IllegalStateException(
                                "多个主消费者绑到同一 queue=" + queue + ": " + previous
                                        + " 与 " + listener.getClass().getName()));
            }
        }
    }

    /**
     * 为单个 listener 声明拓扑并注册 endpoint。
     *
     * @param registrar 框架的 endpoint 注册器
     * @param listener  消费者
     */
    private void register(RabbitListenerEndpointRegistrar registrar, AbstractAmqpListener<?> listener) {
        boolean isDlq = listener instanceof AbstractDlqListener<?>;
        String exchangeName = listener.resolveExchangeName();
        String mainQueueName = listener.resolveQueueName();

        TopicExchange exchange = ExchangeBuilder.topicExchange(exchangeName).durable(true).build();
        rabbitAdmin.declareExchange(exchange);

        String dlxName = ExchangeNaming.deadLetterExchange(exchangeName);
        rabbitAdmin.declareExchange(ExchangeBuilder.topicExchange(dlxName).durable(true).build());

        String listenQueue;
        if (isDlq) {
            // DLQ listener 只声明并监听死信队列本身，绑定到 DLX。
            //
            // 这里**不带任何 arguments** —— DLQ 的保留策略（x-message-ttl / x-max-length）
            // 由 broker 侧的 policy 承担，见 docs/rabbitmq-dlq-policy.md。
            // 原因：队列 arguments 创建后不可变，broker 在重声明时做全等比较，不一致就回
            // 406 PRECONDITION_FAILED 并关掉 channel —— 落到这里就是**整个服务起不来**。
            // 把保留策略写进声明，等于让「调一次 TTL」变成「先停服务、删光所有 DLQ、再启动」，
            // 而 policy 可以随时改、立即对存量队列生效，正是 RabbitMQ 为此提供的机制。
            listenQueue = ((AbstractDlqListener<?>) listener).resolveDlqName();
            Queue dlq = QueueBuilder.durable(listenQueue).build();
            // **只对 DLQ** 放开声明异常：存量 DLQ 可能带着历史 arguments（如某个版本写进去的
            // x-message-ttl），与这里的无参数声明不等价，broker 会回 406。
            // 但对 DLQ 而言这不是错误 —— 队列就在那儿、能收能消费，参数差异由 policy 那条路
            // 去收敛，没有任何理由为此让整个服务起不来（已经发生过两次）。
            // RabbitAdmin 会打一条 WARN，拓扑漂移仍然可见。
            //
            // 注意范围：主 queue、exchange、binding 的声明**照旧 fail fast** ——
            // 那些位置的不等价意味着真的拓扑写错了（前缀拼错、routing key 不匹配），
            // 必须当场炸出来，静默跳过会变成消息静默丢失。
            dlq.setIgnoreDeclarationExceptions(true);
            rabbitAdmin.declareQueue(dlq);
            rabbitAdmin.declareBinding(
                    BindingBuilder.bind(dlq).to(new TopicExchange(dlxName)).with(mainQueueName));
        } else {
            // 主 listener：queue 带 DLX 参数。重试耗尽由 EagleRepublishRecoverer 投到 DLX；
            // 容器层面 nack 的消息（acknowledge-mode 之外的失败）由 broker 按这两个参数兜底转投。
            listenQueue = mainQueueName;
            Queue queue = QueueBuilder.durable(listenQueue)
                    .deadLetterExchange(dlxName)
                    .deadLetterRoutingKey(listenQueue)
                    .build();
            rabbitAdmin.declareQueue(queue);
            Binding binding = BindingBuilder.bind(queue).to(exchange).with(listener.resolveRoutingKey());
            rabbitAdmin.declareBinding(binding);

            // 主消费者一并声明 DLQ：没有 AbstractDlqListener 时 recoverer / broker DLX 也有落点
            Queue dlq = QueueBuilder.durable(ExchangeNaming.deadLetterQueue(listenQueue)).build();
            dlq.setIgnoreDeclarationExceptions(true);
            rabbitAdmin.declareQueue(dlq);
            rabbitAdmin.declareBinding(
                    BindingBuilder.bind(dlq).to(new TopicExchange(dlxName)).with(listenQueue));
        }

        SimpleRabbitListenerEndpoint endpoint = new SimpleRabbitListenerEndpoint();
        // id 用队列名：RabbitListenerEndpointRegistry 按它索引容器，运维可据此单独启停
        endpoint.setId(listenQueue);
        endpoint.setQueueNames(listenQueue);
        endpoint.setAdmin(rabbitAdmin);
        endpoint.setMessageListener(message -> dispatcher.dispatch(listener, message));

        // DLQ 走不带 retry advice 的 factory —— 死信重试会在 DLQ 里打转
        registrar.registerEndpoint(endpoint,
                isDlq ? eagleAmqpDlqListenerContainerFactory : eagleAmqpListenerContainerFactory);

        log.info("[AMQP] listener registered: queue={}, routingKey={}, class={}",
                listenQueue, listener.resolveRoutingKey(), listener.getClass().getSimpleName());
    }
}
