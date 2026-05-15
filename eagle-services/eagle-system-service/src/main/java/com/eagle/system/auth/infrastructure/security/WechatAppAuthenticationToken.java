package com.eagle.system.auth.infrastructure.security;

import lombok.Getter;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;

import java.util.Map;

/**
 * 微信 App 登录认证 Token。
 *
 * @author sunshixiong
 */
@Getter
public class WechatAppAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {
    public static final AuthorizationGrantType WECHAT_APP =
            new AuthorizationGrantType("wechat_app");
    private static final long serialVersionUID = 1L;
    private final String code;

    public WechatAppAuthenticationToken(String code,
                                        Authentication clientPrincipal,
                                        Map<String, Object> additionalParameters) {
        super(WECHAT_APP, clientPrincipal, additionalParameters);
        this.code = code;
    }
}
