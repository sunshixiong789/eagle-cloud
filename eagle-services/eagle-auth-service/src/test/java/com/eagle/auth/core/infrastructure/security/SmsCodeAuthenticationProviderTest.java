package com.eagle.auth.core.infrastructure.security;

import com.eagle.common.dto.EagleUser;
import com.eagle.auth.core.application.service.AccountApplicationService;
import com.eagle.auth.core.domain.model.Account;
import com.eagle.auth.core.domain.model.enums.FreezeReason;
import com.eagle.auth.core.domain.model.valueobject.ProfileHints;
import com.eagle.auth.core.domain.service.SmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SmsCodeAuthenticationProvider")
class SmsCodeAuthenticationProviderTest {

    @Mock
    OAuth2AuthorizationService authorizationService;
    @Mock
    @SuppressWarnings("rawtypes")
    OAuth2TokenGenerator tokenGenerator;
    @Mock
    UserDetailsService userDetailsService;
    @Mock
    SmsService smsService;
    @Mock
    AccountApplicationService accountApplicationService;
    @Mock
    BlacklistChecker blacklistChecker;

    SmsCodeAuthenticationProvider provider;

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

    // ====================== helpers ======================

    private static EagleUser eagleUser() {
        return new EagleUser(1L, "alice", "$2a$10$xx", "Alice", "13800138000", null,
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

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        provider = new SmsCodeAuthenticationProvider(
                authorizationService, tokenGenerator, userDetailsService,
                smsService, accountApplicationService, blacklistChecker);

        AuthorizationServerContextHolder.setContext(new AuthorizationServerContext() {
            @Override
            public String getIssuer() {
                return "https://issuer";
            }

            @Override
            public AuthorizationServerSettings getAuthorizationServerSettings() {
                return AuthorizationServerSettings.builder().issuer("https://issuer").build();
            }
        });
    }

    @Test
    @DisplayName("支持令牌")
    void supportsToken() {
        org.junit.jupiter.api.Assertions.assertTrue(provider.supports(SmsCodeAuthenticationToken.class));
        org.junit.jupiter.api.Assertions.assertFalse(provider.supports(WechatAppAuthenticationToken.class));
    }

    @Nested
    @DisplayName("authenticate")
    class Authenticate {

        @Test
        @DisplayName("应返回Access令牌")
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
        @DisplayName("登录成功应发布 AuthenticationSuccessEvent 供登录日志/防护计数使用")
        @SuppressWarnings("unchecked")
        void shouldPublishAuthenticationSuccessEventOnLogin() {
            RegisteredClient client = clientWithGrants(SmsCodeAuthenticationToken.SMS_CODE);
            OAuth2ClientAuthenticationToken clientAuth = authedClient(client);
            SmsCodeAuthenticationToken authToken =
                    new SmsCodeAuthenticationToken("13800138000", "123456", clientAuth, Map.of());

            when(smsService.verifyCode("13800138000", "123456")).thenReturn(true);
            Account account = activeAccount();
            when(accountApplicationService.findOrCreateByPhone("13800138000")).thenReturn(account);
            EagleUser user = eagleUser();
            when(userDetailsService.loadUserByUsername(account.getUsername())).thenReturn(user);
            when(tokenGenerator.generate(any())).thenReturn(stubJwt());

            ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
            provider.setApplicationEventPublisher(eventPublisher);

            provider.authenticate(authToken);

            ArgumentCaptor<AuthenticationSuccessEvent> captor =
                    ArgumentCaptor.forClass(AuthenticationSuccessEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            // principal 必须是 EagleUser，登录日志监听器才能解析 accountId/username
            assertSame(user, captor.getValue().getAuthentication().getPrincipal());
        }

        @Test
        @DisplayName("应拒绝授权类型不Allowed")
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
        @DisplayName("应拒绝无效验证码")
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
        @DisplayName("应拒绝已冻结账号")
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
        @DisplayName("应Check黑名单首次")
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
}
