package com.eagle.auth.core.infrastructure.external;

import com.eagle.auth.core.domain.AuthErrorCode;
import com.eagle.auth.core.domain.service.AppleCredentialCipher;
import com.eagle.auth.core.domain.service.AppleIdentityService;
import com.eagle.auth.core.infrastructure.external.AppleTokenClient.AppleTokenSet;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 使用 Apple 公钥集验证 identity token 的基础设施适配器。
 */
@Service
public class AppleIdentityServiceImpl implements AppleIdentityService {

    private final JwtDecoder jwtDecoder;
    private final AppleTokenClient tokenClient;
    private final AppleCredentialCipher credentialCipher;

    public AppleIdentityServiceImpl(
            @Qualifier("appleJwtDecoder") JwtDecoder jwtDecoder,
            AppleTokenClient tokenClient,
            AppleCredentialCipher credentialCipher) {
        this.jwtDecoder = jwtDecoder;
        this.tokenClient = tokenClient;
        this.credentialCipher = credentialCipher;
    }

    @Override
    public AppleAuthorization authorize(
            String identityToken, String authorizationCode, String nonce) {
        TrustedIdentity clientIdentity = verifyIdentityToken(identityToken, nonce, true);
        AppleTokenSet tokenSet = tokenClient.exchangeAuthorizationCode(authorizationCode);
        TrustedIdentity exchangedIdentity = verifyIdentityToken(
                tokenSet.identityToken(), null, false);
        if (!constantTimeEquals(clientIdentity.subject(), exchangedIdentity.subject())) {
            throw AuthErrorCode.APPLE_IDENTITY_INVALID.toDomainException();
        }
        String encryptedRefreshToken = credentialCipher.encrypt(tokenSet.refreshToken());
        String email = exchangedIdentity.email() != null
                ? exchangedIdentity.email() : clientIdentity.email();
        return new AppleAuthorization(
                clientIdentity.subject(), email, encryptedRefreshToken);
    }

    @Override
    public void revokeEncryptedRefreshToken(String encryptedRefreshToken) {
        tokenClient.revoke(credentialCipher.decrypt(encryptedRefreshToken));
    }

    private TrustedIdentity verifyIdentityToken(
            String identityToken, String nonce, boolean nonceRequired) {
        try {
            Jwt jwt = jwtDecoder.decode(identityToken);
            String subject = jwt.getSubject();
            String tokenNonce = jwt.getClaimAsString("nonce");
            if (subject == null || subject.isBlank()
                    || (nonceRequired && !constantTimeEquals(nonce, tokenNonce))) {
                throw AuthErrorCode.APPLE_IDENTITY_INVALID.toDomainException();
            }
            return new TrustedIdentity(subject, verifiedEmail(jwt));
        } catch (JwtException ex) {
            throw AuthErrorCode.APPLE_IDENTITY_INVALID.toDomainException();
        }
    }

    private String verifiedEmail(Jwt jwt) {
        Object emailVerified = jwt.getClaim("email_verified");
        boolean verified = Boolean.TRUE.equals(emailVerified)
                || "true".equalsIgnoreCase(String.valueOf(emailVerified));
        return verified ? jwt.getClaimAsString("email") : null;
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    private record TrustedIdentity(String subject, String email) {
    }
}
