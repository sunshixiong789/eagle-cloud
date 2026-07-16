package com.eagle.auth.core.infrastructure.security;

import lombok.Getter;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;

import java.io.Serial;
import java.util.Map;

/** Apple App 登录认证 Token（grant_type = apple_app）。 */
@Getter
public class AppleAppAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {

    public static final AuthorizationGrantType APPLE_APP = new AuthorizationGrantType("apple_app");
    @Serial
    private static final long serialVersionUID = 1L;

    private final String identityToken;
    private final String authorizationCode;
    private final String nonce;
    private final String fullName;

    public AppleAppAuthenticationToken(
                                       String identityToken, String authorizationCode,
                                       String nonce, String fullName,
                                       Authentication clientPrincipal,
                                       Map<String, Object> additionalParameters) {
        super(APPLE_APP, clientPrincipal, additionalParameters);
        this.identityToken = identityToken;
        this.authorizationCode = authorizationCode;
        this.nonce = nonce;
        this.fullName = fullName;
    }
}
