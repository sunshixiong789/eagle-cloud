package com.eagle.system.auth.interfaces.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

/**
 * 更新 OAuth2 客户端请求
 *
 * @author sunshixiong
 */
@Data
public class UpdateOAuthClientRequest {

    @Size(max = 200)
    private String clientName;

    @Size(max = 200)
    private String clientSecret;

    private Set<String> clientAuthenticationMethods;

    private Set<String> authorizationGrantTypes;

    private Set<String> redirectUris;

    private Set<String> scopes;

    private Boolean requireProofKey;

    private Boolean requireAuthorizationConsent;

    private Long accessTokenTtlSeconds;

    private Long refreshTokenTtlSeconds;
}
