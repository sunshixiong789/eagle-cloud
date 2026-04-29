package com.eagle.resource.server.config;

import com.eagle.common.dto.EagleUser;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;

/**
 * Eagle JWT 认证令牌，以 {@link EagleUser} 作为 Principal。
 *
 * <p>相比标准 {@link org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken}
 * 的 Principal 是 {@link Jwt} 对象，此类直接将 {@link EagleUser} 作为 Principal，
 * 使得 SpEL 权限表达式可直接访问用户字段：
 *
 * <pre>{@code
 * @PreAuthorize("#userId == authentication.principal.id")
 * @PreAuthorize("authentication.principal.deptId == #deptId")
 * }</pre>
 *
 * @author 孙士雄
 * @see EagleJwtAuthenticationConverter
 */
public class EagleAuthentication extends AbstractAuthenticationToken {

    private final EagleUser principal;
    private final Jwt credentials;

    /**
     * @param credentials JWT Token（保留原始 Token，供需要访问原始 Claims 的场景使用）
     * @param principal   从 JWT Claims 解析出的用户信息
     * @param authorities 权限集合
     */
    public EagleAuthentication(Jwt credentials, EagleUser principal,
                                Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.credentials = credentials;
        this.principal = principal;
        setAuthenticated(true);
    }

    /**
     * 返回当前登录用户，类型为 {@link EagleUser}。
     */
    @Override
    public EagleUser getPrincipal() {
        return principal;
    }

    /**
     * 返回原始 JWT Token，供需要访问原始 Claims 的场景使用。
     */
    @Override
    public Jwt getCredentials() {
        return credentials;
    }
}
