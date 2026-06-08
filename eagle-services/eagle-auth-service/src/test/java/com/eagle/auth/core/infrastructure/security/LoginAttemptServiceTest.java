package com.eagle.auth.core.infrastructure.security;

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
    @DisplayName("首次失败SetsTTL")
    void firstFailureSetsTtl() {
        when(ops.increment("auth:login-fail:1.1.1.1")).thenReturn(1L);
        service.registerFailure("1.1.1.1");
        verify(redis).expire(eq("auth:login-fail:1.1.1.1"), any(Duration.class));
        verify(ops, never()).set(eq("auth:login-block:1.1.1.1"), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("threshold阻断IP")
    void thresholdBlocksIp() {
        when(ops.increment("auth:login-fail:1.1.1.1")).thenReturn(5L);
        service.registerFailure("1.1.1.1");
        verify(ops).set(eq("auth:login-block:1.1.1.1"), eq("1"), any(Duration.class));
    }

    @Test
    @DisplayName("isBlocked命中")
    void isBlockedHit() {
        when(redis.hasKey("auth:login-block:1.1.1.1")).thenReturn(true);
        assertTrue(service.isBlocked("1.1.1.1"));
    }

    @Test
    @DisplayName("isBlocked失败开放")
    void isBlockedFailOpen() {
        when(redis.hasKey("auth:login-block:1.1.1.1"))
                .thenThrow(new RuntimeException("redis down"));
        assertFalse(service.isBlocked("1.1.1.1"));
    }

    @Test
    @DisplayName("成功Clearskey")
    void successClearsKeys() {
        service.registerSuccess("1.1.1.1");
        verify(redis).delete("auth:login-fail:1.1.1.1");
        verify(redis).delete("auth:login-block:1.1.1.1");
    }

    @Test
    @DisplayName("nullIP无操作")
    void nullIpNoOp() {
        service.registerFailure(null);
        service.registerSuccess("");
        assertFalse(service.isBlocked(""));
        verify(ops, never()).increment(anyString());
    }
}
