package com.eagle.amqp.config;

import com.eagle.amqp.listener.AbstractAmqpListener;
import com.eagle.amqp.properties.AmqpProperties;
import com.eagle.amqp.publisher.DomainEventPublisher;
import com.eagle.amqp.publisher.RabbitDomainEventPublisher;
import com.eagle.amqp.support.AmqpMessageDispatcher;
import com.eagle.amqp.support.PublishConfirmLogger;
import com.eagle.amqp.support.UnroutableMessageLogger;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.amqp.autoconfigure.ConnectionFactoryCustomizer;
import org.springframework.boot.amqp.autoconfigure.RabbitTemplateCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    /**
     * 承载消息投递的执行器，交给 amqp-client 作为 shared executor。
     *
     * <p>{@code DirectMessageListenerContainer} 在 <b>amqp-client 自己的 consumer 线程</b>上
     * 直接调用 listener（这是它与 {@code SimpleMessageListenerContainer} 的根本区别，
     * 给容器设 {@code taskExecutor} 改不了这一点）。该线程池按 connection 共享，
     * 默认大小由 CPU 核数推导 —— 而 {@link AmqpMessageDispatcher} 的退避重试是
     * <b>阻塞式</b>的，几条坏消息同时退避就能占满它，把本服务<b>所有</b>队列的消费一起拖住。
     *
     * <p>换成虚拟线程后阻塞几乎不占成本，退避期间不再挤占其它队列。
     * {@code ExecutorService} 自 Java 19 起实现 {@code AutoCloseable}，
     * 由容器在关闭时调用 {@code close()} 等待在途消息处理完。
     *
     * @return 虚拟线程执行器
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(name = "eagleAmqpConsumerExecutor")
    public ExecutorService eagleAmqpConsumerExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * 把上面的执行器装到 amqp-client 的 {@code ConnectionFactory} 上。
     *
     * <p>用 Boot 的 {@link ConnectionFactoryCustomizer} 扩展点，不接管连接工厂本身 ——
     * {@code spring.rabbitmq.*} 下的连接配置继续由 Boot 自动配置负责。
     *
     * @param eagleAmqpConsumerExecutor 消费执行器
     * @return customizer
     */
    @Bean
    @ConditionalOnMissingBean(name = "eagleAmqpConsumerExecutorCustomizer")
    public ConnectionFactoryCustomizer eagleAmqpConsumerExecutorCustomizer(
            ExecutorService eagleAmqpConsumerExecutor) {
        return factory -> factory.setSharedExecutor(eagleAmqpConsumerExecutor);
    }

    @Bean
    @ConditionalOnMissingBean
    public RabbitAdmin eagleRabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        // 拓扑由 AmqpListenerRegistrar 在启动期显式声明，这里不做全量自动声明
        admin.setAutoStartup(true);
        return admin;
    }

    /**
     * 让自动配置的 {@link RabbitTemplate} 开启 mandatory 并挂上退回回调。
     *
     * <p>不自己声明 {@code RabbitTemplate} bean —— 那会顶掉 Boot 的自动配置，
     * 使用方在 {@code spring.rabbitmq.template.*} 下的设置会静默失效。
     * customizer 是 Boot 提供的定制点，只加东西不夺所有权。
     *
     * <p>没有 mandatory，不可路由的消息会被 broker 静默丢弃，
     * 生产方毫无感知（见 {@link UnroutableMessageLogger} 的事故说明）。
     */
    @Bean
    @ConditionalOnMissingBean(name = "eagleUnroutableMessageCustomizer")
    public RabbitTemplateCustomizer eagleUnroutableMessageCustomizer() {
        UnroutableMessageLogger returnsCallback = new UnroutableMessageLogger();
        PublishConfirmLogger confirmCallback = new PublishConfirmLogger();
        return template -> {
            template.setMandatory(true);
            template.setReturnsCallback(returnsCallback);
            // nack 与 return 是两种不同的丢失方式，都要有落点，否则都是静默丢弃
            template.setConfirmCallback(confirmCallback);
        };
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
    public AmqpMessageDispatcher amqpMessageDispatcher(ObjectMapper objectMapper) {
        return new AmqpMessageDispatcher(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public AmqpListenerRegistrar amqpListenerRegistrar(
            ObjectProvider<AbstractAmqpListener<?>> listeners,
            ConnectionFactory connectionFactory,
            RabbitAdmin eagleRabbitAdmin,
            AmqpProperties properties,
            AmqpMessageDispatcher dispatcher,
            RabbitTemplate rabbitTemplate) {
        return new AmqpListenerRegistrar(
                listeners, connectionFactory, eagleRabbitAdmin, properties, dispatcher, rabbitTemplate);
    }
}
