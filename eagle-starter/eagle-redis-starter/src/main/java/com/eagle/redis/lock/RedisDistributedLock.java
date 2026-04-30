package com.eagle.redis.lock;

import com.eagle.common.exception.codes.LockErrorCode;
import com.eagle.common.lock.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 基于 Redisson 的 {@link DistributedLock} 实现。
 *
 * <p>是 {@code eagle.lock.type=redis}（默认）时容器中注册的 Bean，性能最优，
 * 支持 Redisson 看门狗机制自动续期。
 *
 * @author 孙士雄
 */
@Slf4j
@RequiredArgsConstructor
public class RedisDistributedLock implements DistributedLock {

    private static final String LOCK_PREFIX = "lock:";

    private final RedissonClient redissonClient;

    @Override
    public <T> T tryLock(String lockKey, long waitTime, long leaseTime, Supplier<T> supplier) {
        String fullKey = LOCK_PREFIX + lockKey;
        RLock lock = redissonClient.getLock(fullKey);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("Failed to acquire distributed lock: {}", fullKey);
                throw LockErrorCode.LOCK_ACQUIRE_FAILED.toServiceException();
            }
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw LockErrorCode.LOCK_INTERRUPTED.toServiceException(e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
