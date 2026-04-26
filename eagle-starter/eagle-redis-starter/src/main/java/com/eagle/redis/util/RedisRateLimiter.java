package com.eagle.redis.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Redis 限流器（令牌桶算法）。
 *
 * @author 孙士雄
 */
@Component
@RequiredArgsConstructor
public class RedisRateLimiter {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    /**
     * Lua 脚本：令牌桶算法。
     * <p> KEYS[1] = 桶的 Redis Key<br>
     * ARGV[1] = 容量<br>
     * ARGV[2] = 每秒产生令牌数<br>
     * ARGV[3] = 当前时间戳（秒）
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
                redis.call('SET', key, tokens)
                redis.call('SET', last_key, now)
                redis.call('EXPIRE', key, 60)
                redis.call('EXPIRE', last_key, 60)
                return 1
            else
                redis.call('SET', key, tokens)
                redis.call('SET', last_key, now)
                redis.call('EXPIRE', key, 60)
                redis.call('EXPIRE', last_key, 60)
                return 0
            end
            """;

    private static final RedisScript<Long> RATE_LIMIT_REDIS_SCRIPT =
            RedisScript.of(TOKEN_BUCKET_SCRIPT, Long.class);

    /**
     * 尝试获取令牌。
     *
     * @param key      限流标识（如用户ID + 接口路径）
     * @param capacity 桶容量（最大突发请求数）
     * @param rate     每秒产生令牌数（QPS）
     * @return true 表示允许通过，false 表示被限流
     */
    public boolean tryAcquire(String key, int capacity, double rate) {
        String fullKey = RATE_LIMIT_PREFIX + key;
        long nowSeconds = System.currentTimeMillis() / 1000;
        List<String> keys = Collections.singletonList(fullKey);
        List<String> args = List.of(
                String.valueOf(capacity),
                String.valueOf(rate),
                String.valueOf(nowSeconds));

        Long result = stringRedisTemplate.execute(RATE_LIMIT_REDIS_SCRIPT, keys, args.toArray(new String[0]));
        return result != null && result == 1;
    }

    /**
     * 尝试获取令牌（滑动窗口算法，简单计数器）。
     *
     * @param key      限流标识
     * @param maxCount 窗口内最大请求数
     * @param window   时间窗口
     * @return true 表示允许通过
     */
    public boolean tryAcquireWindow(String key, int maxCount, Duration window) {
        String fullKey = RATE_LIMIT_PREFIX + "window:" + key;
        long now = System.currentTimeMillis();
        long windowStart = now - window.toMillis();

        stringRedisTemplate.opsForZSet().removeRangeByScore(fullKey, 0, windowStart);
        Long current = stringRedisTemplate.opsForZSet().zCard(fullKey);
        if (current != null && current >= maxCount) {
            return false;
        }

        stringRedisTemplate.opsForZSet().add(fullKey, String.valueOf(now), now);
        stringRedisTemplate.expire(fullKey, window);
        return true;
    }
}
