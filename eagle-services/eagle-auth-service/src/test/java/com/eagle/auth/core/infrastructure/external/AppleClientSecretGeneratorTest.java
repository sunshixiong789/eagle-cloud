package com.eagle.auth.core.infrastructure.external;

import com.eagle.auth.core.infrastructure.config.AppleAuthenticationProperties;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppleClientSecretGeneratorTest {

    @Test
    void createsAppleEs256ClientSecretWithExpectedClaims() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        AppleAuthenticationProperties properties = new AppleAuthenticationProperties();
        properties.setEnabled(true);
        properties.setClientId("com.shengxinfast.app");
        properties.setTeamId("NBSUMG9U89");
        properties.setKeyId("KEY1234567");
        properties.setPrivateKey(pem(keyPair));
        Instant now = Instant.parse("2026-07-14T08:00:00Z");

        String value = new AppleClientSecretGenerator(
                properties, Clock.fixed(now, ZoneOffset.UTC)).generate();

        SignedJWT jwt = SignedJWT.parse(value);
        assertTrue(jwt.verify(new ECDSAVerifier((ECPublicKey) keyPair.getPublic())));
        assertEquals("KEY1234567", jwt.getHeader().getKeyID());
        assertEquals("NBSUMG9U89", jwt.getJWTClaimsSet().getIssuer());
        assertEquals("com.shengxinfast.app", jwt.getJWTClaimsSet().getSubject());
        assertEquals("https://appleid.apple.com", jwt.getJWTClaimsSet().getAudience().getFirst());
        assertEquals(now.plusSeconds(300), jwt.getJWTClaimsSet().getExpirationTime().toInstant());
    }

    private String pem(KeyPair keyPair) {
        String content = Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(keyPair.getPrivate().getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" + content + "\n-----END PRIVATE KEY-----";
    }
}
