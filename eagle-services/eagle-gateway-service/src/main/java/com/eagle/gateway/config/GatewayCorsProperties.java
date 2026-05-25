package com.eagle.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 网关 CORS 配置项（{@code eagle.gateway.cors.*}）。
 *
 * <p>历史上同等语义放在 {@code spring.cloud.gateway.server.webflux.globalcors} 下，
 * 但 SCG 5.x 在 {@code globalcors} 与 WebFlux 默认 CORS 处理链共存时会双写
 * {@code Access-Control-Allow-Origin}，浏览器按 CORS 规范拒收（"multiple values"）。
 * 现改由 {@link GatewayCorsConfig} 注册的 {@code CorsWebFilter} 单点处理，
 * 本类承载等价配置项；外部环境变量 {@code CORS_ALLOWED_ORIGINS} 名称不变。
 *
 * <p>列表字段支持逗号分隔字符串（Spring Boot Relaxed Binder 自动转换）：
 * {@code CORS_ALLOWED_ORIGINS=http://a.com,http://b.com}。
 *
 * @author eagle
 */
@Data
@ConfigurationProperties(prefix = "eagle.gateway.cors")
public class GatewayCorsProperties {

    /**
     * 允许的源（支持通配），缺省 *（基线放开；prod profile 强制注入并覆盖）
     */
    private List<String> allowedOriginPatterns = List.of("*");

    /**
     * 允许的 HTTP 方法
     */
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");

    /**
     * 允许的请求头，* 表示放开所有
     */
    private List<String> allowedHeaders = List.of("*");

    /**
     * 暴露给前端 JS 的响应头（前端需要读取的自定义头必须列在这里）
     */
    private List<String> exposedHeaders = List.of();

    /**
     * 是否允许携带 Cookie / Authorization 凭据
     */
    private boolean allowCredentials = true;

    /**
     * 预检请求缓存秒数
     */
    private long maxAge = 3600L;
}
