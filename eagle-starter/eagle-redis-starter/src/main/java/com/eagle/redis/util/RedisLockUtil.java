package com.eagle.redis.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redis 分布式锁工具类。
 *
 * @author 孙士雄
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLockUtil {

    private final RedissonClient redissonClient;

    private static final String LOCK_PREFIX = "lock:";

    /**
     * 尝试获取锁并执行业务逻辑，自动释放锁。
     *
     * @param lockKey  锁键
     * @param waitTime 等待时间（秒）
     * @param leaseTime 租约时间（秒），超过后自动释放
     * @param supplier 业务逻辑
     * @param <T>      返回类型
     * @return 业务逻辑返回值
     * @throws RuntimeException 获取锁失败时抛出
     */
    public <T> T tryLock(String lockKey, long waitTime, long leaseTime, Supplier<T> supplier) {
        String fullKey = LOCK_PREFIX + lockKey;
        RLock lock = redissonClient.getLock(fullKey);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (!acquired) {
                throw new RuntimeException("获取分布式锁失败: " + fullKey);
            }
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取分布式锁被中断: " + fullKey, e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 尝试获取锁并执行业务逻辑（无返回值），自动释放锁。
     *
     * @param lockKey   锁键
     * @param waitTime  等待时间（秒）
     * @param leaseTime 租约时间（秒）
     * @param runnable  业务逻辑
     */
    public void tryLock(String lockKey, long waitTime, long leaseTime, Runnable runnable) {
        tryLock(lockKey, waitTime, leaseTime, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 使用默认参数获取锁：等待 3 秒，租约 30 秒。
     *
     * @param lockKey  锁键
     * @param supplier 业务逻辑
     * @param <T>      返回类型
     * @return 业务逻辑返回值
     */
    public <T> T tryLock(String lockKey, Supplier<T> supplier) {
        return tryLock(lockKey, 3, 30, supplier);
    }

    /**
     * 使用默认参数获取锁（无返回值）。
     *
     * @param lockKey  锁键
     * @param runnable 业务逻辑
     */
    public void tryLock(String lockKey, Runnable runnable) {
        tryLock(lockKey, 3, 30, runnable);
    }
}
