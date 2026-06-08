package com.eagle.auth.core.infrastructure.adapter;

import com.eagle.auth.core.domain.port.OnlineUserInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnlineUserAdapterTest {

    private static final String JTI = "jti-abc";

    @Mock
    StringRedisTemplate redisTemplate;
    @Mock
    ValueOperations<String, String> valueOps;
    @Spy
    ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks
    OnlineUserAdapter adapter;

    /**
     * Mockito 桩：把固定 keys 列表包装为 Cursor。
     * 用 {@link Answers#CALLS_REAL_METHODS} 让 Iterator 的 {@code forEachRemaining} 默认方法
     * 真正调用 {@code hasNext} / {@code next}，否则 mock 默认会把所有方法（包括默认方法）返回空值。
     */
    @SuppressWarnings("unchecked")
    private static Cursor<String> stubCursor(List<String> keys) {
        Iterator<String> it = keys.iterator();
        Cursor<String> cursor = mock(Cursor.class, Answers.CALLS_REAL_METHODS);
        when(cursor.hasNext()).thenAnswer(inv -> it.hasNext());
        when(cursor.next()).thenAnswer(inv -> it.next());
        return cursor;
    }

    private OnlineUserInfo info(long expiresIn) {
        return new OnlineUserInfo(JTI, 1L, "alice", "127.0.0.1",
                LocalDateTime.now(), LocalDateTime.now(),
                "Chrome", "macOS", expiresIn);
    }

    @Nested
    @DisplayName("trackLogin")
    class TrackLogin {
        @Test
        @DisplayName("应设置")
        void shouldSet() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            adapter.trackLogin(info(1800L));
            verify(valueOps).set(eq("online:users:" + JTI), any(String.class),
                    eq(1800L), eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("应吞掉异常")
        void shouldSwallowException() {
            when(redisTemplate.opsForValue())
                    .thenThrow(new RedisConnectionFailureException("redis down"));
            assertDoesNotThrow(() -> adapter.trackLogin(info(1800L)));
        }
    }

    @Nested
    @DisplayName("listOnlineUsers")
    class ListOnlineUsers {
        @Test
        @DisplayName("Redis失败时应返回空")
        void shouldReturnEmptyOnRedisFailure() {
            when(redisTemplate.scan(any(ScanOptions.class)))
                    .thenThrow(new RedisConnectionFailureException("redis down"));
            List<OnlineUserInfo> result = adapter.listOnlineUsers();
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("应跳过Malformed")
        @SuppressWarnings("unchecked")
        void shouldSkipMalformed() {
            // 2 keys: one good JSON, one malformed
            Cursor<String> cursor = stubCursor(List.of("online:users:good", "online:users:bad"));
            when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            String good = "{\"tokenId\":\"jti-1\",\"userId\":1,\"username\":\"alice\","
                    + "\"ip\":\"127.0.0.1\",\"loginTime\":\"2026-05-16T10:00:00\","
                    + "\"lastActiveTime\":\"2026-05-16T11:00:00\","
                    + "\"browser\":\"Chrome\",\"os\":\"macOS\",\"expiresIn\":3600}";
            when(valueOps.get("online:users:good")).thenReturn(good);
            when(valueOps.get("online:users:bad")).thenReturn("{not valid json");

            List<OnlineUserInfo> result = adapter.listOnlineUsers();
            assertEquals(1, result.size());
            assertEquals("jti-1", result.get(0).tokenId());
        }
    }

    @Nested
    @DisplayName("forceLogout")
    class ForceLogout {
        @Test
        @DisplayName("应Use已有TTL")
        void shouldUseExistingTtl() {
            when(redisTemplate.getExpire("online:users:" + JTI, TimeUnit.SECONDS)).thenReturn(120L);
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            adapter.forceLogout(JTI);
            verify(redisTemplate).delete("online:users:" + JTI);
            verify(valueOps).set(eq("token:blacklist:" + JTI), eq("1"),
                    eq(120L), eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("应Use默认TTL")
        void shouldUseDefaultTtl() {
            when(redisTemplate.getExpire("online:users:" + JTI, TimeUnit.SECONDS)).thenReturn(-1L);
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            adapter.forceLogout(JTI);
            verify(valueOps).set(any(), any(), eq(3600L), eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("应吞掉异常")
        void shouldSwallowException() {
            when(redisTemplate.getExpire(any(), any()))
                    .thenThrow(new RedisConnectionFailureException("redis down"));
            assertDoesNotThrow(() -> adapter.forceLogout(JTI));
            verify(redisTemplate, never()).delete((String) any());
        }
    }

    // ---- helpers ----

    @Nested
    @DisplayName("isBlacklisted")
    class IsBlacklisted {
        @Test
        @DisplayName("应返回true")
        void shouldReturnTrue() {
            when(redisTemplate.hasKey("token:blacklist:" + JTI)).thenReturn(true);
            assertTrue(adapter.isBlacklisted(JTI));
        }

        @Test
        @DisplayName("应返回false")
        void shouldReturnFalse() {
            when(redisTemplate.hasKey("token:blacklist:" + JTI)).thenReturn(false);
            assertFalse(adapter.isBlacklisted(JTI));
        }

        @Test
        @DisplayName("Redis失败时应默认false")
        void shouldDefaultFalseOnRedisFailure() {
            when(redisTemplate.hasKey(any(String.class)))
                    .thenThrow(new RedisConnectionFailureException("redis down"));
            assertFalse(adapter.isBlacklisted(JTI));
        }
    }
}
