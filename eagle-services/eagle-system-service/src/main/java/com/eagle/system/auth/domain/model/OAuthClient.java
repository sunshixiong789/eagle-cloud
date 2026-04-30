package com.eagle.system.auth.domain.model;

import com.eagle.common.base.BaseAggregateRoot;
import com.eagle.common.exception.DomainException;
import com.eagle.system.auth.domain.AuthErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * OAuth2 客户端实体（充血模型）
 * <p>
 * 对应 Spring Authorization Server 的 RegisteredClient，
 * <p>
 * 字段设计说明：
 * <ul>
 *   <li>集合类型字段（grantTypes、redirectUris、scopes、authenticationMethods）使用逗号分隔字符串存储</li>
 *   <li>Token 有效期使用秒数存储</li>
 * </ul>
 *
 * @author sunshixiong
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "oauth2_client", comment = "OAuth2 客户端表", indexes = {
        @Index(name = "idx_client_id", columnList = "client_id", unique = true)
})
public class OAuthClient extends BaseAggregateRoot<OAuthClient> {

    @Column(name = "client_id", nullable = false, unique = true, length = 100, comment = "客户端 ID")
    private String clientId;

    @Column(name = "client_secret", length = 200, comment = "客户端密钥（BCrypt 编码）")
    private String clientSecret;

    @Column(name = "client_name", nullable = false, length = 200, comment = "客户端名称")
    private String clientName;

    @Column(name = "client_authentication_methods", nullable = false, length = 1000,
            comment = "认证方式，逗号分隔（none, client_secret_basic, client_secret_post）")
    private String clientAuthenticationMethods;

    @Column(name = "authorization_grant_types", nullable = false, length = 1000,
            comment = "授权类型，逗号分隔（authorization_code, refresh_token, client_credentials 等）")
    private String authorizationGrantTypes;

    @Column(name = "redirect_uris", length = 2000, comment = "重定向 URI，逗号分隔")
    private String redirectUris;

    @Column(name = "scopes", nullable = false, length = 1000, comment = "授权范围，逗号分隔")
    private String scopes;

    @Column(name = "require_proof_key", comment = "是否要求 PKCE")
    private Boolean requireProofKey = false;

    @Column(name = "require_authorization_consent", comment = "是否要求授权同意")
    private Boolean requireAuthorizationConsent = false;

    @Column(name = "access_token_ttl_seconds", nullable = false, comment = "Access Token 有效期（秒）")
    private Long accessTokenTtlSeconds = 3600L;

    @Column(name = "refresh_token_ttl_seconds", nullable = false, comment = "Refresh Token 有效期（秒）")
    private Long refreshTokenTtlSeconds = 2592000L;

    @Column(name = "client_id_issued_at", comment = "客户端 ID 签发时间")
    private Instant clientIdIssuedAt;

    @Column(name = "enabled", nullable = false, comment = "是否启用")
    private Boolean enabled = true;

    // ==================== 业务方法（充血模型）====================

    /**
     * 创建 OAuth2 客户端（静态工厂方法）
     *
     * @param clientId                    客户端 ID（必填，全局唯一）
     * @param clientSecret                客户端密钥（可选，取决于认证方式）
     * @param clientName                  客户端名称（必填）
     * @param clientAuthenticationMethods 认证方式，逗号分隔
     * @param authorizationGrantTypes     授权类型，逗号分隔
     * @param redirectUris                重定向 URI，逗号分隔
     * @param scopes                      授权范围，逗号分隔
     * @return 新创建的客户端实例
     * @throws DomainException 当参数不满足业务规则时
     */
    public static OAuthClient create(String clientId,
                                     String clientSecret,
                                     String clientName,
                                     String clientAuthenticationMethods,
                                     String authorizationGrantTypes,
                                     String redirectUris,
                                     String scopes) {
        if (clientId == null || clientId.isBlank()) {
            throw AuthErrorCode.CLIENT_ID_REQUIRED.toDomainException();
        }
        if (clientName == null || clientName.isBlank()) {
            throw AuthErrorCode.CLIENT_NAME_REQUIRED.toDomainException();
        }
        if (authorizationGrantTypes == null || authorizationGrantTypes.isBlank()) {
            throw AuthErrorCode.CLIENT_GRANT_TYPE_REQ.toDomainException();
        }

        OAuthClient client = new OAuthClient();
        client.clientId = clientId;
        client.clientSecret = clientSecret;
        client.clientName = clientName;
        client.clientAuthenticationMethods = clientAuthenticationMethods != null
                ? clientAuthenticationMethods : "none";
        client.authorizationGrantTypes = authorizationGrantTypes;
        client.redirectUris = redirectUris;
        client.scopes = scopes != null ? scopes : "";
        client.clientIdIssuedAt = Instant.now();
        client.enabled = true;
        return client;
    }

    /**
     * 更新客户端信息
     *
     * @param clientName                  客户端名称
     * @param clientSecret                客户端密钥
     * @param clientAuthenticationMethods 认证方式
     * @param authorizationGrantTypes     授权类型
     * @param redirectUris                重定向 URI
     * @param scopes                      授权范围
     */
    public void updateInfo(String clientName,
                           String clientSecret,
                           String clientAuthenticationMethods,
                           String authorizationGrantTypes,
                           String redirectUris,
                           String scopes) {
        if (clientName != null) {
            this.clientName = clientName;
        }
        if (clientSecret != null) {
            this.clientSecret = clientSecret;
        }
        if (clientAuthenticationMethods != null) {
            this.clientAuthenticationMethods = clientAuthenticationMethods;
        }
        if (authorizationGrantTypes != null) {
            this.authorizationGrantTypes = authorizationGrantTypes;
        }
        if (redirectUris != null) {
            this.redirectUris = redirectUris;
        }
        if (scopes != null) {
            this.scopes = scopes;
        }
    }

    /**
     * 更新 Token 有效期
     *
     * @param accessTokenTtlSeconds  Access Token 有效期（秒）
     * @param refreshTokenTtlSeconds Refresh Token 有效期（秒）
     */
    public void updateTokenSettings(Long accessTokenTtlSeconds, Long refreshTokenTtlSeconds) {
        if (accessTokenTtlSeconds != null && accessTokenTtlSeconds > 0) {
            this.accessTokenTtlSeconds = accessTokenTtlSeconds;
        }
        if (refreshTokenTtlSeconds != null && refreshTokenTtlSeconds > 0) {
            this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
        }
    }

    /**
     * 更新 PKCE 和授权同意设置
     *
     * @param requireProofKey             是否要求 PKCE
     * @param requireAuthorizationConsent 是否要求授权同意
     */
    public void updateClientSettings(Boolean requireProofKey, Boolean requireAuthorizationConsent) {
        if (requireProofKey != null) {
            this.requireProofKey = requireProofKey;
        }
        if (requireAuthorizationConsent != null) {
            this.requireAuthorizationConsent = requireAuthorizationConsent;
        }
    }

    /**
     * 启用客户端
     */
    public void enable() {
        this.enabled = true;
    }

    /**
     * 禁用客户端
     *
     * @throws DomainException 当客户端已被禁用时
     */
    public void disable() {
        if (Boolean.FALSE.equals(this.enabled)) {
            throw AuthErrorCode.CLIENT_DISABLED.toDomainException();
        }
        this.enabled = false;
    }
}
