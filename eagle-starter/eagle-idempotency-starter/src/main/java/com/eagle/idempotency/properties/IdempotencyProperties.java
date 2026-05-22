package com.eagle.idempotency.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 幂等性组件配置属性。
 *
 * <p>示例配置（application.yml）：
 * <pre>
 * eagle:
 *   idempotency:
 *     enabled: true
 *     token-expire-seconds: 300
 *     result-cache-seconds: 86400
 *     key-prefix: "eagle:idempotency:"
 * </pre>
 *
 * @author sunshixiong
 */
@Data
@ConfigurationProperties(prefix = "eagle.idempotency")
public class IdempotencyProperties {

    /**
     * 幂等 Token 有效期（秒），默认 300 秒（5 分钟）。
     * <p>TOKEN 模式下，客户端预先申请 token 后须在此时间内使用，过期则 token 失效。
     */
    private long tokenExpireSeconds = 300;

    /**
     * 业务键幂等结果缓存时长（秒），默认 86400 秒（24 小时）。
     * <p>BUSINESS_KEY 模式下，相同业务键在此时间内的重复请求将被拦截。
     */
    private long resultCacheSeconds = 86400;

    /**
     * Redis key 前缀，用于区分不同服务的幂等键。
     */
    private String keyPrefix = "eagle:idempotency:";
}
