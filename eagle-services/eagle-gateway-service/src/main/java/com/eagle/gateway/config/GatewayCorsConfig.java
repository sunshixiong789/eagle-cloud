package com.eagle.gateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * 网关 CORS 单点装配。
 *
 * <p>用 {@link CorsWebFilter} 唯一负责所有路径（{@code /**}）的 CORS 处理（预检 + 响应头注入），
 * 取代历史上的 {@code spring.cloud.gateway.server.webflux.globalcors}：
 * 在 SCG 5.x 上后者会与 WebFlux 默认 CORS 处理链共存，导致
 * {@code Access-Control-Allow-Origin} 写两份（浏览器按 CORS 规范拒收 "multiple values"）。
 *
 * <p>所有可调字段由 {@link GatewayCorsProperties} 承载（前缀 {@code eagle.gateway.cors}），
 * 与之前 yml 中 {@code globalcors.cors-configurations[/**]} 一一对应。
 *
 * @author 孙士雄
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GatewayCorsProperties.class)
@RequiredArgsConstructor
public class GatewayCorsConfig {

    /**
     * 路径模板：与原 globalcors 的 [/**] 等价
     */
    private static final String PATH_PATTERN = "/**";

    private final GatewayCorsProperties properties;

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(properties.getAllowedOriginPatterns());
        config.setAllowedMethods(properties.getAllowedMethods());
        config.setAllowedHeaders(properties.getAllowedHeaders());
        config.setExposedHeaders(properties.getExposedHeaders());
        config.setAllowCredentials(properties.isAllowCredentials());
        config.setMaxAge(properties.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(PATH_PATTERN, config);
        return new CorsWebFilter(source);
    }
}
