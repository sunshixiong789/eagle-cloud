package com.eagle.redis.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CacheProtectionUtil}.
 */
@ExtendWith(MockitoExtension.class)
class CacheProtectionUtilTest {

    private static final String CACHE_KEY = "product:123";
    private static final String MUTEX_KEY_PREFIX = "eagle:mutex:";
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOps;
    @Mock
    private RLock rLock;
    private CacheProtectionUtil cacheProtectionUtil;

    @BeforeEach
    void setUp() {
        cacheProtectionUtil = new CacheProtectionUtil(redissonClient, redisTemplate);
    }

    @Nested
    @DisplayName("getWithMutex")
    class GetWithMutex {

        @BeforeEach
        void setUpOpsForValue() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
        }

        @Test
        @DisplayName("获取使用互斥锁：命中时应返回从缓存")
        void getWithMutex_shouldReturnFromCacheOnHit() {
            String cachedValue = "cached-product";
            when(valueOps.get(CACHE_KEY)).thenReturn(cachedValue);

            String result = cacheProtectionUtil.getWithMutex(
                    CACHE_KEY, Duration.ofMinutes(10), () -> "db-product", String.class);

            assertEquals(cachedValue, result);
            // Loader should never be called — no lock acquisition needed
            verify(redissonClient, never()).getLock(anyString());
        }

        @Test
        @DisplayName("获取使用互斥锁：缓存未命中时应调用Loader")
        void getWithMutex_shouldCallLoaderOnCacheMiss() throws InterruptedException {
            when(valueOps.get(CACHE_KEY))
                    .thenReturn(null)   // 1st call: cache miss
                    .thenReturn(null);  // 2nd call: double-check after lock acquisition
            when(redissonClient.getLock(MUTEX_KEY_PREFIX + CACHE_KEY)).thenReturn(rLock);
            when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
            when(rLock.isHeldByCurrentThread()).thenReturn(true);

            String result = cacheProtectionUtil.getWithMutex(
                    CACHE_KEY, Duration.ofMinutes(10), () -> "db-product", String.class);

            assertEquals("db-product", result);
            verify(valueOps).set(eq(CACHE_KEY), eq("db-product"), any(Duration.class));
            verify(rLock).unlock();
        }

        @Test
        @DisplayName("获取使用互斥锁：空Loader时应返回nullPlaceholder")
        void getWithMutex_shouldReturnNullPlaceholderOnEmptyLoader() throws InterruptedException {
            when(valueOps.get(CACHE_KEY))
                    .thenReturn(null)   // 1st call: cache miss
                    .thenReturn(null);  // 2nd call: double-check after lock acquisition
            when(redissonClient.getLock(MUTEX_KEY_PREFIX + CACHE_KEY)).thenReturn(rLock);
            when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
            when(rLock.isHeldByCurrentThread()).thenReturn(true);

            String result = cacheProtectionUtil.getWithMutex(
                    CACHE_KEY, Duration.ofMinutes(10), () -> null, String.class);

            // Method should return null (not the placeholder string)
            assertNull(result);
            // Null placeholder "__NULL__" should be written to Redis
            verify(valueOps).set(eq(CACHE_KEY), eq("__NULL__"), any(Duration.class));
        }

        @Test
        @DisplayName("获取使用互斥锁：PlaceholderIs命中时应返回null")
        void getWithMutex_shouldReturnNullWhenPlaceholderIsHit() {
            when(valueOps.get(CACHE_KEY)).thenReturn("__NULL__");

            String result = cacheProtectionUtil.getWithMutex(
                    CACHE_KEY, Duration.ofMinutes(10), () -> "db-value", String.class);

            assertNull(result);
        }

        @Test
        @DisplayName("获取使用互斥锁：应Use互斥锁Lock")
        void getWithMutex_shouldUseMutexLock() throws InterruptedException {
            when(valueOps.get(CACHE_KEY))
                    .thenReturn(null)
                    .thenReturn(null);
            when(redissonClient.getLock(MUTEX_KEY_PREFIX + CACHE_KEY)).thenReturn(rLock);
            when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
            when(rLock.isHeldByCurrentThread()).thenReturn(true);

            cacheProtectionUtil.getWithMutex(
                    CACHE_KEY, Duration.ofMinutes(10), () -> "value", String.class);

            verify(rLock).tryLock(anyLong(), anyLong(), any(TimeUnit.class));
            verify(rLock).unlock();
        }
    }

    @Nested
    @DisplayName("jitter")
    class Jitter {

        @Test
        @DisplayName("jitter：使用在Range时应返回时长")
        void jitter_shouldReturnDurationWithinRange() {
            Duration base = Duration.ofSeconds(60);
            double ratio = 0.2;

            Duration result = cacheProtectionUtil.jitter(base, ratio);

            assertNotNull(result);
            // Range: [60s * (1 - 0.2), 60s * (1 + 0.2)] = [48s, 72s]
            assertTrue(result.toMillis() >= Duration.ofSeconds(48).toMillis(),
                    "Duration should be >= 48s but was: " + result);
            assertTrue(result.toMillis() <= Duration.ofSeconds(72).toMillis(),
                    "Duration should be <= 72s but was: " + result);
        }

        @RepeatedTest(20)
        @DisplayName("jitter：应ProduceVariedResults")
        void jitter_shouldProduceVariedResults() {
            Duration base = Duration.ofSeconds(60);
            double ratio = 0.2;
            Set<Long> results = new HashSet<>();

            for (int i = 0; i < 10; i++) {
                results.add(cacheProtectionUtil.jitter(base, ratio).toMillis());
            }

            // With 10 random samples there should be some variety (at least 2 distinct values)
            assertTrue(results.size() >= 2,
                    "Expected at least 2 distinct jitter values, got: " + results.size());
        }

        @Test
        @DisplayName("jitter：应不返回LessThanHalfBase")
        void jitter_shouldNotReturnLessThanHalfBase() {
            Duration base = Duration.ofMinutes(1);

            for (int i = 0; i < 100; i++) {
                Duration result = cacheProtectionUtil.jitter(base, 0.9);
                assertTrue(result.toMillis() >= base.toMillis() / 2,
                        "Duration must be at least base/2 but was: " + result);
            }
        }
    }
}
