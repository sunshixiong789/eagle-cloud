package com.eagle.auth.core.interfaces.dto.response;

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
public record OAuthClientResponse(
        Long id,
        String clientId,
        String clientName,
        Set<String> clientAuthenticationMethods,
        Set<String> authorizationGrantTypes,
        Set<String> redirectUris,
        Set<String> scopes,
        Boolean requireProofKey,
        Boolean requireAuthorizationConsent,
        Long accessTokenTtlSeconds,
        Long refreshTokenTtlSeconds,
        Instant clientIdIssuedAt,
        Boolean enabled,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
