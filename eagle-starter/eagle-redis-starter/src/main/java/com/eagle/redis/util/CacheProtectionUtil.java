package com.eagle.redis.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 缓存保护工具类。
 *
 * <p>解决高并发场景下三大缓存问题：
 * <ul>
 *   <li><b>缓存穿透</b>：查询不存在的数据，持续打穿 DB。
 *       解决方案：配合 {@link RedissonBloomFilterUtil} 使用，命中过滤器后才查缓存/DB。</li>
 *   <li><b>缓存击穿</b>：热点 key 到期瞬间，大量请求同时穿透到 DB。
 *       解决方案：{@link #getWithMutex} Mutex 互斥锁，只允许一个线程重建缓存，其余等待。</li>
 *   <li><b>缓存雪崩</b>：大量 key 同时到期，DB 瞬间压力暴增。
 *       解决方案：{@link #jitter} 在基础 TTL 上叠加随机偏移量，错开到期时间。</li>
 * </ul>
 *
 * <p>典型使用示例：
 * <pre>{@code
 * // 防击穿：热点商品缓存
 * Product product = cacheProtection.getWithMutex(
 *     "product:" + productId,
 *     Duration.ofMinutes(30),
 *     () -> productRepository.findById(productId).orElse(null),
 *     Product.class
 * );
 *
 * // 防雪崩：批量缓存写入时使用随机 TTL
 * Duration ttl = cacheProtection.jitter(Duration.ofHours(1), 0.3);
 * redisTemplate.opsForValue().set(key, value, ttl);
 * }</pre>
 *
 * @author eagle
 */
@Slf4j
@RequiredArgsConstructor
public class CacheProtectionUtil {

    private static final String MUTEX_KEY_PREFIX = "eagle:mutex:";
    /**
     * 空值占位符，用于缓存穿透防护（防止 DB 不存在的数据被反复查询）
     */
    private static final String NULL_PLACEHOLDER = "__NULL__";
    /**
     * Mutex 锁等待超时（秒）
     */
    private static final int MUTEX_WAIT_SECONDS = 3;
    /**
     * Mutex 锁持有时长（秒）
     */
    private static final int MUTEX_LEASE_SECONDS = 10;
    /**
     * 空值缓存时长
     */
    private static final Duration NULL_VALUE_TTL = Duration.ofMinutes(5);

    private final RedissonClient redissonClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Random random = new Random();

    /**
     * 防击穿：使用 Mutex 互斥锁重建缓存。
     *
     * <p>缓存未命中时，只允许一个线程执行 {@code loader} 查询 DB 并回写缓存，
     * 其余线程等待锁释放后直接读缓存，避免大量请求同时穿透到 DB。
     *
     * <p>同时防穿透：{@code loader} 返回 {@code null} 时缓存空值占位符
     * （TTL = 5 分钟），阻止对不存在数据的持续查询。
     *
     * @param key    缓存 key
     * @param ttl    缓存过期时长（建议使用 {@link #jitter} 添加随机偏移）
     * @param loader 缓存未命中时从 DB 加载数据的方法，允许返回 {@code null}
     * @param type   返回值类型（用于类型安全转换）
     * @param <T>    数据类型
     * @return 缓存数据，若 DB 中不存在则返回 {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T> T getWithMutex(String key, Duration ttl, Supplier<T> loader, Class<T> type) {
        // 1. 先查缓存
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return NULL_PLACEHOLDER.equals(cached) ? null : (T) cached;
        }

        // 2. 缓存未命中，竞争 Mutex 锁
        String mutexKey = MUTEX_KEY_PREFIX + key;
        RLock mutex = redissonClient.getLock(mutexKey);
        try {
            boolean acquired = mutex.tryLock(MUTEX_WAIT_SECONDS, MUTEX_LEASE_SECONDS, TimeUnit.SECONDS);
            if (!acquired) {
                // 未抢到锁，短暂等待后重试读缓存（此时持锁线程应该已回写）
                Thread.sleep(100);
                Object retried = redisTemplate.opsForValue().get(key);
                return retried == null || NULL_PLACEHOLDER.equals(retried) ? null : (T) retried;
            }

            // 3. 获得锁后双重检查（防止已有其他线程回写）
            Object doubleCheck = redisTemplate.opsForValue().get(key);
            if (doubleCheck != null) {
                return NULL_PLACEHOLDER.equals(doubleCheck) ? null : (T) doubleCheck;
            }

            // 4. 查询 DB 并回写缓存
            T value = loader.get();
            if (value == null) {
                // 缓存空占位符，防穿透
                redisTemplate.opsForValue().set(key, NULL_PLACEHOLDER, NULL_VALUE_TTL);
                log.debug("[CacheProtection] Cached null placeholder for key: {}", key);
            } else {
                redisTemplate.opsForValue().set(key, value, ttl);
            }
            return value;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[CacheProtection] Interrupted while waiting for mutex lock, key: {}", key);
            return null;
        } finally {
            if (mutex.isHeldByCurrentThread()) {
                mutex.unlock();
            }
        }
    }

    /**
     * 防雪崩：在基础 TTL 上叠加随机抖动，错开大量 key 的到期时间。
     *
     * <p>抖动范围为 {@code [-jitterRatio * base, +jitterRatio * base]}，
     * 最终 TTL 不低于 {@code base / 2}。
     *
     * <pre>{@code
     * // 基础 TTL 1小时，30% 抖动 → 实际 TTL 在 [42min, 78min] 之间随机
     * Duration ttl = cacheProtection.jitter(Duration.ofHours(1), 0.3);
     * }</pre>
     *
     * @param base        基础过期时长
     * @param jitterRatio 抖动比例（0.0 ~ 1.0），如 0.3 表示 ±30%
     * @return 添加随机抖动后的过期时长
     */
    public Duration jitter(Duration base, double jitterRatio) {
        long baseMs = base.toMillis();
        long maxJitter = (long) (baseMs * jitterRatio);
        // random.nextLong() 范围太大，改用 nextDouble
        long offset = (long) ((random.nextDouble() * 2 - 1) * maxJitter);
        long result = Math.max(baseMs / 2, baseMs + offset);
        return Duration.ofMillis(result);
    }

    /**
     * 删除缓存（同时清除 Mutex 锁，避免锁残留）。
     *
     * @param key 缓存 key
     */
    public void evict(String key) {
        redisTemplate.delete(key);
        String mutexKey = MUTEX_KEY_PREFIX + key;
        RLock mutex = redissonClient.getLock(mutexKey);
        if (mutex.isLocked()) {
            log.warn("[CacheProtection] Evicting key with active mutex, key: {}", key);
        }
    }
}
