package com.eagle.system.auth.infrastructure.config;

import com.eagle.system.auth.domain.model.OAuthClient;
import com.eagle.system.auth.domain.repository.OAuthClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link OAuthClientInitializer} 单测。
 *
 * <p>覆盖 sync-mode 决策：
 * <ul>
 *   <li>客户端不存在 → 创建</li>
 *   <li>已存在 + OVERWRITE → 触发同步（行为保留）</li>
 *   <li>已存在 + CREATE_ONLY → 跳过同步</li>
 *   <li>enabled=false → 一律跳过</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OAuthClientInitializer")
class OAuthClientInitializerTest {

    @Mock OAuthClientRepository repository;
    OAuthClientProperties webProperties;
    OAuthAppClientProperties appProperties;
    @InjectMocks OAuthClientInitializer initializer;

    @BeforeEach
    void setUp() {
        webProperties = newWebProps();
        appProperties = newAppProps();
        initializer = new OAuthClientInitializer(repository, webProperties, appProperties);
    }

    @Nested
    @DisplayName("when client missing in DB")
    class WhenMissing {

        @Test
        @DisplayName("should create new client (web)")
        void shouldCreateNew() {
            when(repository.findByClientId(webProperties.getClientId()))
                    .thenReturn(Optional.empty());
            // app client also missing
            when(repository.findByClientId(appProperties.getClientId()))
                    .thenReturn(Optional.empty());
            when(repository.save(any(OAuthClient.class))).thenAnswer(i -> i.getArgument(0));

            initializer.run(new DefaultApplicationArguments());

            ArgumentCaptor<OAuthClient> captor = ArgumentCaptor.forClass(OAuthClient.class);
            verify(repository, org.mockito.Mockito.times(2)).save(captor.capture());
            assertEquals(webProperties.getClientId(), captor.getAllValues().get(0).getClientId());
        }
    }

    @Nested
    @DisplayName("when client exists in DB")
    class WhenExists {

        @Test
        @DisplayName("OVERWRITE: should sync configuration changes")
        void shouldSyncWhenOverwrite() {
            webProperties.setSyncMode(SyncMode.OVERWRITE);
            appProperties.setEnabled(false);

            OAuthClient existing = existingWebClient();
            // 触发"name 变更"
            webProperties.setClientName("Eagle Web Renamed");
            when(repository.findByClientId(webProperties.getClientId()))
                    .thenReturn(Optional.of(existing));
            when(repository.save(any(OAuthClient.class))).thenAnswer(i -> i.getArgument(0));

            initializer.run(new DefaultApplicationArguments());

            verify(repository).save(existing);
            assertEquals("Eagle Web Renamed", existing.getClientName());
        }

        @Test
        @DisplayName("CREATE_ONLY: should skip sync even if config diverges")
        void shouldSkipWhenCreateOnly() {
            webProperties.setSyncMode(SyncMode.CREATE_ONLY);
            webProperties.setClientName("New Name That Should NOT Apply");
            appProperties.setEnabled(false);

            OAuthClient existing = existingWebClient();
            String originalName = existing.getClientName();
            when(repository.findByClientId(webProperties.getClientId()))
                    .thenReturn(Optional.of(existing));

            initializer.run(new DefaultApplicationArguments());

            // 关键：CREATE_ONLY 下绝不调 save
            verify(repository, never()).save(any());
            assertEquals(originalName, existing.getClientName());
        }
    }

    @Nested
    @DisplayName("when enabled=false")
    class WhenDisabled {

        @Test
        @DisplayName("should not touch repository at all")
        void shouldSkipEverything() {
            webProperties.setEnabled(false);
            appProperties.setEnabled(false);

            initializer.run(new DefaultApplicationArguments());

            verify(repository, never()).findByClientId(any());
            verify(repository, never()).save(any());
        }
    }

    // ====================== helpers ======================

    private OAuthClientProperties newWebProps() {
        OAuthClientProperties p = new OAuthClientProperties();
        p.setEnabled(true);
        p.setClientId("eagleWeb");
        p.setClientName("Eagle Web");
        p.setClientSecret("");
        p.setClientAuthenticationMethods(Set.of("none"));
        p.setAuthorizationGrantTypes(Set.of("authorization_code", "refresh_token"));
        p.setRedirectUris(Set.of("http://localhost/cb"));
        p.setScopes(Set.of("openid"));
        p.setRequireProofKey(true);
        p.setRequireAuthorizationConsent(false);
        p.setAccessTokenTtlSeconds(3600);
        p.setRefreshTokenTtlSeconds(86400);
        p.setSyncMode(SyncMode.OVERWRITE);
        return p;
    }

    private OAuthAppClientProperties newAppProps() {
        OAuthAppClientProperties p = new OAuthAppClientProperties();
        p.setEnabled(true);
        p.setClientId("eagleApp");
        p.setClientName("Eagle App");
        p.setClientSecret("");
        p.setClientAuthenticationMethods(Set.of("none"));
        p.setAuthorizationGrantTypes(Set.of("refresh_token", "sms_code"));
        p.setRedirectUris(Set.of());
        p.setScopes(Set.of("openid"));
        p.setRequireProofKey(false);
        p.setRequireAuthorizationConsent(false);
        p.setAccessTokenTtlSeconds(3600);
        p.setRefreshTokenTtlSeconds(86400);
        p.setSyncMode(SyncMode.OVERWRITE);
        return p;
    }

    private OAuthClient existingWebClient() {
        return OAuthClient.create(
                "eagleWeb", null, "Eagle Web",
                "none",
                "authorization_code,refresh_token",
                "http://localhost/cb",
                "openid");
    }
}
