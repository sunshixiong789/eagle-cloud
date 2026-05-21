package com.eagle.example.integration.cache;

import com.eagle.redis.lock.RedisDistributedLock;
import com.eagle.redis.util.CacheProtectionUtil;
import com.eagle.redis.util.RedisRateLimiter;
import com.eagle.redis.util.RedissonBloomFilterUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis Starter 功能验证服务。
 *
 * <p>验证内容：
 * <ul>
 *   <li>StringRedisTemplate 注入与基本操作</li>
 *   <li>RedisDistributedLock 分布式锁</li>
 *   <li>RedisRateLimiter 限流器</li>
 *   <li>CacheProtectionUtil 缓存防护（穿透/击穿）</li>
 *   <li>RedissonBloomFilterUtil 布隆过滤器</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisVerificationService {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisDistributedLock redisDistributedLock;
    private final RedisRateLimiter redisRateLimiter;
    private final CacheProtectionUtil cacheProtectionUtil;
    private final RedissonBloomFilterUtil bloomFilterUtil;

    public String ping() {
        stringRedisTemplate.opsForValue().set("example:ping", "pong", Duration.ofMinutes(1));
        return Optional.ofNullable(stringRedisTemplate.opsForValue().get("example:ping")).orElse("fail");
    }

    public String lockAndExecute(String lockKey) {
        return redisDistributedLock.tryLock(lockKey, 5, 30, () -> {
            log.info("Lock acquired for key: {}", lockKey);
            return "locked-success";
        });
    }

    public boolean rateLimit(String key, int capacity, double rate) {
        return redisRateLimiter.tryAcquire(key, capacity, rate);
    }

    public void initBloomFilter(String name, long expectedInsertions, double falseProbability) {
        bloomFilterUtil.init(name, expectedInsertions, falseProbability);
    }

    public boolean bloomFilterContains(String name, String element) {
        return bloomFilterUtil.contains(name, element);
    }

    public void bloomFilterAdd(String name, String element) {
        bloomFilterUtil.add(name, element);
    }
}
