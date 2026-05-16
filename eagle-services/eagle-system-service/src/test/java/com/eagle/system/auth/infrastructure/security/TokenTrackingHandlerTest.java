package com.eagle.system.auth.infrastructure.security;

import com.alibaba.fastjson2.JSON;
import com.eagle.system.auth.domain.port.OnlineUserInfo;
import com.eagle.system.auth.domain.port.OnlineUserPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TokenTrackingHandlerTest {

    @Mock OnlineUserPort onlineUserPort;
    @InjectMocks TokenTrackingHandler handler;

    /** 构造一个仅含 header.payload（无签名）的 JWT 字符串，便于测试 claim 解析。 */
    private static String buildJwt(Map<String, Object> claims) {
        Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
        String header = enc.encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = enc.encodeToString(JSON.toJSONBytes(claims));
        return header + "." + payload + ".";
    }

    private OAuth2AccessTokenAuthenticationToken buildTokenAuth(Map<String, Object> claims, Instant expiresAt) {
        String tokenValue = buildJwt(claims);
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                tokenValue,
                Instant.now(),
                expiresAt,
                Set.of("openid", "profile"));
        RegisteredClient client = RegisteredClient.withId("client-id-1")
                .clientId("eagleWeb")
                .authorizationGrantType(new org.springframework.security.oauth2.core.AuthorizationGrantType("password"))
                .build();
        OAuth2ClientAuthenticationToken principal = new OAuth2ClientAuthenticationToken(
                client, org.springframework.security.oauth2.core.ClientAuthenticationMethod.NONE, null);
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
                    Map.of("jti", "jti-1", "sub", "alice"), expiresAt);

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("10.0.0.1");
            request.addHeader("User-Agent",
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Safari/537.36");
            MockHttpServletResponse response = new MockHttpServletResponse();

            handler.onAuthenticationSuccess(request, response, tokenAuth);

            // token response written as JSON
            String body = response.getContentAsString();
            assertTrue(body.contains("access_token"), "body: " + body);
            OAuth2AccessTokenResponse parsed = JSON.parseObject(body, OAuth2AccessTokenResponse.class);
            // parsing the body through fastjson is best-effort; only verify essentials via raw content
            assertTrue(body.contains("Bearer"));

            // online user tracked with extracted claims
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
        @DisplayName("should prefer X-Forwarded-For when client IP header is present")
        void shouldUseXForwardedFor() throws Exception {
            OAuth2AccessTokenAuthenticationToken tokenAuth = buildTokenAuth(
                    Map.of("jti", "jti-2", "sub", "bob"), Instant.now().plusSeconds(60));
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("10.0.0.99");
            request.addHeader("X-Forwarded-For", "203.0.113.5, 198.51.100.42");
            handler.onAuthenticationSuccess(request, new MockHttpServletResponse(), tokenAuth);

            ArgumentCaptor<OnlineUserInfo> captor = ArgumentCaptor.forClass(OnlineUserInfo.class);
            verify(onlineUserPort).trackLogin(captor.capture());
            assertEquals("203.0.113.5", captor.getValue().ip());
        }

        @Test
        @DisplayName("should skip tracking when token has no jti claim")
        void shouldSkipWhenNoJti() throws Exception {
            OAuth2AccessTokenAuthenticationToken tokenAuth = buildTokenAuth(
                    Map.of("sub", "alice"), Instant.now().plusSeconds(60));
            handler.onAuthenticationSuccess(new MockHttpServletRequest(),
                    new MockHttpServletResponse(), tokenAuth);
            verify(onlineUserPort, never()).trackLogin(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("should be a no-op for non-OAuth2 authentication")
        void shouldIgnoreNonOAuth2Auth() throws Exception {
            MockHttpServletResponse response = new MockHttpServletResponse();
            handler.onAuthenticationSuccess(new MockHttpServletRequest(), response,
                    new TestingAuthenticationToken("u", "p"));
            assertEquals(0, response.getContentAsByteArray().length);
            verify(onlineUserPort, never()).trackLogin(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("should not propagate tracking failure to the already-written token response")
        void shouldSwallowTrackingFailure() throws Exception {
            doThrow(new RuntimeException("boom")).when(onlineUserPort)
                    .trackLogin(org.mockito.ArgumentMatchers.any());
            OAuth2AccessTokenAuthenticationToken tokenAuth = buildTokenAuth(
                    Map.of("jti", "jti-x", "sub", "u"), Instant.now().plusSeconds(60));

            MockHttpServletResponse response = new MockHttpServletResponse();
            assertDoesNotThrow(() -> handler.onAuthenticationSuccess(
                    new MockHttpServletRequest(), response, tokenAuth));
            assertTrue(response.getContentAsString().contains("access_token"));
        }
    }
}
