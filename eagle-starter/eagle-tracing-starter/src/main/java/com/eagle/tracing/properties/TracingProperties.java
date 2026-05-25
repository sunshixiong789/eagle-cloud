package com.eagle.tracing.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * 链路追踪配置属性。
 *
 * @author eagle
 */
@Data
@ConfigurationProperties(prefix = "eagle.tracing")
public class TracingProperties {

    /**
     * Zipkin 上报配置。
     */
    @NestedConfigurationProperty
    private Zipkin zipkin = new Zipkin();

    /**
     * 采样率，范围 0.0 ~ 1.0，默认全采样。
     * 生产环境建议设为 0.1（10% 采样）以降低开销。
     */
    private float samplingProbability = 1.0f;

    @Data
    public static class Zipkin {
        /**
         * Zipkin 服务端点，如 {@code http://localhost:9411/api/v2/spans}。
         * 不配置时不启用 Zipkin 上报。
         */
        private String endpoint;
    }
}
