package com.eagle.redis.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/**
 * Redis 限流器，支持令牌桶和滑动窗口两种算法。
 *
 * <p>两种算法均通过 Lua 脚本保证原子性，避免并发竞争。
 *
 * @author eagle
 */
@RequiredArgsConstructor
public class RedisRateLimiter {

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    /**
     * Lua 脚本：令牌桶算法（原子）。
     *
     * <pre>
     * KEYS[1] = 桶的 Redis key
     * ARGV[1] = 容量（最大令牌数）
     * ARGV[2] = 每秒产生令牌数（QPS）
     * ARGV[3] = 当前时间戳（秒）
     * </pre>
     */
    private static final String TOKEN_BUCKET_SCRIPT = """
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local rate = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            local last_key = key .. ":last"
            
            local tokens = redis.call('GET', key)
            tokens = tokens and tonumber(tokens) or capacity
            local last = redis.call('GET', last_key)
            last = last and tonumber(last) or now
            
            local delta = math.max(0, now - last)
            tokens = math.min(capacity, tokens + delta * rate)
            
            if tokens >= 1 then
                tokens = tokens - 1
                redis.call('SET', key, tokens, 'EX', 60)
                redis.call('SET', last_key, now, 'EX', 60)
                return 1
            else
                redis.call('SET', key, tokens, 'EX', 60)
                redis.call('SET', last_key, now, 'EX', 60)
                return 0
            end
            """;
    /**
     * Lua 脚本：滑动窗口算法（原子）。
     *
     * <p>修复原版非原子问题：将 ZREMRANGEBYSCORE + ZCARD + ZADD + PEXPIRE 合并为单个 Lua 事务，
     * 避免高并发下多个请求同时通过计数检查。
     *
     * <p>成员值使用调用方传入的 UUID，避免同毫秒请求因 member 相同而覆盖，导致计数丢失。
     *
     * <pre>
     * KEYS[1] = ZSet 的 Redis key
     * ARGV[1] = 窗口内最大请求数
     * ARGV[2] = 窗口大小（毫秒）
     * ARGV[3] = 当前时间戳（毫秒）
     * ARGV[4] = 唯一成员值（UUID，防同毫秒覆盖）
     * </pre>
     */
    private static final String SLIDING_WINDOW_SCRIPT = """
            local key = KEYS[1]
            local max_count = tonumber(ARGV[1])
            local window_ms = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            local member = ARGV[4]
            
            redis.call('ZREMRANGEBYSCORE', key, 0, now - window_ms)
            local current = tonumber(redis.call('ZCARD', key))
            if current < max_count then
                redis.call('ZADD', key, now, member)
                redis.call('PEXPIRE', key, window_ms)
                return 1
            else
                return 0
            end
            """;
    private static final RedisScript<Long> TOKEN_BUCKET_REDIS_SCRIPT =
            RedisScript.of(TOKEN_BUCKET_SCRIPT, Long.class);
    private static final RedisScript<Long> SLIDING_WINDOW_REDIS_SCRIPT =
            RedisScript.of(SLIDING_WINDOW_SCRIPT, Long.class);
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 令牌桶限流：适合允许一定突发流量的场景。
     *
     * @param key      限流标识（如 "userId:api_path"）
     * @param capacity 桶容量（最大突发请求数）
     * @param rate     每秒补充令牌数（稳定 QPS）
     * @return {@code true} 允许通过，{@code false} 被限流
     */
    public boolean tryAcquire(String key, int capacity, double rate) {
        String fullKey = RATE_LIMIT_PREFIX + key;
        long nowSeconds = System.currentTimeMillis() / 1000;

        Long result = stringRedisTemplate.execute(
                TOKEN_BUCKET_REDIS_SCRIPT,
                Collections.singletonList(fullKey),
                String.valueOf(capacity),
                String.valueOf(rate),
                String.valueOf(nowSeconds));
        return result != null && result == 1L;
    }

    /**
     * 滑动窗口限流：精确控制时间窗口内的请求数量。
     *
     * <p>使用 Lua 脚本保证原子性，成员值使用 UUID 防止同毫秒请求相互覆盖。
     *
     * @param key      限流标识
     * @param maxCount 时间窗口内最大请求数
     * @param window   时间窗口大小
     * @return {@code true} 允许通过，{@code false} 被限流
     */
    public boolean tryAcquireWindow(String key, int maxCount, Duration window) {
        String fullKey = RATE_LIMIT_PREFIX + "window:" + key;
        long nowMs = System.currentTimeMillis();
        // UUID 作为 ZSet member，确保同毫秒的并发请求不会相互覆盖
        String member = UUID.randomUUID().toString();

        Long result = stringRedisTemplate.execute(
                SLIDING_WINDOW_REDIS_SCRIPT,
                Collections.singletonList(fullKey),
                String.valueOf(maxCount),
                String.valueOf(window.toMillis()),
                String.valueOf(nowMs),
                member);
        return result != null && result == 1L;
    }
}
