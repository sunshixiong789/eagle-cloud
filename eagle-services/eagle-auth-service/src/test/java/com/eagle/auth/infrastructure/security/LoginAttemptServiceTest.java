package com.eagle.auth.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LoginAttemptService")
class LoginAttemptServiceTest {

    @Mock
    StringRedisTemplate redis;
    @Mock
    ValueOperations<String, String> ops;

    LoginAttemptService service;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(ops);
        service = new LoginAttemptService(redis);
    }

    @Test
    @DisplayName("first failure should set TTL but not write block key")
    void firstFailureSetsTtl() {
        when(ops.increment("auth:login-fail:1.1.1.1")).thenReturn(1L);
        service.registerFailure("1.1.1.1");
        verify(redis).expire(eq("auth:login-fail:1.1.1.1"), any(Duration.class));
        verify(ops, never()).set(eq("auth:login-block:1.1.1.1"), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("hitting MAX_ATTEMPTS writes block key")
    void thresholdBlocksIp() {
        when(ops.increment("auth:login-fail:1.1.1.1")).thenReturn(5L);
        service.registerFailure("1.1.1.1");
        verify(ops).set(eq("auth:login-block:1.1.1.1"), eq("1"), any(Duration.class));
    }

    @Test
    @DisplayName("isBlocked returns true when block key exists")
    void isBlockedHit() {
        when(redis.hasKey("auth:login-block:1.1.1.1")).thenReturn(true);
        assertTrue(service.isBlocked("1.1.1.1"));
    }

    @Test
    @DisplayName("isBlocked returns false when redis throws (fail-open)")
    void isBlockedFailOpen() {
        when(redis.hasKey("auth:login-block:1.1.1.1"))
                .thenThrow(new RuntimeException("redis down"));
        assertFalse(service.isBlocked("1.1.1.1"));
    }

    @Test
    @DisplayName("registerSuccess clears both attempt and block keys")
    void successClearsKeys() {
        service.registerSuccess("1.1.1.1");
        verify(redis).delete("auth:login-fail:1.1.1.1");
        verify(redis).delete("auth:login-block:1.1.1.1");
    }

    @Test
    @DisplayName("null / blank IP is no-op")
    void nullIpNoOp() {
        service.registerFailure(null);
        service.registerSuccess("");
        assertFalse(service.isBlocked(""));
        verify(ops, never()).increment(anyString());
    }
}
