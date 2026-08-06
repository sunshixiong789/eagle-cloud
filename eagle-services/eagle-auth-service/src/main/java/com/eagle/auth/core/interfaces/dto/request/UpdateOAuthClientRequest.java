package com.eagle.auth.core.interfaces.dto.request;

import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * 更新 OAuth2 客户端请求
 * <p>
 * 字段为 null 表示「不修改」，由应用服务按 null 判断是否覆盖。
 *
 * @author sunshixiong
 */
public record UpdateOAuthClientRequest(

        @Size(max = 200)
        String clientName,

        @Size(max = 200)
        String clientSecret,

        Set<String> clientAuthenticationMethods,

        Set<String> authorizationGrantTypes,

        Set<String> redirectUris,

        Set<String> scopes,

        Boolean requireProofKey,

        Boolean requireAuthorizationConsent,

        Long accessTokenTtlSeconds,

        Long refreshTokenTtlSeconds
) {
}
