package com.eagle.system.auth.interfaces.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

/**
 * 创建 OAuth2 客户端请求
 *
 * @author sunshixiong
 */
@Data
public class CreateOAuthClientRequest {

    @NotBlank(message = "客户端 ID 不能为空")
    @Size(max = 100)
    private String clientId;

    @Size(max = 200)
    private String clientSecret;

    @NotBlank(message = "客户端名称不能为空")
    @Size(max = 200)
    private String clientName;

    /**
     * 认证方式集合，如 ["none"], ["client_secret_basic"]
     */
    private Set<String> clientAuthenticationMethods;

    /**
     * 授权类型集合，如 ["authorization_code", "refresh_token"]
     */
    private Set<String> authorizationGrantTypes;

    /**
     * 重定向 URI 集合
     */
    private Set<String> redirectUris;

    /**
     * 授权范围集合，如 ["openid", "profile"]
     */
    private Set<String> scopes;

    /**
     * 是否要求 PKCE
     */
    private Boolean requireProofKey = false;

    /**
     * 是否要求授权同意
     */
    private Boolean requireAuthorizationConsent = false;

    /**
     * Access Token 有效期（秒），默认 3600
     */
    private Long accessTokenTtlSeconds = 3600L;

    /**
     * Refresh Token 有效期（秒），默认 30 天
     */
    private Long refreshTokenTtlSeconds = 2592000L;
}
