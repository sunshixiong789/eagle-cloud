package com.eagle.auth.core.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppleAuthenticationConfigTest {

    @Test
    void validatesAppleIssuerAudienceAndTimestamp() {
        AppleAuthenticationProperties properties = new AppleAuthenticationProperties();
        Instant now = Instant.now();
        Jwt valid = token(properties.getIssuer(), List.of(properties.getClientId()), now, now.plusSeconds(300));
        Jwt wrongAudience = token(properties.getIssuer(), List.of("other.app"), now, now.plusSeconds(300));

        var validator = AppleAuthenticationConfig.createValidator(properties);

        assertFalse(validator.validate(valid).hasErrors());
        assertTrue(validator.validate(wrongAudience).hasErrors());
    }

    private Jwt token(String issuer, List<String> audience, Instant issuedAt, Instant expiresAt) {
        return Jwt.withTokenValue("signed-jwt")
                .header("alg", "RS256")
                .issuer(issuer)
                .audience(audience)
                .subject("apple-subject")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
    }
}
