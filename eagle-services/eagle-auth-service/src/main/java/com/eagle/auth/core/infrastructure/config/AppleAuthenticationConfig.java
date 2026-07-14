package com.eagle.auth.core.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Apple 公钥 JWT 验证器配置。
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AppleAuthenticationProperties.class)
public class AppleAuthenticationConfig {

    @Bean("appleJwtDecoder")
    JwtDecoder appleJwtDecoder(AppleAuthenticationProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.getJwkSetUri()).build();
        decoder.setJwtValidator(createValidator(properties));
        return decoder;
    }

    static OAuth2TokenValidator<Jwt> createValidator(AppleAuthenticationProperties properties) {
        OAuth2TokenValidator<Jwt> issuerAndTimestamp =
                JwtValidators.createDefaultWithIssuer(properties.getIssuer());
        OAuth2TokenValidator<Jwt> audience = jwt -> {
            if (jwt.getAudience().contains(properties.getClientId())) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "invalid_token", "Apple identity token audience mismatch", null));
        };
        return new DelegatingOAuth2TokenValidator<>(issuerAndTimestamp, audience);
    }
}
