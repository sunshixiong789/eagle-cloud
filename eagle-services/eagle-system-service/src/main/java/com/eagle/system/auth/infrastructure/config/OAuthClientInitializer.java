package com.eagle.system.auth.infrastructure.config;

import com.eagle.system.auth.domain.model.OAuthClient;
import com.eagle.system.auth.domain.repository.OAuthClientRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * OAuth2 客户端初始化器
 * <p>
 * 应用启动时根据 {@link OAuthClientProperties}（web 端，强制 PKCE）和
 * {@link OAuthAppClientProperties}（App 端，关闭 PKCE）配置：
 * <ul>
 *   <li>客户端不存在 → 创建新客户端</li>
 *   <li>客户端已存在 → 同步配置变更（redirect_uris、scopes、grant_types 等）</li>
 * </ul>
 * 可分别通过 {@code eagle.oauth.default-client.enabled=false} /
 * {@code eagle.oauth.app-client.enabled=false} 关闭。
 * {@code @Order(2)} 确保在 AdminInitializer 之后执行。
 *
 * @author sunshixiong
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class OAuthClientInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OAuthClientInitializer.class);

    private final OAuthClientRepository oAuthClientRepository;
    private final OAuthClientProperties webProperties;
    private final OAuthAppClientProperties appProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        initializeIfEnabled(ClientSpec.ofWeb(webProperties), "Web 端");
        initializeIfEnabled(ClientSpec.ofApp(appProperties), "App 端");
    }

    private void initializeIfEnabled(ClientSpec spec, String label) {
        if (!spec.enabled()) {
            log.debug("{}客户端初始化已关闭, clientId: {}", label, spec.clientId());
            return;
        }
        oAuthClientRepository.findByClientId(spec.clientId()).ifPresentOrElse(
                existing -> {
                    if (spec.syncMode() == SyncMode.CREATE_ONLY) {
                        log.info("{}客户端已存在，syncMode=CREATE_ONLY 跳过同步, clientId: {}",
                                label, spec.clientId());
                        return;
                    }
                    syncExistingClient(existing, spec, label);
                },
                () -> createNewClient(spec, label));
    }

    private void createNewClient(ClientSpec spec, String label) {
        String secret = spec.clientSecret();
        OAuthClient client = OAuthClient.create(
                spec.clientId(),
                secret == null || secret.isBlank() ? null : secret,
                spec.clientName(),
                joinSet(spec.clientAuthenticationMethods()),
                joinSet(spec.authorizationGrantTypes()),
                joinSet(spec.redirectUris()),
                joinSet(spec.scopes()));
        client.updateClientSettings(spec.requireProofKey(), spec.requireAuthorizationConsent());
        client.updateTokenSettings(spec.accessTokenTtlSeconds(), spec.refreshTokenTtlSeconds());
        oAuthClientRepository.save(client);
        log.info("{}客户端初始化成功, clientId: {}", label, spec.clientId());
    }

    /**
     * 同步已有客户端的配置（yml 变更后重启即生效，无需手动改库）
     */
    private void syncExistingClient(OAuthClient existing, ClientSpec spec, String label) {
        String configRedirectUris = joinSet(spec.redirectUris());
        String configGrantTypes = joinSet(spec.authorizationGrantTypes());
        String configScopes = joinSet(spec.scopes());
        String configAuthMethods = joinSet(spec.clientAuthenticationMethods());

        boolean changed = false;

        if (!equalsNullable(configRedirectUris, existing.getRedirectUris())
                || !equalsNullable(configGrantTypes, existing.getAuthorizationGrantTypes())
                || !equalsNullable(configScopes, existing.getScopes())
                || !equalsNullable(configAuthMethods, existing.getClientAuthenticationMethods())
                || !equalsNullable(spec.clientName(), existing.getClientName())) {
            existing.updateInfo(
                    spec.clientName(), null,
                    configAuthMethods, configGrantTypes,
                    configRedirectUris, configScopes);
            changed = true;
        }

        if (spec.requireProofKey() != Boolean.TRUE.equals(existing.getRequireProofKey())
                || spec.requireAuthorizationConsent() != Boolean.TRUE.equals(
                existing.getRequireAuthorizationConsent())) {
            existing.updateClientSettings(
                    spec.requireProofKey(), spec.requireAuthorizationConsent());
            changed = true;
        }

        if (spec.accessTokenTtlSeconds() != existing.getAccessTokenTtlSeconds()
                || spec.refreshTokenTtlSeconds() != existing.getRefreshTokenTtlSeconds()) {
            existing.updateTokenSettings(
                    spec.accessTokenTtlSeconds(), spec.refreshTokenTtlSeconds());
            changed = true;
        }

        if (changed) {
            oAuthClientRepository.save(existing);
            log.info("{}客户端配置已同步更新, clientId: {}", label, existing.getClientId());
        } else {
            log.debug("{}客户端配置未变更, clientId: {}", label, existing.getClientId());
        }
    }

    private String joinSet(Set<String> set) {
        return set == null ? "" : String.join(",", set);
    }

    private boolean equalsNullable(String a, String b) {
        if (a == null && b == null) {
            return true;
        }
        return a != null && a.equals(b);
    }

    /**
     * 把两类 Properties 抽象为统一的客户端规格，避免重复实现。
     */
    record ClientSpec(
            boolean enabled,
            SyncMode syncMode,
            String clientId,
            String clientName,
            String clientSecret,
            Set<String> clientAuthenticationMethods,
            Set<String> authorizationGrantTypes,
            Set<String> redirectUris,
            Set<String> scopes,
            boolean requireProofKey,
            boolean requireAuthorizationConsent,
            long accessTokenTtlSeconds,
            long refreshTokenTtlSeconds) {

        static ClientSpec ofWeb(OAuthClientProperties p) {
            return new ClientSpec(p.isEnabled(), p.getSyncMode(),
                    p.getClientId(), p.getClientName(),
                    p.getClientSecret(), p.getClientAuthenticationMethods(),
                    p.getAuthorizationGrantTypes(), p.getRedirectUris(), p.getScopes(),
                    p.isRequireProofKey(), p.isRequireAuthorizationConsent(),
                    p.getAccessTokenTtlSeconds(), p.getRefreshTokenTtlSeconds());
        }

        static ClientSpec ofApp(OAuthAppClientProperties p) {
            return new ClientSpec(p.isEnabled(), p.getSyncMode(),
                    p.getClientId(), p.getClientName(),
                    p.getClientSecret(), p.getClientAuthenticationMethods(),
                    p.getAuthorizationGrantTypes(), p.getRedirectUris(), p.getScopes(),
                    p.isRequireProofKey(), p.isRequireAuthorizationConsent(),
                    p.getAccessTokenTtlSeconds(), p.getRefreshTokenTtlSeconds());
        }
    }
}
