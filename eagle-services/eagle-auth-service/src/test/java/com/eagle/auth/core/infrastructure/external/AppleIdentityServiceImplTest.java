package com.eagle.auth.core.infrastructure.external;

import com.eagle.auth.core.domain.AuthErrorCode;
import com.eagle.common.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppleIdentityServiceImplTest {

    @Test
    void returnsOnlyServerVerifiedIdentityClaims() {
        JwtDecoder decoder = mock(JwtDecoder.class);
        Jwt jwt = Jwt.withTokenValue("signed-jwt")
                .header("alg", "RS256")
                .subject("apple-subject-1")
                .claim("nonce", "nonce-1")
                .claim("email", "relay@privaterelay.appleid.com")
                .claim("email_verified", "true")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        when(decoder.decode("signed-jwt")).thenReturn(jwt);

        var identity = new AppleIdentityServiceImpl(decoder)
                .verify("signed-jwt", "nonce-1");

        assertEquals("apple-subject-1", identity.subject());
        assertEquals("relay@privaterelay.appleid.com", identity.email());
    }

    @Test
    void rejectsNonceMismatchAndUnverifiedEmail() {
        JwtDecoder decoder = mock(JwtDecoder.class);
        Jwt jwt = Jwt.withTokenValue("signed-jwt")
                .header("alg", "RS256")
                .subject("apple-subject-1")
                .claim("nonce", "nonce-1")
                .claim("email", "unverified@example.com")
                .claim("email_verified", false)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        when(decoder.decode("signed-jwt")).thenReturn(jwt);
        AppleIdentityServiceImpl service = new AppleIdentityServiceImpl(decoder);

        assertNull(service.verify("signed-jwt", "nonce-1").email());
        DomainException error = assertThrows(
                DomainException.class, () -> service.verify("signed-jwt", "other-nonce"));
        assertEquals(AuthErrorCode.APPLE_IDENTITY_INVALID, error.getErrorCode());
    }

    @Test
    void translatesJwtVerificationFailure() {
        JwtDecoder decoder = mock(JwtDecoder.class);
        when(decoder.decode("invalid")).thenThrow(new JwtException("bad signature"));

        DomainException error = assertThrows(
                DomainException.class,
                () -> new AppleIdentityServiceImpl(decoder).verify("invalid", "nonce"));

        assertEquals(AuthErrorCode.APPLE_IDENTITY_INVALID, error.getErrorCode());
    }
}
