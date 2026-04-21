package com.eagle.system.auth.infrastructure.persistence;

import com.eagle.system.auth.domain.model.OAuthClient;
import com.eagle.system.auth.domain.repository.OAuthClientRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于 JPA 的 RegisteredClientRepository 实现
 * <p>
 * 桥接 Spring Authorization Server 框架与项目的 JPA 实体。
 * 将 {@link OAuthClient} 转换为 {@link RegisteredClient} 供框架使用。
 *
 * @author sunshixiong
 */
@Component
@RequiredArgsConstructor
public class JpaRegisteredClientRepository implements RegisteredClientRepository {

    private static final Logger log = LoggerFactory.getLogger(JpaRegisteredClientRepository.class);

    private final OAuthClientRepository oAuthClientRepository;

    @Override
    public void save(RegisteredClient registeredClient) {
        OAuthClient existing = oAuthClientRepository.findByClientId(registeredClient.getClientId())
                .orElse(null);

        if (existing != null) {
            existing.updateInfo(
                    registeredClient.getClientName(),
                    registeredClient.getClientSecret(),
                    toMethodsString(registeredClient),
                    toGrantTypesString(registeredClient),
                    String.join(",", registeredClient.getRedirectUris()),
                    String.join(",", registeredClient.getScopes())
            );
            oAuthClientRepository.save(existing);
        } else {
            OAuthClient client = OAuthClient.create(
                    registeredClient.getClientId(),
                    registeredClient.getClientSecret(),
                    registeredClient.getClientName(),
                    toMethodsString(registeredClient),
                    toGrantTypesString(registeredClient),
                    String.join(",", registeredClient.getRedirectUris()),
                    String.join(",", registeredClient.getScopes())
            );
            TokenSettings tokenSettings = registeredClient.getTokenSettings();
            client.updateTokenSettings(
                    tokenSettings.getAccessTokenTimeToLive().getSeconds(),
                    tokenSettings.getRefreshTokenTimeToLive().getSeconds()
            );
            ClientSettings clientSettings = registeredClient.getClientSettings();
            client.updateClientSettings(
                    clientSettings.isRequireProofKey(),
                    clientSettings.isRequireAuthorizationConsent()
            );
            oAuthClientRepository.save(client);
        }
        log.info("OAuth2 客户端已保存, clientId: {}", registeredClient.getClientId());
    }

    @Override
    public RegisteredClient findById(String id) {
        return oAuthClientRepository.findByClientId(id)
                .filter(OAuthClient::getEnabled)
                .map(this::toRegisteredClient)
                .orElse(null);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        return oAuthClientRepository.findByClientId(clientId)
                .filter(OAuthClient::getEnabled)
                .map(this::toRegisteredClient)
                .orElse(null);
    }

    /**
     * 将 JPA 实体转换为 Spring RegisteredClient
     */
    private RegisteredClient toRegisteredClient(OAuthClient entity) {
        RegisteredClient.Builder builder = RegisteredClient
                .withId(entity.getId().toString())
                .clientId(entity.getClientId())
                .clientName(entity.getClientName())
                .clientIdIssuedAt(entity.getClientIdIssuedAt());

        if (entity.getClientSecret() != null && !entity.getClientSecret().isBlank()) {
            builder.clientSecret(entity.getClientSecret());
        }

        // 认证方式
        parseCommaSeparated(entity.getClientAuthenticationMethods())
                .forEach(m -> builder.clientAuthenticationMethod(
                        new ClientAuthenticationMethod(m)));

        // 授权类型
        parseCommaSeparated(entity.getAuthorizationGrantTypes())
                .forEach(g -> builder.authorizationGrantType(
                        new AuthorizationGrantType(g)));

        // 重定向 URI
        if (entity.getRedirectUris() != null && !entity.getRedirectUris().isBlank()) {
            parseCommaSeparated(entity.getRedirectUris())
                    .forEach(builder::redirectUri);
        }

        // 授权范围
        if (entity.getScopes() != null && !entity.getScopes().isBlank()) {
            parseCommaSeparated(entity.getScopes())
                    .forEach(builder::scope);
        }

        // Token 设置
        builder.tokenSettings(TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofSeconds(entity.getAccessTokenTtlSeconds()))
                .refreshTokenTimeToLive(Duration.ofSeconds(entity.getRefreshTokenTtlSeconds()))
                .build());

        // 客户端设置
        builder.clientSettings(ClientSettings.builder()
                .requireProofKey(Boolean.TRUE.equals(entity.getRequireProofKey()))
                .requireAuthorizationConsent(Boolean.TRUE.equals(entity.getRequireAuthorizationConsent()))
                .build());

        return builder.build();
    }

    private List<String> parseCommaSeparated(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private String toMethodsString(RegisteredClient client) {
        return client.getClientAuthenticationMethods().stream()
                .map(ClientAuthenticationMethod::getValue)
                .collect(Collectors.joining(","));
    }

    private String toGrantTypesString(RegisteredClient client) {
        return client.getAuthorizationGrantTypes().stream()
                .map(AuthorizationGrantType::getValue)
                .collect(Collectors.joining(","));
    }
}
