package com.eagle.auth.infrastructure.security;

import lombok.Getter;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;

import java.io.Serial;
import java.util.Map;

/**
 * 手机号一键登录认证 Token
 * <p>
 * 自定义 OAuth2 grant_type: {@code phone_one_click}。
 * 客户端从运营商或聚合 SDK 获取 access_token 后提交至授权端点，
 * 由 {@link PhoneOneClickAuthenticationProvider} 完成校验并签发 OAuth2 Token。
 *
 * @author sunshixiong
 */
@Getter
public class PhoneOneClickAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {

    public static final AuthorizationGrantType PHONE_ONE_CLICK =
            new AuthorizationGrantType("phone_one_click");
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 运营商 / SDK 颁发的一键登录 access_token
     */
    private final String accessToken;

    public PhoneOneClickAuthenticationToken(String accessToken,
                                            Authentication clientPrincipal,
                                            Map<String, Object> additionalParameters) {
        super(PHONE_ONE_CLICK, clientPrincipal, additionalParameters);
        this.accessToken = accessToken;
    }
}
