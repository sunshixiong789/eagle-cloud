package com.eagle.auth.infrastructure.security;

import com.eagle.auth.domain.port.OnlineUserPort;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * BlacklistAwareJwtDecoder 单元测试
 *
 * @author sunshixiong
 */
@DisplayName("黑名单感知 JWT 解码器")
@ExtendWith(MockitoExtension.class)
class BlacklistAwareJwtDecoderTest {

    @Mock
    private JWKSource<SecurityContext> jwkSource;

    @Mock
    private OnlineUserPort onlineUserPort;

    @Nested
    @DisplayName("decode")
    class Decode {

        @Test
        @DisplayName("should decode valid non-blacklisted token")
        void shouldDecodeValidNonBlacklistedToken() throws Exception {
            // Given
            BlacklistAwareJwtDecoder decoder = new BlacklistAwareJwtDecoder(jwkSource, onlineUserPort);

            JwtDecoder mockDelegate = mock(JwtDecoder.class);
            Jwt jwt = new Jwt("token-value", Instant.now(), Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"), Map.of("jti", "token-123", "sub", "user1"));

            setDelegate(decoder, mockDelegate);
            when(mockDelegate.decode("test-token")).thenReturn(jwt);
            when(onlineUserPort.isBlacklisted("token-123")).thenReturn(false);

            // When
            Jwt result = decoder.decode("test-token");

            // Then
            assertNotNull(result);
            assertEquals("token-123", result.getId());
            verify(onlineUserPort).isBlacklisted("token-123");
        }

        @Test
        @DisplayName("should throw BadJwtException for blacklisted token")
        void shouldThrowForBlacklistedToken() throws Exception {
            // Given
            BlacklistAwareJwtDecoder decoder = new BlacklistAwareJwtDecoder(jwkSource, onlineUserPort);

            JwtDecoder mockDelegate = mock(JwtDecoder.class);
            Jwt jwt = new Jwt("token-value", Instant.now(), Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"), Map.of("jti", "revoked-token", "sub", "user1"));

            setDelegate(decoder, mockDelegate);
            when(mockDelegate.decode("test-token")).thenReturn(jwt);
            when(onlineUserPort.isBlacklisted("revoked-token")).thenReturn(true);

            // When & Then
            assertThrows(BadJwtException.class, () -> decoder.decode("test-token"));
        }

        @Test
        @DisplayName("should pass through token without jti")
        void shouldPassThroughTokenWithoutJti() throws Exception {
            // Given
            BlacklistAwareJwtDecoder decoder = new BlacklistAwareJwtDecoder(jwkSource, onlineUserPort);

            JwtDecoder mockDelegate = mock(JwtDecoder.class);
            Jwt jwt = new Jwt("token-value", Instant.now(), Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"), Map.of("sub", "user1"));

            setDelegate(decoder, mockDelegate);
            when(mockDelegate.decode("test-token")).thenReturn(jwt);

            // When
            Jwt result = decoder.decode("test-token");

            // Then
            assertNotNull(result);
            assertNull(result.getId());
            verify(onlineUserPort, never()).isBlacklisted(any());
        }
    }

    private void setDelegate(BlacklistAwareJwtDecoder decoder, JwtDecoder delegate) throws Exception {
        Field delegateField = BlacklistAwareJwtDecoder.class.getDeclaredField("delegate");
        delegateField.setAccessible(true);
        delegateField.set(decoder, delegate);
    }
}
