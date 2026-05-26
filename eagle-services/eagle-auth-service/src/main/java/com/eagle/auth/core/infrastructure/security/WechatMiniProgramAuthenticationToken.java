package com.eagle.auth.core.infrastructure.security;

import lombok.Getter;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;

import java.util.Map;

/**
 * 微信小程序登录认证 Token
 *
 * @author sunshixiong
 */
@Getter
public class WechatMiniProgramAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {
    public static final AuthorizationGrantType WECHAT_MINI_PROGRAM =
            new AuthorizationGrantType("wechat_mini_program");
    private static final long serialVersionUID = 1L;
    private final String code;

    public WechatMiniProgramAuthenticationToken(String code,
                                                Authentication clientPrincipal,
                                                Map<String, Object> additionalParameters) {
        super(WECHAT_MINI_PROGRAM, clientPrincipal, additionalParameters);
        this.code = code;
    }

}
