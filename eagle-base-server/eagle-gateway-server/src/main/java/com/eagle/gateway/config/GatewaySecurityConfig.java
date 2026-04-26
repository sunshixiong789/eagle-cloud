package com.eagle.gateway.config;

import org.springframework.beans.factory.annotation.Value;
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
public class GatewaySecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}")
    private String jwkSetUri;

    /**
     * 配置 Reactive JWT 解码器。
     *
     * @return ReactiveJwtDecoder
     */
    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
    }
}
