package com.eagle.redis.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redisson 分布式限流工具类。
 *
 * <p>基于 {@link RRateLimiter} 实现，底层使用令牌桶算法，相比 Lua 脚本实现更简洁可靠。
 * 支持两种限流模式：
 * <ul>
 *   <li>{@link RateType#OVERALL} — 全局限流，所有节点共享同一速率上限</li>
 *   <li>{@link RateType#PER_CLIENT} — 单节点限流，每个实例独立计算</li>
 * </ul>
 *
 * <p>与 {@link RedisRateLimiter}（Lua 脚本实现）的区别：
 * 本类更适合需要精确控制速率且长期运行的场景；Lua 实现更轻量，适合无状态的短期限流。
 *
 * @author 孙士雄
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedissonRateLimiterUtil {

    private final RedissonClient redissonClient;

    /**
     * 尝试获取 1 个令牌（全局限流）。
     *
     * <p>首次调用时自动初始化限流器；已存在则直接使用，不重置速率。
     * 若需修改速率，请先调用 {@link #delete} 删除旧限流器再重新创建。
     *
     * <pre>
     * // 示例：接口每秒最多 10 次请求（全局）
     * boolean allowed = rateLimiterUtil.tryAcquire("api:createOrder", 10, Duration.ofSeconds(1));
     * </pre>
     *
     * @param key          限流器 key
     * @param rate         时间间隔内允许的最大请求数
     * @param rateInterval 时间间隔
     * @return {@code true} 获取令牌成功；{@code false} 已达速率上限
     */
    public boolean tryAcquire(String key, long rate, Duration rateInterval) {
        return tryAcquire(key, rate, rateInterval, RateType.OVERALL, 1);
    }

    /**
     * 尝试获取指定数量的令牌（全局限流）。
     *
     * @param key          限流器 key
     * @param rate         时间间隔内允许的最大请求数
     * @param rateInterval 时间间隔
     * @param permits      本次申请的令牌数
     * @return {@code true} 获取令牌成功；{@code false} 已达速率上限
     */
    public boolean tryAcquire(String key, long rate, Duration rateInterval, long permits) {
        return tryAcquire(key, rate, rateInterval, RateType.OVERALL, permits);
    }

    /**
     * 完整参数版本：支持指定限流类型和申请令牌数。
     *
     * @param key          限流器 key
     * @param rate         时间间隔内允许的最大请求数
     * @param rateInterval 时间间隔
     * @param rateType     限流类型（OVERALL 全局 / PER_CLIENT 单节点）
     * @param permits      本次申请的令牌数
     * @return {@code true} 获取令牌成功；{@code false} 已达速率上限
     */
    public boolean tryAcquire(String key, long rate, Duration rateInterval,
                              RateType rateType, long permits) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        // trySetRate 仅在 key 不存在时初始化，已存在的限流器不会被重置
        rateLimiter.trySetRate(rateType, rate, rateInterval);
        return rateLimiter.tryAcquire(permits);
    }

    /**
     * 删除限流器（用于重置速率配置）。
     *
     * @param key 限流器 key
     */
    public void delete(String key) {
        redissonClient.getRateLimiter(key).delete();
    }

    /**
     * 查询限流器可用令牌数。
     *
     * @param key 限流器 key
     * @return 当前可用令牌数，key 不存在时返回 -1
     */
    public long availablePermits(String key) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        if (!rateLimiter.isExists()) {
            return -1;
        }
        return rateLimiter.availablePermits();
    }
}
