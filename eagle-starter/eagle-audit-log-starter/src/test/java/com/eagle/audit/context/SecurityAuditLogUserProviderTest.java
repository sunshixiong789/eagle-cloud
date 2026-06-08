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
        @DisplayName("无认证")
        void noAuth() {
            assertThat(provider.getCurrentUserId()).isNull();
            assertThat(provider.getCurrentUserName()).isNull();
        }

        @Test
        @DisplayName("JWT user_id 声明")
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
        @DisplayName("JWT subject 声明")
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
        @DisplayName("JWT 作为 credentials")
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
        @DisplayName("普通认证")
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
