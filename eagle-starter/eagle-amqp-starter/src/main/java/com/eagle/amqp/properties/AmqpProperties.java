package com.eagle.amqp.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * AMQP（RabbitMQ）拓扑命名配置。
 *
 * <p><b>本类只管 eagle 侧的拓扑命名</b>。broker 连接、监听容器行为（prefetch、
 * acknowledge-mode、退避重试次数与间隔…）一律走 Spring Boot 原生的
 * {@code spring.rabbitmq.*}，本 starter 不再重复一套自己的键。
 *
 * <p>迁移前这里另有一组 {@code eagle.amqp.consumer.*}（prefetch / max-attempts /
 * initial-backoff / max-backoff / multiplier / retry-alert-threshold），它们与
 * {@code spring.rabbitmq.listener.simple.*} 逐一重复，且因为容器是手工 new 的，
 * Boot 那套反而静默失效。现在容器交由 Boot 的
 * {@code SimpleRabbitListenerContainerFactoryConfigurer} 装配，标准键真正生效，
 * eagle 侧的重复键已删除。starter 级默认值（如默认打开重试）见
 * {@code EagleAmqpDefaultsEnvironmentPostProcessor}。
 *
 * <p>示例（application.yml）：
 * <pre>
 * spring:
 *   rabbitmq:
 *     listener:
 *       simple:
 *         prefetch: 32
 *         retry:
 *           max-retries: 3
 *           initial-interval: 1s
 *           max-interval: 30s
 *           multiplier: 2.0
 * eagle:
 *   amqp:
 *     exchange-prefix: dev_
 *     consumer-group: user_service_default
 * </pre>
 *
 * @author eagle
 */
@Data
@ConfigurationProperties(prefix = "eagle.amqp")
public class AmqpProperties {

    /**
     * 未覆盖 {@code getConsumerGroup()} 时的默认分组。启动期若仍使用此值会直接失败。
     */
    public static final String DEFAULT_CONSUMER_GROUP = "eagle_default";

    /**
     * exchange / queue 名的环境前缀，如 {@code dev_} / {@code test_} / {@code prod_}。
     *
     * <p>留空表示不加前缀 —— 跨环境共享的历史 topic（如 {@code eagle_auth_events}）
     * 由发布方与消费方直接写字面量，不受本项影响。
     */
    private String exchangePrefix = "";

    /**
     * 消费者默认分组名，决定 queue 名后缀。
     *
     * <p><b>注意</b>：同一 exchange 上的不同消费者<b>必须</b>各自覆盖
     * {@code getConsumerGroup()} 给出唯一值 —— 共用默认值会让它们绑到同一个 queue，
     * 退化成竞争消费（这正是迁移前存在的线上缺陷）。
     */
    private String consumerGroup = DEFAULT_CONSUMER_GROUP;

    /**
     * 等待 broker publisher confirm / return 的超时。
     *
     * <p>{@code DomainEventPublisher.publish} 用 Spring AMQP 的
     * {@code CorrelationData.getFuture()} 等结果，nack 或不可路由会抛
     * {@code PUBLISH_FAILED}。超时同样视为发布失败。
     */
    private Duration publisherConfirmTimeout = Duration.ofSeconds(5);
}
