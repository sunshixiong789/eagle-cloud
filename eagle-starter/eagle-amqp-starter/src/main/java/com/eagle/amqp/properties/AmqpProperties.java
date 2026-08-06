package com.eagle.amqp.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * AMQP（RabbitMQ）消息配置属性。
 *
 * <p>取代原 {@code eagle.rocketmq.*}。broker 连接本身走 Spring Boot 原生的
 * {@code spring.rabbitmq.*}，本类只管 eagle 侧的拓扑命名与消费行为。
 *
 * <p><b>前缀键收敛</b>：原先并存两套互不兼容的键 ——
 * ease-mind 侧的 {@code eagle.rocketmq.topic-prefix}（绑定到 Properties，业务手工拼接）
 * 与 eagle-cloud 侧的 {@code eagle.rocketmq.topic-env-prefix}（Properties 里根本没这个字段，
 * 仅一处 {@code Environment.getProperty} 直读）。两者在容器化 dev 环境下会让生产方与消费方
 * 的 topic 对不上。此处统一为唯一的 {@link #exchangePrefix}。
 *
 * <p>示例（application.yml）：
 * <pre>
 * eagle:
 *   amqp:
 *     exchange-prefix: dev_
 *     consumer-group: user_service_default
 *     consumer:
 *       prefetch: 32
 *       retry-alert-threshold: 3
 *       max-attempts: 4
 *       initial-backoff: 1s
 *       max-backoff: 30s
 *       multiplier: 2.0
 * </pre>
 *
 * @author eagle
 */
@Data
@ConfigurationProperties(prefix = "eagle.amqp")
public class AmqpProperties {

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
    private String consumerGroup = "eagle_default";

    private final Consumer consumer = new Consumer();

    /**
     * 消费侧行为配置。
     */
    @Data
    public static class Consumer {

        /**
         * 每个消费者的预取数量（basic.qos），对应原 RocketMQ 的 maxCachedMessageCount。
         */
        private int prefetch = 32;

        /**
         * 投递次数达到该阈值时触发 {@code onRetryAlert} 告警回调。
         */
        private int retryAlertThreshold = 3;

        /**
         * 单条消息的最大尝试次数（含首次），耗尽后投递到 DLQ。
         *
         * <p>RocketMQ 由 Broker 侧重试最多 16 次；RabbitMQ 无 Broker 侧重试，
         * 这里改由客户端退避重试实现，次数刻意调小以免长时间占用消费者。
         */
        private int maxAttempts = 4;

        /**
         * 首次重试前的退避时长。
         */
        private Duration initialBackoff = Duration.ofSeconds(1);

        /**
         * 退避时长上限。
         */
        private Duration maxBackoff = Duration.ofSeconds(30);

        /**
         * 退避倍率。
         */
        private double multiplier = 2.0;
    }
}
