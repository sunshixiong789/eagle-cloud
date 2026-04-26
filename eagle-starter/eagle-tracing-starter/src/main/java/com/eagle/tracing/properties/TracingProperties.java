package com.eagle.tracing.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 链路追踪配置属性。
 *
 * @author 孙士雄
 */
@Data
@ConfigurationProperties(prefix = "eagle.tracing")
public class TracingProperties {

    /**
     * 是否启用链路追踪。
     */
    private boolean enabled = true;

    /**
     * Zipkin 上报配置。
     */
    private Zipkin zipkin = new Zipkin();

    @Data
    public static class Zipkin {
        /**
         * Zipkin 服务端点地址，如 {@code http://localhost:9411/api/v2/spans}。
         * 为空时不启用 Zipkin 上报。
         */
        private String endpoint;
    }
}
