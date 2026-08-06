package com.eagle.amqp.config;

import com.eagle.amqp.listener.AbstractAmqpListener;
import com.eagle.amqp.properties.AmqpProperties;
import com.eagle.amqp.publisher.DomainEventPublisher;
import com.eagle.amqp.publisher.RabbitDomainEventPublisher;
import com.eagle.amqp.support.AmqpMessageDispatcher;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

/**
 * AMQP 消息自动配置，取代原 {@code RocketMqAutoConfiguration}。
 *
 * <p>相比原配置刻意少装配了三块（均为两个仓库零调用）：
 * <ul>
 *   <li>事务消息（半消息 + 回查）—— RabbitMQ 无等价物，且业务从未使用</li>
 *   <li>MQ 分布式锁 —— 靠 RocketMQ 的 receive + invisibleDuration 语义实现，AMQP 无法复刻；
 *       且 {@code eagle.lock.type} 从未配成 {@code mq}，bean 从未装配过</li>
 *   <li>topic admin —— AMQP 用 {@link RabbitAdmin} 声明式建拓扑，不需要独立的 admin 客户端
 *       （原实现为此额外引入了 4.x remoting 的 rocketmq-tools，正是 grpc-netty-shaded 的来源）</li>
 * </ul>
 *
 * @author eagle
 */
@AutoConfiguration
@ConditionalOnClass(RabbitTemplate.class)
@EnableConfigurationProperties(AmqpProperties.class)
public class EagleAmqpAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RabbitAdmin eagleRabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        // 拓扑由 AmqpListenerRegistrar 在启动期显式声明，这里不做全量自动声明
        admin.setAutoStartup(true);
        return admin;
    }

    @Bean
    @ConditionalOnMissingBean
    public DomainEventPublisher domainEventPublisher(RabbitTemplate rabbitTemplate,
                                                     RabbitAdmin eagleRabbitAdmin,
                                                     AmqpProperties properties,
                                                     ObjectMapper objectMapper) {
        return new RabbitDomainEventPublisher(rabbitTemplate, eagleRabbitAdmin, properties, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public AmqpMessageDispatcher amqpMessageDispatcher(ObjectMapper objectMapper,
                                                       RabbitTemplate rabbitTemplate,
                                                       AmqpProperties properties) {
        return new AmqpMessageDispatcher(objectMapper, rabbitTemplate, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public AmqpListenerRegistrar amqpListenerRegistrar(
            ObjectProvider<AbstractAmqpListener<?>> listeners,
            ConnectionFactory connectionFactory,
            RabbitAdmin eagleRabbitAdmin,
            AmqpProperties properties,
            AmqpMessageDispatcher dispatcher) {
        return new AmqpListenerRegistrar(
                listeners, connectionFactory, eagleRabbitAdmin, properties, dispatcher);
    }
}
