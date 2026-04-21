package com.eagle.auth.infrastructure.adapter;

import com.eagle.auth.domain.port.OnlineUserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class OnlineUserAdapterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private OnlineUserAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new OnlineUserAdapter(redisTemplate);
        // lenient: not all test methods use valueOps
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Nested
    @DisplayName("trackLogin")
    class TrackLogin {

        @Test
        @DisplayName("should write user info to Redis with TTL")
        void shouldWriteToRedisWithTtl() {
            OnlineUserInfo info = new OnlineUserInfo(
                "jti-1", 1L, "admin", "127.0.0.1",
                LocalDateTime.now(), LocalDateTime.now(), "Chrome", "macOS", 3600L
            );
            adapter.trackLogin(info);
            verify(valueOps).set(eq("online:users:jti-1"), anyString(), eq(3600L), eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("should not throw exception when Redis is unavailable")
        void shouldNotThrowWhenRedisUnavailable() {
            OnlineUserInfo info = new OnlineUserInfo(
                "jti-1", 1L, "admin", "127.0.0.1",
                LocalDateTime.now(), LocalDateTime.now(), "Chrome", "macOS", 3600L
            );
            when(valueOps.set(anyString(), anyString(), anyLong(), any())).thenThrow(new RuntimeException("Redis connection failed"));
            
            // Should not throw exception
            adapter.trackLogin(info);
        }
    }

    @Nested
    @DisplayName("forceLogout")
    class ForceLogout {

        @Test
        @DisplayName("should delete online key and add to blacklist")
        void shouldDeleteAndBlacklist() {
            when(redisTemplate.getExpire("online:users:jti-1", TimeUnit.SECONDS)).thenReturn(1800L);
            adapter.forceLogout("jti-1");
            verify(redisTemplate).delete("online:users:jti-1");
            verify(valueOps).set(eq("token:blacklist:jti-1"), eq("1"), eq(1800L), eq(TimeUnit.SECONDS));
        }
    }

    @Nested
    @DisplayName("isBlacklisted")
    class IsBlacklisted {

        @Test
        @DisplayName("should return true when key exists in Redis")
        void shouldReturnTrueWhenBlacklisted() {
            when(redisTemplate.hasKey("token:blacklist:jti-1")).thenReturn(Boolean.TRUE);
            assertThat(adapter.isBlacklisted("jti-1")).isTrue();
        }

        @Test
        @DisplayName("should return false when key does not exist")
        void shouldReturnFalseWhenNotBlacklisted() {
            when(redisTemplate.hasKey("token:blacklist:jti-1")).thenReturn(Boolean.FALSE);
            assertThat(adapter.isBlacklisted("jti-1")).isFalse();
        }

        @Test
        @DisplayName("should return false when Redis is unavailable")
        void shouldReturnFalseWhenRedisUnavailable() {
            when(redisTemplate.hasKey("token:blacklist:jti-1")).thenThrow(new RuntimeException("Redis connection failed"));
            
            // Should return false when Redis is unavailable
            assertThat(adapter.isBlacklisted("jti-1")).isFalse();
        }
    }
}
