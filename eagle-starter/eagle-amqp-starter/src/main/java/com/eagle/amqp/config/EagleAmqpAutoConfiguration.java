package com.eagle.amqp.config;

import com.eagle.amqp.listener.AbstractAmqpListener;
import com.eagle.amqp.properties.AmqpProperties;
import com.eagle.amqp.publisher.DomainEventPublisher;
import com.eagle.amqp.publisher.RabbitDomainEventPublisher;
import com.eagle.amqp.support.AmqpMessageDispatcher;
import com.eagle.amqp.support.EagleRepublishRecoverer;
import com.eagle.amqp.support.PublishConfirmLogger;
import com.eagle.amqp.support.UnroutableMessageLogger;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.amqp.autoconfigure.RabbitProperties;
import org.springframework.boot.amqp.autoconfigure.RabbitTemplateCustomizer;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

/**
 * AMQP 消息自动配置。
 *
 * <p><b>装配原则：只加东西，不夺所有权。</b> 连接工厂、{@code RabbitTemplate}、
 * 监听容器的参数全部继续由 Boot 的 {@code spring.rabbitmq.*} 负责，本类通过 Boot 提供的
 * customizer / configurer 扩展点接入。踩过的坑：手工 {@code new} 掉框架的
 * {@code *Factory} 会让对应的整片 {@code spring.*} 配置静默失效。
 *
 * <p>刻意少装配了三块（均为两个仓库零调用）：
 * <ul>
 *   <li>事务消息（半消息 + 回查）—— RabbitMQ 无等价物，且业务从未使用</li>
 *   <li>MQ 分布式锁 —— 靠 RocketMQ 的 receive + invisibleDuration 语义实现，AMQP 无法复刻</li>
 *   <li>topic admin —— AMQP 用 {@link RabbitAdmin} 声明式建拓扑，不需要独立的 admin 客户端</li>
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
        return new RabbitAdmin(connectionFactory);
    }

    /**
     * 让自动配置的 {@link RabbitTemplate} 开启 mandatory 并挂上两个回调。
     *
     * <p>不自己声明 {@code RabbitTemplate} bean —— 那会顶掉 Boot 的自动配置，
     * 使用方在 {@code spring.rabbitmq.template.*} 下的设置会静默失效。
     *
     * <p>没有 mandatory，不可路由的消息会被 broker 静默丢弃，生产方毫无感知
     * （见 {@link UnroutableMessageLogger} 的事故说明）；而
     * {@code publisher-confirm-type=correlated} 不注册 {@code ConfirmCallback}
     * 等于没开（见 {@link PublishConfirmLogger}）。两者是不同的丢失方式，都要有落点。
     *
     * @return template customizer
     */
    @Bean
    @ConditionalOnMissingBean(name = "eaglePublishReliabilityCustomizer")
    public RabbitTemplateCustomizer eaglePublishReliabilityCustomizer() {
        UnroutableMessageLogger returnsCallback = new UnroutableMessageLogger();
        PublishConfirmLogger confirmCallback = new PublishConfirmLogger();
        return template -> {
            template.setMandatory(true);
            template.setReturnsCallback(returnsCallback);
            template.setConfirmCallback(confirmCallback);
        };
    }

    /**
     * 重试耗尽后的落点。
     *
     * <p>声明成 bean 是关键一步：Boot 的
     * {@link SimpleRabbitListenerContainerFactoryConfigurer} 会用
     * {@code ObjectProvider#getIfUnique()} 取走唯一的 {@link MessageRecoverer}，
     * 装进它按 {@code spring.rabbitmq.listener.simple.retry.*} 构建的 retry advice 里。
     * 换言之退避重试与死信投递都由框架完成，本 starter 只提供「投到哪」这一份知识。
     *
     * @param rabbitTemplate   投递用 template
     * @param rabbitProperties Boot 的 rabbit 配置，用于读取重试次数
     * @return recoverer
     */
    @Bean
    @ConditionalOnMissingBean
    public MessageRecoverer eagleMessageRecoverer(RabbitTemplate rabbitTemplate,
                                                  RabbitProperties rabbitProperties) {
        long maxRetries = rabbitProperties.getListener().getSimple().getRetry().getMaxRetries();
        return new EagleRepublishRecoverer(rabbitTemplate, maxRetries);
    }

    /**
     * 主消费者用的容器工厂：Boot 配置 + 一个安全默认。
     *
     * <p>{@code defaultRequeueRejected} 先设再交给 configurer —— Boot 的
     * {@code RabbitProperties} 里该项是 {@code Boolean} 且默认 {@code null}，
     * configurer 只在使用方<b>显式配过</b>时才覆盖。于是这里既给了安全默认，
     * 又保留了 {@code spring.rabbitmq.listener.simple.default-requeue-rejected} 的可配置性。
     *
     * <p>为什么默认必须是 {@code false}：框架默认 {@code true} 会让 nack 的消息立刻回队头重投，
     * 而失败原因（DLX 不可达等）通常不会自愈 —— 变成「重投 → 再失败 → 再重投」的死循环。
     * 关掉之后由 broker 按队列上的 {@code x-dead-letter-exchange} 自动转投，不需要写任何搬运代码。
     *
     * @param configurer        Boot 的工厂配置器（已带上 recoverer 与 retry 设置）
     * @param connectionFactory 连接工厂
     * @return 容器工厂
     */
    @Bean
    @ConditionalOnMissingBean(name = "eagleAmqpListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory eagleAmqpListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setDefaultRequeueRejected(false);
        configurer.configure(factory, connectionFactory);
        return factory;
    }

    /**
     * DLQ 消费者用的容器工厂：与主工厂同源，但**清掉 retry advice**。
     *
     * <p>死信重试会在 DLQ 里打转 —— 处理失败的死信没有下一站可去。
     * {@code AmqpMessageDispatcher} 已经把 DLQ 处理失败消化成日志，这里再叠一层重试毫无意义。
     *
     * @param configurer        Boot 的工厂配置器
     * @param connectionFactory 连接工厂
     * @return 容器工厂
     */
    @Bean
    @ConditionalOnMissingBean(name = "eagleAmqpDlqListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory eagleAmqpDlqListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setDefaultRequeueRejected(false);
        configurer.configure(factory, connectionFactory);
        factory.setAdviceChain();
        return factory;
    }

    @Bean
    @ConditionalOnMissingBean
    public DomainEventPublisher domainEventPublisher(RabbitTemplate rabbitTemplate,
                                                     RabbitAdmin eagleRabbitAdmin,
                                                     ConnectionFactory connectionFactory,
                                                     AmqpProperties properties,
                                                     ObjectMapper objectMapper) {
        return new RabbitDomainEventPublisher(
                rabbitTemplate, eagleRabbitAdmin, connectionFactory, properties, objectMapper);
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
            RabbitAdmin eagleRabbitAdmin,
            AmqpMessageDispatcher dispatcher,
            RabbitListenerContainerFactory<?> eagleAmqpListenerContainerFactory,
            RabbitListenerContainerFactory<?> eagleAmqpDlqListenerContainerFactory) {
        return new AmqpListenerRegistrar(listeners, eagleRabbitAdmin, dispatcher,
                eagleAmqpListenerContainerFactory, eagleAmqpDlqListenerContainerFactory);
    }
}
