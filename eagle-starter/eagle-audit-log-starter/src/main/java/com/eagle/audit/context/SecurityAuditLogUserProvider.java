package com.eagle.audit.context;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * 从 Spring Security {@link SecurityContextHolder} 抽取当前操作者的 {@link AuditLogUserProvider}。
 *
 * <p>策略:
 * <ul>
 *   <li>operatorId: 优先 JWT claim {@code user_id};否则 JWT subject;否则 {@link Authentication#getName()}</li>
 *   <li>operatorName: {@link Authentication#getName()}</li>
 * </ul>
 *
 * <p>JWT 从 {@link JwtAuthenticationToken#getToken()} 或 {@link Authentication#getCredentials()} 取
 * (前者是 OAuth2 资源服务器标准,后者是 eagle-resource-server-starter 的 EagleAuthentication 习惯)。
 *
 * @author eagle
 */
@Slf4j
public class SecurityAuditLogUserProvider implements AuditLogUserProvider {

    private static final String CLAIM_USER_ID = "user_id";

    @Override
    public String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        Jwt jwt = extractJwt(auth);
        if (jwt != null) {
            Object userId = jwt.getClaim(CLAIM_USER_ID);
            if (userId != null) {
                return userId.toString();
            }
            if (jwt.getSubject() != null) {
                return jwt.getSubject();
            }
        }
        return auth.getName();
    }

    @Override
    public String getCurrentUserName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    private Jwt extractJwt(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken();
        }
        if (auth.getCredentials() instanceof Jwt jwt) {
            return jwt;
        }
        return null;
    }
}
