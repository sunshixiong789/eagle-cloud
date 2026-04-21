package com.eagle.auth.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LoginAttemptService 单元测试
 *
 * @author sunshixiong
 */
@DisplayName("登录频率限制服务")
class LoginAttemptServiceTest {

    private LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp() {
        loginAttemptService = new LoginAttemptService();
    }

    @Nested
    @DisplayName("registerFailure")
    class RegisterFailure {

        @Test
        @DisplayName("should not block after single failure")
        void shouldNotBlockAfterSingleFailure() {
            // Given
            String ip = "192.168.1.1";

            // When
            loginAttemptService.registerFailure(ip);

            // Then
            assertFalse(loginAttemptService.isBlocked(ip));
        }

        @Test
        @DisplayName("should not block after 4 failures")
        void shouldNotBlockAfter4Failures() {
            // Given
            String ip = "192.168.1.1";

            // When
            for (int i = 0; i < 4; i++) {
                loginAttemptService.registerFailure(ip);
            }

            // Then
            assertFalse(loginAttemptService.isBlocked(ip));
        }

        @Test
        @DisplayName("should block after 5 failures")
        void shouldBlockAfter5Failures() {
            // Given
            String ip = "192.168.1.1";

            // When
            for (int i = 0; i < 5; i++) {
                loginAttemptService.registerFailure(ip);
            }

            // Then
            assertTrue(loginAttemptService.isBlocked(ip));
        }

        @Test
        @DisplayName("should track different IPs independently")
        void shouldTrackDifferentIPsIndependently() {
            // Given
            String ip1 = "192.168.1.1";
            String ip2 = "192.168.1.2";

            // When
            for (int i = 0; i < 5; i++) {
                loginAttemptService.registerFailure(ip1);
            }
            loginAttemptService.registerFailure(ip2);

            // Then
            assertTrue(loginAttemptService.isBlocked(ip1));
            assertFalse(loginAttemptService.isBlocked(ip2));
        }
    }

    @Nested
    @DisplayName("registerSuccess")
    class RegisterSuccess {

        @Test
        @DisplayName("should clear failure record on success")
        void shouldClearFailureRecordOnSuccess() {
            // Given
            String ip = "192.168.1.1";
            for (int i = 0; i < 5; i++) {
                loginAttemptService.registerFailure(ip);
            }
            assertTrue(loginAttemptService.isBlocked(ip));

            // When
            loginAttemptService.registerSuccess(ip);

            // Then
            assertFalse(loginAttemptService.isBlocked(ip));
        }

        @Test
        @DisplayName("should handle success for unknown IP gracefully")
        void shouldHandleSuccessForUnknownIP() {
            // Given
            String ip = "10.0.0.1";

            // When & Then (no exception)
            loginAttemptService.registerSuccess(ip);
            assertFalse(loginAttemptService.isBlocked(ip));
        }
    }

    @Nested
    @DisplayName("isBlocked")
    class IsBlocked {

        @Test
        @DisplayName("should return false for unknown IP")
        void shouldReturnFalseForUnknownIP() {
            // Given
            String ip = "10.0.0.1";

            // When & Then
            assertFalse(loginAttemptService.isBlocked(ip));
        }

        @Test
        @DisplayName("should remain blocked after reaching threshold")
        void shouldRemainBlockedAfterThreshold() {
            // Given
            String ip = "192.168.1.1";
            for (int i = 0; i < 7; i++) {
                loginAttemptService.registerFailure(ip);
            }

            // When & Then
            assertTrue(loginAttemptService.isBlocked(ip));
        }
    }
}
