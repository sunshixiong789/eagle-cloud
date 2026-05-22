package com.eagle.rocketmq.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RocketMQ 配置属性。
 *
 * @author 孙士雄
 */
@Data
@ConfigurationProperties(prefix = "eagle.rocketmq")
public class RocketMqProperties {

    /**
     * 接入点地址，如 {@code localhost:8081}。
     */
    private String endpoints = "localhost:8081";

    /**
     * 默认生产者组。
     */
    private String producerGroup = "eagle-producer-group";

    /**
     * 消费者组（未在监听器中显式覆盖时使用此默认值）。
     */
    private String consumerGroup = "eagle-consumer-group";

    /**
     * 默认 Topic 前缀，自动推导 Topic 时使用。
     */
    private String topicPrefix = "eagle-";

    /**
     * 客户端请求超时时间（毫秒）。
     * 适用于生产者发送和消费者拉取，默认 3000 ms。
     */
    private int requestTimeoutMillis = 3000;

    /**
     * 消息发送失败后的最大重试次数（同步发送）。
     * 默认 2 次，与 RocketMQ 客户端内置重试叠加时请注意幂等性。
     */
    private int maxAttempts = 2;

    /**
     * 消费者配置。
     */
    private Consumer consumer = new Consumer();

    /**
     * 消费者细粒度配置。
     */
    @Data
    public static class Consumer {

        /**
         * 本地缓存消息条数上限。
         * 控制 PushConsumer 的消费速率，防止内存溢出，默认 1024。
         */
        private int maxCachedMessageCount = 1024;

        /**
         * 本地缓存消息总字节数上限（字节），默认 64 MB。
         * 与 {@link #maxCachedMessageCount} 同时生效，任意一个触发则暂停拉取。
         */
        private int maxCachedMessageSizeInBytes = 64 * 1024 * 1024;

        /**
         * 重试次数告警阈值。
         *
         * <p>消息投递次数达到此值时，调用 {@code onRetryAlert()} 触发告警。
         * RocketMQ 默认最大重试 16 次，超过后进入死信队列（DLQ）。
         * 默认告警阈值为 3，提前感知消费异常。
         */
        private int retryAlertThreshold = 3;
    }
}
