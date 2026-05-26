package com.eagle.auth.core.interfaces.dto.response;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * OAuth2 客户端响应
 * <p>
 * 注意：不包含 clientSecret，避免敏感信息泄露
 *
 * @author sunshixiong
 */
@Data
public class OAuthClientResponse {

    private Long id;
    private String clientId;
    private String clientName;
    private Set<String> clientAuthenticationMethods;
    private Set<String> authorizationGrantTypes;
    private Set<String> redirectUris;
    private Set<String> scopes;
    private Boolean requireProofKey;
    private Boolean requireAuthorizationConsent;
    private Long accessTokenTtlSeconds;
    private Long refreshTokenTtlSeconds;
    private Instant clientIdIssuedAt;
    private Boolean enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
