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
     * 是否启用 RocketMQ。
     */
    private boolean enabled = true;

    /**
     * 接入点地址，如 {@code localhost:8081}。
     */
    private String endpoints = "localhost:8081";

    /**
     * 默认生产者组。
     */
    private String producerGroup = "eagle-producer-group";

    /**
     * 消费者组。
     */
    private String consumerGroup = "eagle-consumer-group";

    /**
     * 默认 Topic 前缀。
     */
    private String topicPrefix = "eagle-";
}
