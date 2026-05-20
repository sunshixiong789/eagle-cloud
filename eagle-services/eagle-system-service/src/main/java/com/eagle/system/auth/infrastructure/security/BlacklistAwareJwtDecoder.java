package com.eagle.system.auth.infrastructure.security;

import com.eagle.system.auth.domain.port.OnlineUserPort;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.annotation.PostConstruct;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

/**
 * 带黑名单校验的 {@link JwtDecoder}。
 *
 * <p>用 {@code @PostConstruct} 一次性构建 delegate；避免与 {@link JWKSource} Bean 产生循环依赖，
 * 同时省去原 double-checked locking 写法。
 *
 * @author sunshixiong
 */
@Component
public class BlacklistAwareJwtDecoder implements JwtDecoder {

    private final JWKSource<SecurityContext> jwkSource;
    private final OnlineUserPort onlineUserPort;

    private JwtDecoder delegate;

    public BlacklistAwareJwtDecoder(JWKSource<SecurityContext> jwkSource,
                                    OnlineUserPort onlineUserPort) {
        this.jwkSource = jwkSource;
        this.onlineUserPort = onlineUserPort;
    }

    /**
     * 测试用构造器：允许直接注入 delegate，避免依赖 OAuth2AuthorizationServerConfiguration
     * 与真实 JWKSource。
     */
    BlacklistAwareJwtDecoder(OnlineUserPort onlineUserPort, JwtDecoder delegate) {
        this.jwkSource = null;
        this.onlineUserPort = onlineUserPort;
        this.delegate = delegate;
    }

    @PostConstruct
    void init() {
        if (this.delegate == null) {
            this.delegate = OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
        }
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        Jwt jwt = delegate.decode(token);
        String jti = jwt.getId();
        if (jti != null && onlineUserPort.isBlacklisted(jti)) {
            throw new BadJwtException("Token has been revoked");
        }
        return jwt;
    }
}
