package com.eagle.system.auth.infrastructure.security;

import com.eagle.common.exception.ServiceException;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SmsSendRateLimiter")
class SmsSendRateLimiterTest {

    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> ops;

    SmsSendRateLimiter limiter;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(ops);
        limiter = new SmsSendRateLimiter(redis);
    }

    @Test
    @DisplayName("first call sets TTL on minute key")
    void firstCallSetsTtl() {
        when(ops.increment("auth:sms:ip-min:1.1.1.1")).thenReturn(1L);
        when(ops.increment("auth:sms:ip-hour:1.1.1.1")).thenReturn(1L);

        limiter.checkAndIncrement("1.1.1.1");

        verify(redis).expire(eq("auth:sms:ip-min:1.1.1.1"), any(Duration.class));
        verify(redis).expire(eq("auth:sms:ip-hour:1.1.1.1"), any(Duration.class));
    }

    @Test
    @DisplayName("throws ServiceException when minute quota exceeded")
    void minuteOverLimit() {
        when(ops.increment("auth:sms:ip-min:1.1.1.1")).thenReturn(11L);

        assertThrows(ServiceException.class, () -> limiter.checkAndIncrement("1.1.1.1"));
    }

    @Test
    @DisplayName("throws ServiceException when hour quota exceeded")
    void hourOverLimit() {
        when(ops.increment("auth:sms:ip-min:1.1.1.1")).thenReturn(1L);
        when(ops.increment("auth:sms:ip-hour:1.1.1.1")).thenReturn(51L);

        assertThrows(ServiceException.class, () -> limiter.checkAndIncrement("1.1.1.1"));
    }

    @Test
    @DisplayName("null IP is no-op (no redis call)")
    void nullIpNoOp() {
        assertDoesNotThrow(() -> limiter.checkAndIncrement(null));
        assertDoesNotThrow(() -> limiter.checkAndIncrement(""));
    }

    @Test
    @DisplayName("redis failure should not block the request (fail-open)")
    void redisFailureFailsOpen() {
        when(ops.increment(any())).thenThrow(new RuntimeException("redis down"));
        assertDoesNotThrow(() -> limiter.checkAndIncrement("1.1.1.1"));
    }
}
