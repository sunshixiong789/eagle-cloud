package com.eagle.rocketmq.idempotency;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 幂等检查器配置。
 *
 * <p>所有字段都有合理默认值,无需消费方显式配置。
 *
 * @author sunshixiong
 */
@Data
@ConfigurationProperties(prefix = "eagle.rocketmq.idempotency")
public class IdempotencyProperties {

    /**
     * Redis Key 前缀。默认 {@code eagle:mq:idempotent:},与 rules/15-messaging.md 范例对齐。
     * 多租户场景可改为 {@code eagle:mq:idempotent:{tenant}:},但需调用方在 eventId 中带上 tenantId。
     */
    private String keyPrefix = "eagle:mq:idempotent:";

    /**
     * 默认 TTL,24 小时。覆盖此值时确保 ≥ MQ 最大重试窗口(默认 16 次 × 退避总和约 数小时)。
     */
    private Duration defaultTtl = Duration.ofDays(1);
}
