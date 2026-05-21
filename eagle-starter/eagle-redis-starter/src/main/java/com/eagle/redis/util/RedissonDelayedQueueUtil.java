package com.eagle.redis.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * Redisson 延迟队列工具类。
 *
 * <p>基于 {@link RDelayedQueue} 实现，适用于需要延迟触发的场景：
 * <ul>
 *   <li>订单超时未支付自动取消（30 分钟后触发）</li>
 *   <li>优惠券到期提醒</li>
 *   <li>延迟消息通知</li>
 * </ul>
 *
 * <p><b>生产者</b>调用 {@link #offer} 投递延迟任务；
 * <b>消费者</b>通过 {@link #take}（阻塞）或 {@link #poll}（非阻塞）获取到期任务。
 *
 * <p>消费者推荐在 {@code @Async} 方法或独立线程中循环调用 {@link #take} 处理任务，
 * 服务重启后延迟任务仍保留在 Redis 中不会丢失。
 *
 * @author 孙士雄
 */
@Slf4j
@RequiredArgsConstructor
public class RedissonDelayedQueueUtil {

    private final RedissonClient redissonClient;

    /**
     * 投递延迟任务。
     *
     * <pre>
     * // 订单超时取消示例（30 分钟后触发）
     * delayedQueue.offer("order:timeout", orderId, 30, TimeUnit.MINUTES);
     * </pre>
     *
     * @param queueName 队列名称
     * @param item      任务内容
     * @param delay     延迟时长
     * @param timeUnit  时间单位
     * @param <T>       任务类型
     */
    @SuppressWarnings("deprecation")
    public <T> void offer(String queueName, T item, long delay, TimeUnit timeUnit) {
        RBlockingQueue<T> blockingQueue = redissonClient.getBlockingQueue(queueName);
        RDelayedQueue<T> delayedQueue = redissonClient.getDelayedQueue(blockingQueue);
        try {
            delayedQueue.offer(item, delay, timeUnit);
            log.debug("Delayed task offered to [{}]: item={}, delay={}{}",
                    queueName, item, delay, timeUnit);
        } finally {
            // RDelayedQueue 不需要手动关闭，但需要调用 destroy 释放内部资源
            delayedQueue.destroy();
        }
    }

    /**
     * 阻塞获取到期任务（消费者端使用）。
     *
     * <p>当队列为空时阻塞等待，直到有任务到期。
     * 推荐在 {@code @Async} 方法中循环调用：
     *
     * <pre>
     * {@code
     * @Async
     * public void startOrderTimeoutConsumer() {
     *     while (!Thread.currentThread().isInterrupted()) {
     *         Long orderId = delayedQueue.take("order:timeout", Long.class);
     *         if (orderId != null) {
     *             orderService.cancelIfUnpaid(orderId);
     *         }
     *     }
     * }
     * }
     * </pre>
     *
     * @param queueName 队列名称
     * @param <T>       任务类型
     * @return 到期的任务，线程被中断时返回 {@code null}
     */
    public <T> T take(String queueName) {
        RBlockingQueue<T> blockingQueue = redissonClient.getBlockingQueue(queueName);
        try {
            return blockingQueue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("DelayedQueue [{}] consumer interrupted", queueName);
            return null;
        }
    }

    /**
     * 非阻塞获取到期任务，无任务时立即返回 {@code null}。
     *
     * @param queueName 队列名称
     * @param <T>       任务类型
     * @return 到期的任务，无任务时返回 {@code null}
     */
    public <T> T poll(String queueName) {
        RBlockingQueue<T> blockingQueue = redissonClient.getBlockingQueue(queueName);
        return blockingQueue.poll();
    }

    /**
     * 带超时的阻塞获取，超时后返回 {@code null}。
     *
     * @param queueName 队列名称
     * @param timeout   超时时长
     * @param timeUnit  时间单位
     * @param <T>       任务类型
     * @return 到期的任务，超时或中断时返回 {@code null}
     */
    public <T> T poll(String queueName, long timeout, TimeUnit timeUnit) {
        RBlockingQueue<T> blockingQueue = redissonClient.getBlockingQueue(queueName);
        try {
            return blockingQueue.poll(timeout, timeUnit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("DelayedQueue [{}] poll interrupted", queueName);
            return null;
        }
    }

    /**
     * 查询队列中待到期的任务数量。
     *
     * @param queueName 队列名称
     * @return 待处理任务数
     */
    public int size(String queueName) {
        return redissonClient.getBlockingQueue(queueName).size();
    }

    /**
     * 取消尚未到期的任务。
     *
     * @param queueName 队列名称
     * @param item      要取消的任务内容
     * @param <T>       任务类型
     * @return {@code true} 取消成功；{@code false} 任务不存在或已到期
     */
    @SuppressWarnings("deprecation")
    public <T> boolean cancel(String queueName, T item) {
        RBlockingQueue<T> blockingQueue = redissonClient.getBlockingQueue(queueName);
        RDelayedQueue<T> delayedQueue = redissonClient.getDelayedQueue(blockingQueue);
        try {
            return delayedQueue.remove(item);
        } finally {
            delayedQueue.destroy();
        }
    }
}
