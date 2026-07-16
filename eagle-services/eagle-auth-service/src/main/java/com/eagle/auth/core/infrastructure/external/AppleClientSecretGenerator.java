package com.eagle.auth.core.infrastructure.external;

import com.eagle.auth.core.domain.AuthErrorCode;
import com.eagle.auth.core.infrastructure.config.AppleAuthenticationProperties;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

/** 生成调用 Apple REST API 所需的 ES256 client_secret。 */
@Component
public class AppleClientSecretGenerator {

    private static final long CLIENT_SECRET_TTL_MINUTES = 5;

    private final AppleAuthenticationProperties properties;
    private final Clock clock;

    @Autowired
    public AppleClientSecretGenerator(AppleAuthenticationProperties properties) {
        this(properties, Clock.systemUTC());
    }

    /** 测试用构造器：允许注入固定 Clock。 */
    AppleClientSecretGenerator(AppleAuthenticationProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public String generate() {
        ensureEnabled();
        try {
            Instant issuedAt = clock.instant();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(properties.getTeamId())
                    .issueTime(Date.from(issuedAt))
                    .expirationTime(Date.from(issuedAt.plus(
                            CLIENT_SECRET_TTL_MINUTES, ChronoUnit.MINUTES)))
                    .audience(properties.getIssuer())
                    .subject(properties.getClientId())
                    .build();
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.ES256)
                            .type(JOSEObjectType.JWT)
                            .keyID(properties.getKeyId())
                            .build(),
                    claims);
            jwt.sign(new ECDSASigner(parsePrivateKey(properties.getPrivateKey())));
            return jwt.serialize();
        } catch (Exception ex) {
            throw AuthErrorCode.APPLE_NOT_CONFIGURED.toDomainException();
        }
    }

    private ECPrivateKey parsePrivateKey(String pem) throws Exception {
        String normalized = pem.replace("\\n", "\n")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(normalized);
        return (ECPrivateKey) KeyFactory.getInstance("EC")
                .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    private void ensureEnabled() {
        if (!properties.isEnabled() || !properties.isServerCredentialComplete()) {
            throw AuthErrorCode.APPLE_NOT_CONFIGURED.toDomainException();
        }
    }
}
