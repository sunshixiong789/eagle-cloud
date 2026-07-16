package com.eagle.auth.core.infrastructure.external;

import com.eagle.auth.core.domain.AuthErrorCode;
import com.eagle.auth.core.domain.service.AppleCredentialCipher;
import com.eagle.common.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppleIdentityServiceImplTest {

    @Test
    void exchangesCodeAndReturnsOnlyServerVerifiedIdentity() {
        JwtDecoder decoder = mock(JwtDecoder.class);
        AppleTokenClient tokenClient = mock(AppleTokenClient.class);
        AppleCredentialCipher cipher = mock(AppleCredentialCipher.class);
        when(decoder.decode("client-jwt")).thenReturn(jwt("client-jwt", true));
        when(decoder.decode("server-jwt")).thenReturn(jwt("server-jwt", false));
        when(tokenClient.exchangeAuthorizationCode("apple-auth-code"))
                .thenReturn(new AppleTokenClient.AppleTokenSet("server-jwt", "refresh-token"));
        when(cipher.encrypt("refresh-token")).thenReturn("encrypted-token");

        var authorization = new AppleIdentityServiceImpl(decoder, tokenClient, cipher)
                .authorize("client-jwt", "apple-auth-code", "nonce-1");

        assertEquals("apple-subject-1", authorization.subject());
        assertEquals("relay@privaterelay.appleid.com", authorization.email());
        assertEquals("encrypted-token", authorization.encryptedRefreshToken());
    }

    @Test
    void rejectsNonceMismatchBeforeCallingAppleTokenEndpoint() {
        JwtDecoder decoder = mock(JwtDecoder.class);
        AppleTokenClient tokenClient = mock(AppleTokenClient.class);
        AppleCredentialCipher cipher = mock(AppleCredentialCipher.class);
        when(decoder.decode("client-jwt")).thenReturn(jwt("client-jwt", true));
        AppleIdentityServiceImpl service = new AppleIdentityServiceImpl(
                decoder, tokenClient, cipher);

        DomainException error = assertThrows(DomainException.class,
                () -> service.authorize("client-jwt", "code", "other-nonce"));

        assertEquals(AuthErrorCode.APPLE_IDENTITY_INVALID, error.getErrorCode());
    }

    @Test
    void rejectsSubjectMismatchBetweenClientAndExchangedTokens() {
        JwtDecoder decoder = mock(JwtDecoder.class);
        AppleTokenClient tokenClient = mock(AppleTokenClient.class);
        AppleCredentialCipher cipher = mock(AppleCredentialCipher.class);
        when(decoder.decode("client-jwt")).thenReturn(jwt("client-jwt", true));
        Jwt otherSubject = Jwt.withTokenValue("server-jwt")
                .header("alg", "RS256")
                .subject("different-subject")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        when(decoder.decode("server-jwt")).thenReturn(otherSubject);
        when(tokenClient.exchangeAuthorizationCode("code"))
                .thenReturn(new AppleTokenClient.AppleTokenSet("server-jwt", "refresh-token"));

        DomainException error = assertThrows(DomainException.class,
                () -> new AppleIdentityServiceImpl(decoder, tokenClient, cipher)
                        .authorize("client-jwt", "code", "nonce-1"));

        assertEquals(AuthErrorCode.APPLE_IDENTITY_INVALID, error.getErrorCode());
    }

    @Test
    void decryptsRefreshTokenBeforeRevocation() {
        JwtDecoder decoder = mock(JwtDecoder.class);
        AppleTokenClient tokenClient = mock(AppleTokenClient.class);
        AppleCredentialCipher cipher = mock(AppleCredentialCipher.class);
        when(cipher.decrypt("encrypted-token")).thenReturn("refresh-token");

        new AppleIdentityServiceImpl(decoder, tokenClient, cipher)
                .revokeEncryptedRefreshToken("encrypted-token");

        verify(tokenClient).revoke("refresh-token");
    }

    @Test
    void translatesJwtVerificationFailure() {
        JwtDecoder decoder = mock(JwtDecoder.class);
        when(decoder.decode("invalid")).thenThrow(new JwtException("bad signature"));

        DomainException error = assertThrows(DomainException.class,
                () -> new AppleIdentityServiceImpl(
                        decoder, mock(AppleTokenClient.class),
                        mock(AppleCredentialCipher.class))
                        .authorize("invalid", "code", "nonce"));

        assertEquals(AuthErrorCode.APPLE_IDENTITY_INVALID, error.getErrorCode());
    }

    private Jwt jwt(String tokenValue, boolean includeNonce) {
        Jwt.Builder builder = Jwt.withTokenValue(tokenValue)
                .header("alg", "RS256")
                .subject("apple-subject-1")
                .claim("email", "relay@privaterelay.appleid.com")
                .claim("email_verified", "true")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300));
        if (includeNonce) {
            builder.claim("nonce", "nonce-1");
        }
        return builder.build();
    }
}
