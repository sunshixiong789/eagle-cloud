package com.eagle.system.auth.infrastructure.security;

import com.eagle.system.auth.domain.port.OnlineUserPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("BlacklistAwareJwtDecoder")
class BlacklistAwareJwtDecoderTest {

    private JwtDecoder delegate;
    private OnlineUserPort onlineUserPort;
    private BlacklistAwareJwtDecoder decoder;

    @BeforeEach
    void setUp() {
        delegate = mock(JwtDecoder.class);
        onlineUserPort = mock(OnlineUserPort.class);
        decoder = new BlacklistAwareJwtDecoder(onlineUserPort, delegate);
    }

    @Test
    @DisplayName("returns decoded jwt when jti not blacklisted")
    void shouldReturnJwtWhenNotBlacklisted() {
        Jwt jwt = jwtWithJti("jti-1");
        when(delegate.decode("token")).thenReturn(jwt);
        when(onlineUserPort.isBlacklisted("jti-1")).thenReturn(false);

        assertSame(jwt, decoder.decode("token"));
    }

    @Test
    @DisplayName("throws BadJwtException when jti is blacklisted")
    void shouldThrowWhenBlacklisted() {
        Jwt jwt = jwtWithJti("jti-2");
        when(delegate.decode("token")).thenReturn(jwt);
        when(onlineUserPort.isBlacklisted("jti-2")).thenReturn(true);

        BadJwtException ex = assertThrows(BadJwtException.class, () -> decoder.decode("token"));
        assertEquals("Token has been revoked", ex.getMessage());
    }

    @Test
    @DisplayName("returns jwt when jti claim absent (no jti = nothing to revoke)")
    void shouldReturnJwtWhenJtiMissing() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("alice")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        when(delegate.decode("token")).thenReturn(jwt);

        assertSame(jwt, decoder.decode("token"));
    }

    private Jwt jwtWithJti(String jti) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .jti(jti)
                .subject("alice")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }
}
