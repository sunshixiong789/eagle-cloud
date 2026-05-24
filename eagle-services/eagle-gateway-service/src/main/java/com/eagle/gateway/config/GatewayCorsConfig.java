package com.eagle.gateway.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

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
 * <p><strong>fail-fast 校验</strong>: 在 {@code prod} / {@code staging} profile 下,
 * {@code allowCredentials=true} 时 {@code allowedOriginPatterns} 不允许包含 {@code "*"} ——
 * 这是 OWASP 与 12-security.md 的红线(允许任意源 + 携带凭证 = CSRF + 数据泄漏)。
 * 违规直接抛 {@link IllegalStateException} 启动失败,而不是上线后悄悄破防。
 *
 * @author 孙士雄
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GatewayCorsProperties.class)
@RequiredArgsConstructor
public class GatewayCorsConfig {

    /**
     * 路径模板：与原 globalcors 的 [/**] 等价
     */
    private static final String PATH_PATTERN = "/**";

    /** 强约束 fail-fast 适用 profile;dev/local/test 仍允许 *(便于本地联调)。 */
    private static final Profiles SECURE_PROFILES = Profiles.of("prod", "staging");

    private final GatewayCorsProperties properties;
    private final Environment environment;

    @Bean
    public CorsWebFilter corsWebFilter() {
        assertSecureCorsInProd();

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

    /**
     * 启动期校验:prod / staging 不允许 {@code allowCredentials=true} 与 {@code allowedOriginPatterns=*} 共存。
     * <p>包级可见以便单元测试直接断言。
     */
    void assertSecureCorsInProd() {
        if (!environment.acceptsProfiles(SECURE_PROFILES)) {
            return;
        }
        if (!properties.isAllowCredentials()) {
            return;
        }
        List<String> patterns = properties.getAllowedOriginPatterns();
        boolean containsWildcard = patterns != null && patterns.stream()
                .anyMatch(p -> p != null && p.trim().equals("*"));
        if (containsWildcard) {
            throw new IllegalStateException(
                    "CORS 配置非法: prod/staging 环境下 allowCredentials=true 时禁止 "
                            + "allowedOriginPatterns 含 '*' (违反 12-security.md). "
                            + "当前 patterns=" + patterns);
        }
    }
}
