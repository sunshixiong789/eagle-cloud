package com.eagle.audit.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityAuditLogUserProviderTest {

    private final SecurityAuditLogUserProvider provider = new SecurityAuditLogUserProvider();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("getCurrentUserId")
    class GetCurrentUserId {

        @Test
        @DisplayName("returns null when no authentication")
        void noAuth() {
            assertThat(provider.getCurrentUserId()).isNull();
            assertThat(provider.getCurrentUserName()).isNull();
        }

        @Test
        @DisplayName("prefers JWT user_id claim from JwtAuthenticationToken")
        void jwtUserIdClaim() {
            Jwt jwt = Jwt.withTokenValue("token").header("alg", "none")
                    .claim("user_id", 1024L)
                    .subject("sub-7")
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                    .build();
            JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
            auth.setAuthenticated(true);
            SecurityContextHolder.getContext().setAuthentication(auth);

            assertThat(provider.getCurrentUserId()).isEqualTo("1024");
        }

        @Test
        @DisplayName("falls back to JWT subject when user_id absent")
        void jwtSubject() {
            Jwt jwt = Jwt.withTokenValue("token").header("alg", "none")
                    .subject("alice")
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                    .build();
            JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
            auth.setAuthenticated(true);
            SecurityContextHolder.getContext().setAuthentication(auth);

            assertThat(provider.getCurrentUserId()).isEqualTo("alice");
        }

        @Test
        @DisplayName("extracts JWT from Authentication.getCredentials when not a JwtAuthenticationToken")
        void jwtAsCredentials() {
            Jwt jwt = Jwt.withTokenValue("token").header("alg", "none")
                    .claim("user_id", 9527L).subject("sub")
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                    .build();
            TestingAuthenticationToken auth = new TestingAuthenticationToken("alice", jwt);
            auth.setAuthenticated(true);
            SecurityContextHolder.getContext().setAuthentication(auth);

            assertThat(provider.getCurrentUserId()).isEqualTo("9527");
        }

        @Test
        @DisplayName("falls back to Authentication.getName when no JWT")
        void plainAuth() {
            TestingAuthenticationToken auth =
                    new TestingAuthenticationToken("alice", "password");
            auth.setAuthenticated(true);
            SecurityContextHolder.getContext().setAuthentication(auth);

            assertThat(provider.getCurrentUserId()).isEqualTo("alice");
            assertThat(provider.getCurrentUserName()).isEqualTo("alice");
        }
    }
}
