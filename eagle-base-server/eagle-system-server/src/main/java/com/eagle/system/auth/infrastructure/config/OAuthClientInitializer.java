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
 * OAuth2 默认客户端初始化器
 * <p>
 * 应用启动时根据 {@link OAuthClientProperties} 配置：
 * <ul>
 *   <li>客户端不存在 → 创建新客户端</li>
 *   <li>客户端已存在 → 同步配置变更（redirect_uris、scopes、grant_types 等）</li>
 * </ul>
 * 可通过 {@code eagle.oauth.default-client.enabled=false} 关闭。
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
    private final OAuthClientProperties properties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            log.debug("默认客户端初始化已关闭");
            return;
        }

        String clientId = properties.getClientId();

        oAuthClientRepository.findByClientId(clientId).ifPresentOrElse(
                this::syncExistingClient,
                this::createNewClient
        );
    }

    private void createNewClient() {
        String secret = properties.getClientSecret();

        OAuthClient client = OAuthClient.create(
                properties.getClientId(),
                secret.isBlank() ? null : secret,
                properties.getClientName(),
                joinSet(properties.getClientAuthenticationMethods()),
                joinSet(properties.getAuthorizationGrantTypes()),
                joinSet(properties.getRedirectUris()),
                joinSet(properties.getScopes())
        );
        client.updateClientSettings(
                properties.isRequireProofKey(), properties.isRequireAuthorizationConsent());
        client.updateTokenSettings(
                properties.getAccessTokenTtlSeconds(), properties.getRefreshTokenTtlSeconds());

        oAuthClientRepository.save(client);
        log.info("默认客户端初始化成功, clientId: {}", properties.getClientId());
    }

    /**
     * 同步已有客户端的配置（yml 变更后重启即生效，无需手动改库）
     */
    private void syncExistingClient(OAuthClient existing) {
        String configRedirectUris = joinSet(properties.getRedirectUris());
        String configGrantTypes = joinSet(properties.getAuthorizationGrantTypes());
        String configScopes = joinSet(properties.getScopes());
        String configAuthMethods = joinSet(properties.getClientAuthenticationMethods());

        boolean changed = false;

        if (!equalsNullable(configRedirectUris, existing.getRedirectUris())
                || !equalsNullable(configGrantTypes, existing.getAuthorizationGrantTypes())
                || !equalsNullable(configScopes, existing.getScopes())
                || !equalsNullable(configAuthMethods, existing.getClientAuthenticationMethods())
                || !equalsNullable(properties.getClientName(), existing.getClientName())) {
            existing.updateInfo(
                    properties.getClientName(), null,
                    configAuthMethods, configGrantTypes,
                    configRedirectUris, configScopes);
            changed = true;
        }

        if (properties.isRequireProofKey() != Boolean.TRUE.equals(existing.getRequireProofKey())
                || properties.isRequireAuthorizationConsent() != Boolean.TRUE.equals(
                existing.getRequireAuthorizationConsent())) {
            existing.updateClientSettings(
                    properties.isRequireProofKey(), properties.isRequireAuthorizationConsent());
            changed = true;
        }

        if (properties.getAccessTokenTtlSeconds() != existing.getAccessTokenTtlSeconds()
                || properties.getRefreshTokenTtlSeconds() != existing.getRefreshTokenTtlSeconds()) {
            existing.updateTokenSettings(
                    properties.getAccessTokenTtlSeconds(), properties.getRefreshTokenTtlSeconds());
            changed = true;
        }

        if (changed) {
            oAuthClientRepository.save(existing);
            log.info("默认客户端配置已同步更新, clientId: {}", existing.getClientId());
        } else {
            log.debug("默认客户端配置未变更, clientId: {}", existing.getClientId());
        }
    }

    private String joinSet(Set<String> set) {
        return String.join(",", set);
    }

    private boolean equalsNullable(String a, String b) {
        if (a == null && b == null) {
            return true;
        }
        return a != null && a.equals(b);
    }
}
