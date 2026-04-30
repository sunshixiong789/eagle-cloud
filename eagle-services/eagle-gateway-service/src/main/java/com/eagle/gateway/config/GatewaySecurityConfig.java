package com.eagle.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

/**
 * 网关安全配置。
 *
 * <p>配置 {@link ReactiveJwtDecoder} 用于在 Gateway GlobalFilter 中解析 JWT Token。
 * 不启用 Spring Security 的过滤器链（Gateway 使用自己的过滤器链）。
 *
 * @author 孙士雄
 */
@Configuration
@EnableConfigurationProperties(GatewayProperties.class)
public class GatewaySecurityConfig {

    /**
     * 配置 Reactive JWT 解码器。
     *
     * <p>JWK Set URI 由 {@code eagle.gateway.security.auth-server-url} + {@code /oauth2/jwks} 拼接，
     * 避免将授权服务器地址硬编码在配置文件中。
     *
     * @param properties Gateway 配置属性
     * @return ReactiveJwtDecoder
     */
    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder(GatewayProperties properties) {
        String jwkSetUri = properties.getSecurity().getAuthServerUrl() + "/oauth2/jwks";
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
    }
}
