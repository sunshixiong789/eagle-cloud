package com.eagle.sentinel.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Sentinel 限流熔断配置属性。
 *
 * <p>示例配置（application.yml）：
 * <pre>
 * eagle:
 *   sentinel:
 *     dashboard: "localhost:8858"
 *     heartbeat-interval-ms: 10000
 *     origin-parser-enabled: true
 *     url-cleaner: true
 * </pre>
 *
 * @author eagle
 */
@Data
@ConfigurationProperties(prefix = "eagle.sentinel")
public class SentinelProperties {

    /**
     * Sentinel Dashboard 控制台地址。
     */
    private String dashboard = "localhost:8858";

    /**
     * 与 Dashboard 心跳间隔（毫秒）。
     */
    private int heartbeatIntervalMs = 10000;

    /**
     * 是否启用请求来源解析，用于授权规则（Authority Rule）。
     * 开启后通过 {@code X-Application-Name} 请求头识别调用方。
     */
    private boolean originParserEnabled = true;

    /**
     * 是否开启 URL 清洗，将含路径变量的相同 URL 合并为同一资源统计。
     * 如 {@code /users/1} 和 {@code /users/2} 合并为 {@code /users/{id}}。
     */
    private boolean urlCleaner = true;
}
