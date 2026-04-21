package com.eagle.system.auth.infrastructure.security;

import com.eagle.system.auth.domain.port.OnlineUserPort;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

/**
 * 带黑名单校验的 JwtDecoder。
 * <p>
 * 替换 SecurityConfig 中的默认 {@code JwtDecoder} @Bean。
 * 使用延迟初始化（double-checked locking）构建 delegate，
 * 避免与 {@link JWKSource} Bean 产生循环依赖。
 *
 * @author sunshixiong
 */
@Component
@RequiredArgsConstructor
public class BlacklistAwareJwtDecoder implements JwtDecoder {

    private final JWKSource<SecurityContext> jwkSource;
    private final OnlineUserPort onlineUserPort;

    /** delegate 延迟初始化，避免循环依赖。 */
    private volatile JwtDecoder delegate;

    @Override
    public Jwt decode(String token) throws JwtException {
        Jwt jwt = getDelegate().decode(token);
        String jti = jwt.getId();
        if (jti != null && onlineUserPort.isBlacklisted(jti)) {
            throw new BadJwtException("Token has been revoked");
        }
        return jwt;
    }

    private JwtDecoder getDelegate() {
        if (delegate == null) {
            synchronized (this) {
                if (delegate == null) {
                    delegate = OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
                }
            }
        }
        return delegate;
    }
}
