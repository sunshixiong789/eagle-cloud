package com.eagle.redis.util;

import com.eagle.redis.exception.RedisErrorCode;
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
 * <p>基于 Redisson {@link RLock} 实现，支持自定义等待/租约时间，
 * 锁在 finally 块中自动释放，不会因业务异常导致死锁。
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
     * 尝试获取锁并执行有返回值的业务逻辑，自动释放锁。
     *
     * @param lockKey   锁键（自动加 "lock:" 前缀）
     * @param waitTime  最长等待时间（秒）
     * @param leaseTime 锁最大持有时间（秒），超时后自动释放防死锁
     * @param supplier  业务逻辑
     * @param <T>       返回类型
     * @return 业务逻辑返回值
     * @throws com.eagle.common.exception.ServiceException 获取锁失败或线程被中断时抛出（HTTP 500）
     */
    public <T> T tryLock(String lockKey, long waitTime, long leaseTime, Supplier<T> supplier) {
        String fullKey = LOCK_PREFIX + lockKey;
        RLock lock = redissonClient.getLock(fullKey);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("Failed to acquire distributed lock: {}", fullKey);
                throw RedisErrorCode.LOCK_ACQUIRE_FAILED.toServiceException();
            }
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw RedisErrorCode.LOCK_INTERRUPTED.toServiceException(e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 尝试获取锁并执行无返回值的业务逻辑，自动释放锁。
     *
     * @param lockKey   锁键
     * @param waitTime  最长等待时间（秒）
     * @param leaseTime 锁最大持有时间（秒）
     * @param runnable  业务逻辑
     */
    public void tryLock(String lockKey, long waitTime, long leaseTime, Runnable runnable) {
        tryLock(lockKey, waitTime, leaseTime, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 使用默认参数获取锁（等待 3 秒，租约 30 秒），有返回值。
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
     * 使用默认参数获取锁（等待 3 秒，租约 30 秒），无返回值。
     *
     * @param lockKey  锁键
     * @param runnable 业务逻辑
     */
    public void tryLock(String lockKey, Runnable runnable) {
        tryLock(lockKey, 3, 30, runnable);
    }
}
