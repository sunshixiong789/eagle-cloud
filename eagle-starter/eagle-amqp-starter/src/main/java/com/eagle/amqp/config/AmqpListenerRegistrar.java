package com.eagle.amqp.config;

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
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
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
            Queue dlq = QueueBuilder.durable(listenQueue).build();
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
        // 手动 ACK：由 dispatcher 在重试与 DLQ 投递之后决定 ack/nack
        container.setMessageListener(message -> dispatcher.dispatch(listener, message));
        container.afterPropertiesSet();
        container.start();
        containers.add(container);

        log.info("[AMQP] listener registered: queue={}, routingKey={}, class={}",
                listenQueue, listener.resolveRoutingKey(), listener.getClass().getSimpleName());
    }

    @Override
    public void destroy() {
        containers.forEach(DirectMessageListenerContainer::stop);
        log.info("[AMQP] {} listener container(s) stopped", containers.size());
    }
}
