package com.eagle.common.lock;

import java.util.function.Supplier;

/**
 * 分布式锁抽象接口。
 *
 * <p>支持多种实现：基于 Redis（Redisson）、基于 MQ（RocketMQ SimpleConsumer）等。
 * 通过 {@code eagle.lock.type} 配置切换实现，业务代码无感。
 *
 * <p>统一语义：尝试在 {@code waitTime} 秒内获取名为 {@code lockKey} 的锁，
 * 获取后业务最长可持有 {@code leaseTime} 秒，超时未释放则锁自动失效以防死锁。
 *
 * @author eagle
 */
public interface DistributedLock {

    /**
     * 尝试获取锁并执行有返回值的业务逻辑，自动释放锁。
     *
     * @param lockKey   锁键
     * @param waitTime  最长等待时间（秒），为 0 表示不等待
     * @param leaseTime 锁最大持有时间（秒），超时后由实现层自动释放防死锁
     * @param supplier  业务逻辑
     * @param <T>       返回类型
     * @return 业务逻辑返回值
     * @throws com.eagle.common.exception.ServiceException 获取锁失败或线程被中断时抛出（HTTP 500）
     */
    <T> T tryLock(String lockKey, long waitTime, long leaseTime, Supplier<T> supplier);

    /**
     * 尝试获取锁并执行无返回值的业务逻辑。
     */
    default void tryLock(String lockKey, long waitTime, long leaseTime, Runnable runnable) {
        tryLock(lockKey, waitTime, leaseTime, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 使用默认参数获取锁（等待 3 秒，租约 30 秒），有返回值。
     */
    default <T> T tryLock(String lockKey, Supplier<T> supplier) {
        return tryLock(lockKey, 3L, 30L, supplier);
    }

    /**
     * 使用默认参数获取锁，无返回值。
     */
    default void tryLock(String lockKey, Runnable runnable) {
        tryLock(lockKey, 3L, 30L, runnable);
    }
}
