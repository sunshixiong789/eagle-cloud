package com.eagle.auth.core.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * OAuth2 App 端公开客户端配置属性。
 *
 * <p>对应 application.yml 中的 {@code eagle.oauth.app-client} 前缀配置。
 * 用于应用启动时预置 App 端使用的 OAuth2 public client。</p>
 *
 * <p>与 {@link OAuthClientProperties}（web 端，强制 PKCE）的区别：本 client 关闭
 * {@code require-proof-key}，仅授权自定义 grant_type（sms_code / wechat_app / wechat_mini_program /
 * phone_one_click）+ refresh_token，以满足移动端无 PKCE 的 token 请求场景；
 * 不开放 {@code authorization_code}，避免被滥用绕过 PKCE。</p>
 *
 * @author sunshixiong
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "eagle.oauth.app-client")
public class OAuthAppClientProperties {

    private boolean enabled = true;

    /**
     * 启动时与 DB 已有配置的同步策略；详见 {@link OAuthClientProperties#getSyncMode}。
     */
    private SyncMode syncMode = SyncMode.OVERWRITE;

    private String clientId = "eagleApp";

    private String clientName = "Eagle App 移动端";

    private String clientSecret = "";

    private Set<String> clientAuthenticationMethods = Set.of("none");

    private Set<String> authorizationGrantTypes = Set.of(
            "refresh_token", "wechat_app", "wechat_mini_program", "sms_code", "phone_one_click");

    private Set<String> redirectUris = Set.of();

    private Set<String> scopes = Set.of("openid", "profile", "email", "address", "phone");

    private boolean requireProofKey = false;

    private boolean requireAuthorizationConsent = false;

    private long accessTokenTtlSeconds = 3600L;

    private long refreshTokenTtlSeconds = 2592000L;
}
