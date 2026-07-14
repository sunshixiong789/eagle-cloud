package com.eagle.auth.core.infrastructure.external;

import com.eagle.auth.core.domain.AuthErrorCode;
import com.eagle.auth.core.domain.service.AppleIdentityService;
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

    public AppleIdentityServiceImpl(@Qualifier("appleJwtDecoder") JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public AppleIdentity verify(String identityToken, String nonce) {
        try {
            Jwt jwt = jwtDecoder.decode(identityToken);
            String subject = jwt.getSubject();
            String tokenNonce = jwt.getClaimAsString("nonce");
            if (subject == null || subject.isBlank() || !constantTimeEquals(nonce, tokenNonce)) {
                throw AuthErrorCode.APPLE_IDENTITY_INVALID.toDomainException();
            }
            return new AppleIdentity(subject, verifiedEmail(jwt));
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
}
