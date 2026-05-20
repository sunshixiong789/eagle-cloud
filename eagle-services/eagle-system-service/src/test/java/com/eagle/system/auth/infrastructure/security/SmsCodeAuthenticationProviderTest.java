package com.eagle.system.auth.infrastructure.security;

import com.eagle.common.dto.EagleUser;
import com.eagle.system.auth.application.service.AccountApplicationService;
import com.eagle.system.auth.domain.model.Account;
import com.eagle.system.auth.domain.model.enums.AccountStatus;
import com.eagle.system.auth.domain.model.enums.FreezeReason;
import com.eagle.system.auth.domain.model.valueobject.ProfileHints;
import com.eagle.system.auth.domain.service.SmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SmsCodeAuthenticationProvider")
class SmsCodeAuthenticationProviderTest {

    @Mock OAuth2AuthorizationService authorizationService;
    @Mock @SuppressWarnings("rawtypes") OAuth2TokenGenerator tokenGenerator;
    @Mock UserDetailsService userDetailsService;
    @Mock SmsService smsService;
    @Mock AccountApplicationService accountApplicationService;
    @Mock BlacklistChecker blacklistChecker;

    SmsCodeAuthenticationProvider provider;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        provider = new SmsCodeAuthenticationProvider(
                authorizationService, tokenGenerator, userDetailsService,
                smsService, accountApplicationService, blacklistChecker);

        AuthorizationServerContextHolder.setContext(new AuthorizationServerContext() {
            @Override
            public String getIssuer() { return "https://issuer"; }
            @Override
            public AuthorizationServerSettings getAuthorizationServerSettings() {
                return AuthorizationServerSettings.builder().issuer("https://issuer").build();
            }
        });
    }

    @Nested
    @DisplayName("authenticate")
    class Authenticate {

        @Test
        @DisplayName("returns access token on happy path")
        @SuppressWarnings("unchecked")
        void shouldReturnAccessToken() {
            RegisteredClient client = clientWithGrants(SmsCodeAuthenticationToken.SMS_CODE);
            OAuth2ClientAuthenticationToken clientAuth = authedClient(client);
            SmsCodeAuthenticationToken authToken =
                    new SmsCodeAuthenticationToken("13800138000", "123456", clientAuth, Map.of());

            when(smsService.verifyCode("13800138000", "123456")).thenReturn(true);
            Account account = activeAccount();
            when(accountApplicationService.findOrCreateByPhone("13800138000")).thenReturn(account);
            when(userDetailsService.loadUserByUsername(account.getUsername())).thenReturn(eagleUser());
            when(tokenGenerator.generate(any())).thenReturn(stubJwt());

            OAuth2AccessTokenAuthenticationToken result =
                    (OAuth2AccessTokenAuthenticationToken) provider.authenticate(authToken);

            assertNotNull(result);
            assertEquals(OAuth2AccessToken.TokenType.BEARER, result.getAccessToken().getTokenType());
            verify(authorizationService).save(any());
            verify(blacklistChecker).checkLogin(null, "13800138000", null, null);
        }

        @Test
        @DisplayName("throws unauthorized_client when grant_type not registered for client")
        void shouldRejectGrantTypeNotAllowed() {
            // 只授权 refresh_token，没有 sms_code
            RegisteredClient client = clientWithGrants(AuthorizationGrantType.REFRESH_TOKEN);
            SmsCodeAuthenticationToken authToken = new SmsCodeAuthenticationToken(
                    "13800138000", "123456", authedClient(client), Map.of());

            OAuth2AuthenticationException ex = assertThrows(OAuth2AuthenticationException.class,
                    () -> provider.authenticate(authToken));
            assertEquals("unauthorized_client", ex.getError().getErrorCode());
        }

        @Test
        @DisplayName("throws invalid_grant when sms code is wrong")
        void shouldRejectInvalidCode() {
            RegisteredClient client = clientWithGrants(SmsCodeAuthenticationToken.SMS_CODE);
            SmsCodeAuthenticationToken authToken = new SmsCodeAuthenticationToken(
                    "13800138000", "wrong", authedClient(client), Map.of());

            when(smsService.verifyCode("13800138000", "wrong")).thenReturn(false);

            OAuth2AuthenticationException ex = assertThrows(OAuth2AuthenticationException.class,
                    () -> provider.authenticate(authToken));
            assertEquals("invalid_grant", ex.getError().getErrorCode());
            verify(accountApplicationService, never()).findOrCreateByPhone(any());
        }

        @Test
        @DisplayName("throws account_frozen when target account is frozen")
        void shouldRejectFrozenAccount() {
            RegisteredClient client = clientWithGrants(SmsCodeAuthenticationToken.SMS_CODE);
            SmsCodeAuthenticationToken authToken = new SmsCodeAuthenticationToken(
                    "13800138000", "123456", authedClient(client), Map.of());

            when(smsService.verifyCode("13800138000", "123456")).thenReturn(true);
            Account frozen = activeAccount();
            frozen.freezeByAdmin(99L, "admin", FreezeReason.OTHER, null, "test");
            when(accountApplicationService.findOrCreateByPhone("13800138000")).thenReturn(frozen);

            OAuth2AuthenticationException ex = assertThrows(OAuth2AuthenticationException.class,
                    () -> provider.authenticate(authToken));
            assertEquals("account_frozen", ex.getError().getErrorCode());
            verify(userDetailsService, never()).loadUserByUsername(any());
        }

        @Test
        @DisplayName("invokes blacklist check before sms verification")
        void shouldCheckBlacklistFirst() {
            RegisteredClient client = clientWithGrants(SmsCodeAuthenticationToken.SMS_CODE);
            SmsCodeAuthenticationToken authToken = new SmsCodeAuthenticationToken(
                    "13800138000", "123456", authedClient(client), Map.of());

            // 黑名单抛异常
            org.mockito.Mockito.doThrow(new RuntimeException("blacklisted"))
                    .when(blacklistChecker).checkLogin(null, "13800138000", null, null);

            assertThrows(RuntimeException.class, () -> provider.authenticate(authToken));
            verify(smsService, never()).verifyCode(any(), any());
        }
    }

    @Test
    @DisplayName("supports SmsCodeAuthenticationToken")
    void supportsToken() {
        org.junit.jupiter.api.Assertions.assertTrue(provider.supports(SmsCodeAuthenticationToken.class));
        org.junit.jupiter.api.Assertions.assertFalse(provider.supports(WechatAppAuthenticationToken.class));
    }

    // ====================== helpers ======================

    private static RegisteredClient clientWithGrants(AuthorizationGrantType... grants) {
        RegisteredClient.Builder b = RegisteredClient.withId("c-1")
                .clientId("eagleApp")
                .clientName("App");
        for (AuthorizationGrantType g : grants) {
            b.authorizationGrantType(g);
        }
        return b.scope("openid").build();
    }

    private static OAuth2ClientAuthenticationToken authedClient(RegisteredClient client) {
        return new OAuth2ClientAuthenticationToken(
                client, ClientAuthenticationMethod.NONE, null);
    }

    private static Account activeAccount() {
        Account account = Account.create("alice",
                "$2a$10$Eu6yJzqg.qd3.kmkBeAUkOnhwx7ZNZ8XHaVgkrkR.RZcMz1XqWQGS",
                "13800138000", new ProfileHints(null, null, null));
        return account;
    }

    private static EagleUser eagleUser() {
        return new EagleUser(1L, "alice", "$2a$10$xx", "Alice", "13800138000",
                true, true, true, true, Collections.emptyList());
    }

    private static OAuth2Token stubJwt() {
        Instant iat = Instant.now();
        return Jwt.withTokenValue("token.value")
                .header("alg", "RS256")
                .jti("jti-1")
                .subject("alice")
                .issuedAt(iat)
                .expiresAt(iat.plusSeconds(3600))
                .build();
    }

    @SuppressWarnings("unused")
    private static List<RegisteredClient> noop() {
        return Collections.emptyList();
    }
}
