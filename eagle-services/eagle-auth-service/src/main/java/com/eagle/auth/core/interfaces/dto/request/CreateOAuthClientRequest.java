package com.eagle.auth.core.interfaces.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * 创建 OAuth2 客户端请求
 *
 * @param clientId                    客户端 ID
 * @param clientSecret                客户端密钥（明文，由应用服务加密后落库）
 * @param clientName                  客户端名称
 * @param clientAuthenticationMethods 认证方式集合，如 ["none"], ["client_secret_basic"]
 * @param authorizationGrantTypes     授权类型集合，如 ["authorization_code", "refresh_token"]
 * @param redirectUris                重定向 URI 集合
 * @param scopes                      授权范围集合，如 ["openid", "profile"]
 * @param requireProofKey             是否要求 PKCE，缺省 false
 * @param requireAuthorizationConsent 是否要求授权同意，缺省 false
 * @param accessTokenTtlSeconds       Access Token 有效期（秒），缺省 3600
 * @param refreshTokenTtlSeconds      Refresh Token 有效期（秒），缺省 30 天
 * @author sunshixiong
 */
public record CreateOAuthClientRequest(

        @NotBlank(message = "客户端 ID 不能为空")
        @Size(max = 100)
        String clientId,

        @Size(max = 200)
        String clientSecret,

        @NotBlank(message = "客户端名称不能为空")
        @Size(max = 200)
        String clientName,

        Set<String> clientAuthenticationMethods,

        Set<String> authorizationGrantTypes,

        Set<String> redirectUris,

        Set<String> scopes,

        Boolean requireProofKey,

        Boolean requireAuthorizationConsent,

        Long accessTokenTtlSeconds,

        Long refreshTokenTtlSeconds
) {

    /**
     * record 没有字段初始化器，原 {@code @Data} 类上的默认值改在这里兜底：
     * 请求 JSON 未带这些字段时反序列化为 null，若不补默认会把行为从「false / 3600」变成空值。
     */
    public CreateOAuthClientRequest {
        requireProofKey = requireProofKey != null ? requireProofKey : Boolean.FALSE;
        requireAuthorizationConsent = requireAuthorizationConsent != null
                ? requireAuthorizationConsent : Boolean.FALSE;
        accessTokenTtlSeconds = accessTokenTtlSeconds != null ? accessTokenTtlSeconds : 3600L;
        refreshTokenTtlSeconds = refreshTokenTtlSeconds != null ? refreshTokenTtlSeconds : 2592000L;
    }
}
