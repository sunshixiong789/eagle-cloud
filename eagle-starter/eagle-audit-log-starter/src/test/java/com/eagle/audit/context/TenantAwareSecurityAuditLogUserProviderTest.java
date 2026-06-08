package com.eagle.audit.context;

import com.eagle.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class TenantAwareSecurityAuditLogUserProviderTest {

    private final TenantAwareSecurityAuditLogUserProvider provider =
            new TenantAwareSecurityAuditLogUserProvider();

    @AfterEach
    void clear() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("getCurrentTenantId")
    class GetCurrentTenantId {

        @Test
        @DisplayName("空线程本地")
        void emptyThreadLocal() {
            assertThat(provider.getCurrentTenantId()).isNull();
        }

        @Test
        @DisplayName("propagates从线程本地")
        void propagatesFromThreadLocal() {
            TenantContextHolder.setTenantId("tenant-007");

            assertThat(provider.getCurrentTenantId()).isEqualTo("tenant-007");
        }

        @Test
        @DisplayName("inherits用户Resolution")
        void inheritsUserResolution() {
            TenantContextHolder.setTenantId("tenant-007");
            TestingAuthenticationToken auth =
                    new TestingAuthenticationToken("alice", "password");
            auth.setAuthenticated(true);
            SecurityContextHolder.getContext().setAuthentication(auth);

            assertThat(provider.getCurrentUserId()).isEqualTo("alice");
            assertThat(provider.getCurrentUserName()).isEqualTo("alice");
            assertThat(provider.getCurrentTenantId()).isEqualTo("tenant-007");
        }
    }
}
