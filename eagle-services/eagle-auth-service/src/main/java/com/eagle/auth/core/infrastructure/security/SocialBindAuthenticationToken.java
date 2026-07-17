package com.eagle.auth.core.infrastructure.security;

import lombok.Getter;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;

import java.io.Serial;
import java.util.Map;

/**
 * 第三方身份挂靠手机号认证 Token（grant_type = social_bind）。
 *
 * <p>参数：{@code bind_ticket}（binding_required 响应下发的一次性凭证）、
 * {@code phone}、{@code code}（短信验证码）。
 *
 * @author sunshixiong
 */
@Getter
public class SocialBindAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {

    public static final AuthorizationGrantType SOCIAL_BIND =
            new AuthorizationGrantType("social_bind");

    @Serial
    private static final long serialVersionUID = 1L;

    private final String bindTicket;
    private final String phone;
    private final String code;

    public SocialBindAuthenticationToken(String bindTicket, String phone, String code,
                                         Authentication clientPrincipal,
                                         Map<String, Object> additionalParameters) {
        super(SOCIAL_BIND, clientPrincipal, additionalParameters);
        this.bindTicket = bindTicket;
        this.phone = phone;
        this.code = code;
    }
}
