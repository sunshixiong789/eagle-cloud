package com.eagle.auth.infrastructure.security;

import com.eagle.auth.domain.port.OnlineUserInfo;
import com.eagle.auth.domain.port.OnlineUserPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TokenTrackingHandlerTest {

    @Mock
    OnlineUserPort onlineUserPort;
    @Mock
    OAuth2AuthorizationService authorizationService;
    @InjectMocks
    TokenTrackingHandler handler;

    @AfterEach
    void cleanup() {
        ClientIpHolder.clear();
    }

    /**
     * 构造一个带 metadata claims 的 OAuth2Authorization，用于 TokenTrackingHandler
     * 通过 OAuth2AuthorizationService.findByToken(...) 二次查询取 jti / sub。
     */
    private OAuth2AccessTokenAuthenticationToken buildTokenAuth(String tokenValue,
                                                                String username,
                                                                Map<String, Object> claims,
                                                                Instant expiresAt) {
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                tokenValue,
                Instant.now(),
                expiresAt,
                Set.of("openid", "profile"));

        RegisteredClient client = RegisteredClient.withId("client-id-1")
                .clientId("eagleWeb")
                .authorizationGrantType(new AuthorizationGrantType("password"))
                .build();
        OAuth2ClientAuthenticationToken principal = new OAuth2ClientAuthenticationToken(
                client, ClientAuthenticationMethod.NONE, null);

        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(client)
                .principalName(username)
                .authorizationGrantType(new AuthorizationGrantType("password"))
                .token(accessToken, md -> md.put(
                        OAuth2Authorization.Token.CLAIMS_METADATA_NAME, claims))
                .build();
        when(authorizationService.findByToken(tokenValue, OAuth2TokenType.ACCESS_TOKEN))
                .thenReturn(authorization);

        return new OAuth2AccessTokenAuthenticationToken(
                client, principal, accessToken, null, Map.of("scope", "openid"));
    }

    @Nested
    @DisplayName("onAuthenticationSuccess")
    class Success {

        @Test
        @DisplayName("should write OAuth2 token JSON to response and track online user")
        void shouldWriteResponseAndTrack() throws Exception {
            Instant expiresAt = Instant.now().plusSeconds(3600);
            OAuth2AccessTokenAuthenticationToken tokenAuth = buildTokenAuth(
                    "header.payload.sig", "alice",
                    Map.of("jti", "jti-1", "sub", "alice"), expiresAt);

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("10.0.0.1");
            ClientIpHolder.set("10.0.0.1");
            request.addHeader("User-Agent",
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Safari/537.36");
            MockHttpServletResponse response = new MockHttpServletResponse();

            handler.onAuthenticationSuccess(request, response, tokenAuth);

            String body = response.getContentAsString();
            assertTrue(body.contains("access_token"), "body: " + body);
            assertTrue(body.contains("Bearer"));

            ArgumentCaptor<OnlineUserInfo> captor = ArgumentCaptor.forClass(OnlineUserInfo.class);
            verify(onlineUserPort).trackLogin(captor.capture());
            OnlineUserInfo info = captor.getValue();
            assertEquals("jti-1", info.tokenId());
            assertEquals("alice", info.username());
            assertEquals("10.0.0.1", info.ip());
            assertEquals("Chrome", info.browser());
            assertEquals("macOS", info.os());
        }

        @Test
        @DisplayName("should read client IP from ClientIpHolder (post-trusted-proxy resolution)")
        void shouldUseClientIpHolder() throws Exception {
            OAuth2AccessTokenAuthenticationToken tokenAuth = buildTokenAuth(
                    "h.p.s", "bob", Map.of("jti", "jti-2", "sub", "bob"),
                    Instant.now().plusSeconds(60));
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("10.0.0.99");
            ClientIpHolder.set("203.0.113.5");

            handler.onAuthenticationSuccess(request, new MockHttpServletResponse(), tokenAuth);

            ArgumentCaptor<OnlineUserInfo> captor = ArgumentCaptor.forClass(OnlineUserInfo.class);
            verify(onlineUserPort).trackLogin(captor.capture());
            assertEquals("203.0.113.5", captor.getValue().ip());
        }

        @Test
        @DisplayName("should skip tracking when authorization has no jti claim")
        void shouldSkipWhenNoJti() throws Exception {
            OAuth2AccessTokenAuthenticationToken tokenAuth = buildTokenAuth(
                    "h.p.s", "alice", Map.of("sub", "alice"),
                    Instant.now().plusSeconds(60));
            handler.onAuthenticationSuccess(new MockHttpServletRequest(),
                    new MockHttpServletResponse(), tokenAuth);
            verify(onlineUserPort, never()).trackLogin(any());
        }

        @Test
        @DisplayName("should be a no-op for non-OAuth2 authentication")
        void shouldIgnoreNonOAuth2Auth() throws Exception {
            MockHttpServletResponse response = new MockHttpServletResponse();
            handler.onAuthenticationSuccess(new MockHttpServletRequest(), response,
                    new TestingAuthenticationToken("u", "p"));
            assertEquals(0, response.getContentAsByteArray().length);
            verify(onlineUserPort, never()).trackLogin(any());
        }

        @Test
        @DisplayName("should not propagate tracking failure to the already-written token response")
        void shouldSwallowTrackingFailure() throws Exception {
            OAuth2AccessTokenAuthenticationToken tokenAuth = buildTokenAuth(
                    "h.p.s", "u", Map.of("jti", "jti-x", "sub", "u"),
                    Instant.now().plusSeconds(60));
            doThrow(new RuntimeException("boom")).when(onlineUserPort).trackLogin(any());

            MockHttpServletResponse response = new MockHttpServletResponse();
            assertDoesNotThrow(() -> handler.onAuthenticationSuccess(
                    new MockHttpServletRequest(), response, tokenAuth));
            assertTrue(response.getContentAsString().contains("access_token"));
        }

        @Test
        @DisplayName("should skip tracking when authorization not found")
        void shouldSkipWhenAuthzMissing() throws Exception {
            String tokenValue = "no-authz";
            OAuth2AccessToken accessToken = new OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER, tokenValue,
                    Instant.now(), Instant.now().plusSeconds(60),
                    Set.of("openid"));
            RegisteredClient client = RegisteredClient.withId("c")
                    .clientId("eagleWeb")
                    .authorizationGrantType(new AuthorizationGrantType("password"))
                    .build();
            OAuth2ClientAuthenticationToken principal = new OAuth2ClientAuthenticationToken(
                    client, ClientAuthenticationMethod.NONE, null);
            OAuth2AccessTokenAuthenticationToken tokenAuth =
                    new OAuth2AccessTokenAuthenticationToken(
                            client, principal, accessToken, null, Map.of());
            when(authorizationService.findByToken(eq(tokenValue), eq(OAuth2TokenType.ACCESS_TOKEN)))
                    .thenReturn(null);

            handler.onAuthenticationSuccess(new MockHttpServletRequest(),
                    new MockHttpServletResponse(), tokenAuth);

            verify(onlineUserPort, never()).trackLogin(any());
        }
    }
}
