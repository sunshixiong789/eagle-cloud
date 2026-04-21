package com.eagle.system.auth.infrastructure.security;

import lombok.Getter;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;

import java.util.Map;

/**
 * 短信验证码登录认证 Token
 *
 * @author sunshixiong
 */
@Getter
public class SmsCodeAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {
    private static final long serialVersionUID = 1L;

    public static final AuthorizationGrantType SMS_CODE =
            new AuthorizationGrantType("sms_code");

    private final String phone;
    private final String code;

    public SmsCodeAuthenticationToken(String phone, String code,
                                      Authentication clientPrincipal,
                                      Map<String, Object> additionalParameters) {
        super(SMS_CODE, clientPrincipal, additionalParameters);
        this.phone = phone;
        this.code = code;
    }

}
