package com.eagle.auth.core.infrastructure.security;

import lombok.Getter;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;

import java.util.Map;

/**
 * 淘宝 App 登录认证 Token（grant_type = taobao_app）。
 *
 * @author sunshixiong
 */
@Getter
public class TaobaoAppAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {

    public static final AuthorizationGrantType TAOBAO_APP =
            new AuthorizationGrantType("taobao_app");
    private static final long serialVersionUID = 1L;

    private final String tbAccessToken;
    private final String tbAuthCode;
    private final String phone;
    private final String smsCode;

    public TaobaoAppAuthenticationToken(String tbAccessToken, String tbAuthCode,
                                        String phone, String smsCode,
                                        Authentication clientPrincipal,
                                        Map<String, Object> additionalParameters) {
        super(TAOBAO_APP, clientPrincipal, additionalParameters);
        this.tbAccessToken = tbAccessToken;
        this.tbAuthCode = tbAuthCode;
        this.phone = phone;
        this.smsCode = smsCode;
    }
}
