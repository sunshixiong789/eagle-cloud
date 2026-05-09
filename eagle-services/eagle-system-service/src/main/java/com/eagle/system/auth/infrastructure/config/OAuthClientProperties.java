package com.eagle.system.auth.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * OAuth2 默认客户端配置属性
 *
 * <p>对应 application.yml 中的 {@code eagle.oauth.default-client} 前缀配置。
 * 用于应用启动时预置默认 OAuth2 客户端。</p>
 *
 * @author sunshixiong
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "eagle.oauth.default-client")
public class OAuthClientProperties {

    /**
     * 是否启用默认客户端初始化
     */
    private boolean enabled = true;

    /**
     * 客户端 ID
     */
    private String clientId = "eagleWeb";

    /**
     * 客户端名称
     */
    private String clientName = "Eagle Web 前端应用";

    /**
     * 客户端密钥（留空表示公开客户端）
     */
    private String clientSecret = "";

    /**
     * 认证方式
     */
    private Set<String> clientAuthenticationMethods = Set.of("none");

    /**
     * 授权类型
     */
    private Set<String> authorizationGrantTypes = Set.of(
            "authorization_code", "refresh_token", "wechat_mini_program", "sms_code", "phone_one_click");

    /**
     * 重定向 URI
     */
    private Set<String> redirectUris = Set.of("http://localhost:8080/auth/auth0/sign-in");

    /**
     * 授权范围
     */
    private Set<String> scopes = Set.of("openid", "profile", "email", "address", "phone");

    /**
     * 是否要求 PKCE
     */
    private boolean requireProofKey = true;

    /**
     * 是否要求授权同意
     */
    private boolean requireAuthorizationConsent = false;

    /**
     * Access Token 有效期（秒）
     */
    private long accessTokenTtlSeconds = 3600L;

    /**
     * Refresh Token 有效期（秒）
     */
    private long refreshTokenTtlSeconds = 2592000L;
}
