package com.eagle.redis.util;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redisson 原子计数工具类。
 *
 * <p>基于 {@link RAtomicLong} 实现，所有操作均为原子性，适用于：
 * <ul>
 *   <li>库存扣减（{@link #decrementIfSufficient}）</li>
 *   <li>积分/余额消费（{@link #decrementIfSufficient}）</li>
 *   <li>计数器自增/自减（{@link #increment}/{@link #decrement}）</li>
 *   <li>CAS 无锁更新（{@link #compareAndSet}）</li>
 * </ul>
 *
 * <h2>库存同步扣减完整示例</h2>
 *
 * <h3>1. 初始化库存（系统启动或商品上架时）</h3>
 * <pre>{@code
 * // 上架商品 SKU-1001，库存 100
 * atomicUtil.initIfAbsent("stock:sku:1001", 100, Duration.ofDays(7));
 * }</pre>
 *
 * <h3>2. 下单扣减库存（同步，无锁 CAS）</h3>
 * <pre>{@code
 * @Service
 * @RequiredArgsConstructor
 * public class OrderApplicationService {
 *
 *     private final RedissonAtomicUtil atomicUtil;
 *
 *     @Transactional(rollbackFor = Exception.class)
 *     public void createOrder(CreateOrderRequest request) {
 *         String stockKey = "stock:sku:" + request.getSkuId();
 *
 *         // 原子扣减：CAS 自旋，不足则返回 false，不会超扣
 *         boolean deducted = atomicUtil.decrementIfSufficient(stockKey, request.getQuantity());
 *         if (!deducted) {
 *             throw OrderErrorCode.INSUFFICIENT_STOCK.toDomainException();
 *         }
 *
 *         // 库存扣减成功，继续创建订单
 *         Order order = Order.create(request.getSkuId(), request.getQuantity());
 *         orderRepository.save(order);
 *     }
 * }
 * }</pre>
 *
 * <h3>3. 取消订单回滚库存</h3>
 * <pre>{@code
 * public void cancelOrder(Long orderId) {
 *     Order order = orderRepository.findById(orderId)
 *             .orElseThrow(OrderErrorCode.ORDER_NOT_FOUND::toNotFoundException);
 *     order.cancel();
 *     orderRepository.save(order);
 *
 *     // 回滚库存
 *     atomicUtil.addAndGet("stock:sku:" + order.getSkuId(), order.getQuantity());
 * }
 * }</pre>
 *
 * <h3>4. 查询实时库存</h3>
 * <pre>{@code
 * long stock = atomicUtil.get("stock:sku:1001");
 * if (stock <= 0) {
 *     // 标记商品售罄
 * }
 * }</pre>
 *
 * <h3>5. 并发场景说明</h3>
 * <p>{@link #decrementIfSufficient} 采用 CAS（Compare-And-Swap）自旋实现：
 * <ol>
 *   <li>读取当前库存值</li>
 *   <li>校验是否充足，不足立即返回 false</li>
 *   <li>尝试 CAS 更新（当前值 → 当前值 - 扣减量）</li>
 *   <li>若 CAS 失败（说明有并发修改），重新读取并重试</li>
 * </ol>
 * <p>此方案无需加锁，适合高并发低竞争场景（如大库存商品）。
 * 若库存极少且并发极高（抢购场景），建议配合 {@link com.eagle.common.lock.DistributedLock} 使用悲观锁避免过多自旋。
 *
 * @author 孙士雄
 */
@Component
@RequiredArgsConstructor
public class RedissonAtomicUtil {

    private final RedissonClient redissonClient;

    /**
     * 初始化计数器，若 key 不存在则设置初始值；已存在则不修改。
     *
     * @param key      计数器 key
     * @param initial  初始值
     * @param duration 过期时间，{@code null} 表示永不过期
     */
    public void initIfAbsent(String key, long initial, Duration duration) {
        RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
        if (!atomicLong.isExists()) {
            atomicLong.set(initial);
            if (duration != null) {
                atomicLong.expire(duration);
            }
        }
    }

    /**
     * 原子自增 1，返回自增后的值。
     *
     * @param key 计数器 key
     * @return 自增后的值
     */
    public long increment(String key) {
        return redissonClient.getAtomicLong(key).incrementAndGet();
    }

    /**
     * 原子加 delta，返回加后的值。
     *
     * @param key   计数器 key
     * @param delta 增量（可为负数）
     * @return 操作后的值
     */
    public long addAndGet(String key, long delta) {
        return redissonClient.getAtomicLong(key).addAndGet(delta);
    }

    /**
     * 原子自减 1，返回自减后的值。
     *
     * @param key 计数器 key
     * @return 自减后的值
     */
    public long decrement(String key) {
        return redissonClient.getAtomicLong(key).decrementAndGet();
    }

    /**
     * 原子扣减（库存/余额场景）：当前值 >= delta 时才扣减，否则返回 false。
     *
     * <p>使用 CAS + 自旋实现无锁原子扣减，避免超扣（超卖）。
     * 高并发竞争激烈时可配合 {@link com.eagle.common.lock.DistributedLock} 降低自旋次数。
     *
     * @param key   计数器 key
     * @param delta 扣减量（正数）
     * @return {@code true} 扣减成功；{@code false} 余量不足
     */
    public boolean decrementIfSufficient(String key, long delta) {
        RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
        while (true) {
            long current = atomicLong.get();
            if (current < delta) {
                return false;
            }
            if (atomicLong.compareAndSet(current, current - delta)) {
                return true;
            }
            // CAS 失败说明有并发修改，重试
        }
    }

    /**
     * 获取当前值。
     *
     * @param key 计数器 key
     * @return 当前值，key 不存在时返回 0
     */
    public long get(String key) {
        return redissonClient.getAtomicLong(key).get();
    }

    /**
     * 设置新值并返回旧值。
     *
     * @param key      计数器 key
     * @param newValue 新值
     * @return 设置前的旧值
     */
    public long getAndSet(String key, long newValue) {
        return redissonClient.getAtomicLong(key).getAndSet(newValue);
    }

    /**
     * CAS 原子更新：仅当当前值等于 expect 时，才将其更新为 update。
     *
     * @param key    计数器 key
     * @param expect 期望当前值
     * @param update 更新值
     * @return {@code true} 更新成功；{@code false} 当前值与期望不符
     */
    public boolean compareAndSet(String key, long expect, long update) {
        return redissonClient.getAtomicLong(key).compareAndSet(expect, update);
    }

    /**
     * 删除计数器。
     *
     * @param key 计数器 key
     */
    public void delete(String key) {
        redissonClient.getAtomicLong(key).delete();
    }
}
