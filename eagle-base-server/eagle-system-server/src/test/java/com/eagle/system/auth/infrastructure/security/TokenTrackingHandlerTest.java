package com.eagle.auth.infrastructure.security;

import com.eagle.auth.domain.port.OnlineUserInfo;
import com.eagle.auth.domain.port.OnlineUserPort;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link TokenTrackingHandler} 单元测试。
 * <p>
 * 使用 fastjson2 解析 JWT payload 中的 jti/sub claim，
 * 测试验证在线用户追踪逻辑的正确性。
 */
@ExtendWith(MockitoExtension.class)
class TokenTrackingHandlerTest {

    @Mock
    private OnlineUserPort onlineUserPort;

    @InjectMocks
    private TokenTrackingHandler handler;

    @Nested
    @DisplayName("onAuthenticationSuccess")
    class OnAuthenticationSuccess {

        @Test
        @DisplayName("should skip when authentication is not OAuth2AccessTokenAuthenticationToken")
        void shouldSkipWhenNotTokenAuth() throws Exception {
            Authentication auth = mock(Authentication.class);
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            handler.onAuthenticationSuccess(request, response, auth);

            verifyNoInteractions(onlineUserPort);
        }

        @Test
        @DisplayName("should track online user when JWT contains jti and sub claims")
        void shouldTrackOnlineUserWhenJwtHasJti() throws Exception {
            String jwtValue = buildFakeJwt("test-jti-123", "admin");
            OAuth2AccessTokenAuthenticationToken tokenAuth = buildTokenAuth(jwtValue);
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("192.168.1.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            handler.onAuthenticationSuccess(request, response, tokenAuth);

            ArgumentCaptor<OnlineUserInfo> captor = ArgumentCaptor.forClass(OnlineUserInfo.class);
            verify(onlineUserPort).trackLogin(captor.capture());
            OnlineUserInfo tracked = captor.getValue();
            assertThat(tracked.tokenId()).isEqualTo("test-jti-123");
            assertThat(tracked.username()).isEqualTo("admin");
            assertThat(tracked.ip()).isEqualTo("192.168.1.1");
        }

        @Test
        @DisplayName("should skip tracking when JWT has no jti claim")
        void shouldSkipTrackingWhenNoJti() throws Exception {
            String jwtValue = buildFakeJwt(null, "admin");
            OAuth2AccessTokenAuthenticationToken tokenAuth = buildTokenAuth(jwtValue);
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            handler.onAuthenticationSuccess(request, response, tokenAuth);

            verifyNoInteractions(onlineUserPort);
        }
    }

    /**
     * 构建仅含 payload 的伪 JWT（无签名验证，仅用于测试 claim 解析）。
     *
     * @param jti jti claim 值，null 表示不包含该 claim
     * @param sub sub claim 值
     */
    private String buildFakeJwt(String jti, String sub) {
        String header = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payloadJson = jti != null
            ? "{\"jti\":\"" + jti + "\",\"sub\":\"" + sub + "\"}"
            : "{\"sub\":\"" + sub + "\"}";
        String payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".fake-signature";
    }

    private OAuth2AccessTokenAuthenticationToken buildTokenAuth(String jwtValue) {
        Instant now = Instant.now();
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER, jwtValue, now, now.plusSeconds(3600));
        OAuth2AccessTokenAuthenticationToken tokenAuth =
            mock(OAuth2AccessTokenAuthenticationToken.class);
        when(tokenAuth.getAccessToken()).thenReturn(accessToken);
        when(tokenAuth.getAdditionalParameters()).thenReturn(Map.of());
        return tokenAuth;
    }
}
