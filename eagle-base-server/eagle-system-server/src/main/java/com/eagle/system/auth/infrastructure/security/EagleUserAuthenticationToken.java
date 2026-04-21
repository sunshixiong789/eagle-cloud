package com.eagle.system.auth.infrastructure.security;

import com.eagle.common.dto.EagleUser;
import org.springframework.security.authentication.AbstractAuthenticationToken;

/**
 * 自定义用户认证 Token
 * <p>
 * 用于在自定义 grant type 中包装 EagleUser 作为 principal，
 * 以便 jwtTokenCustomizer 能正确提取用户信息写入 JWT
 *
 * @author sunshixiong
 */
public class EagleUserAuthenticationToken extends AbstractAuthenticationToken {

    private final EagleUser eagleUser;

    public EagleUserAuthenticationToken(EagleUser eagleUser) {
        super(eagleUser.getAuthorities());
        this.eagleUser = eagleUser;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return eagleUser;
    }

    @Override
    public String getName() {
        return eagleUser.getUsername();
    }
}
