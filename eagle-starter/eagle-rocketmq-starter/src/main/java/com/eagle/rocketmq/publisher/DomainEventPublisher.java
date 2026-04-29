package com.eagle.rocketmq.publisher;

import com.eagle.common.event.BaseEvent;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * 领域事件发布器接口。
 *
 * <p>将领域事件序列化并发送到 RocketMQ，支持同步、异步、延迟和顺序消息。
 *
 * @author 孙士雄
 */
public interface DomainEventPublisher {

    // -------------------------------------------------------------------------
    // 同步发送
    // -------------------------------------------------------------------------

    /**
     * 同步发布领域事件，Topic 由事件类名自动推导（{@code prefix + EventClassName}）。
     *
     * @param event 领域事件
     * @param <T>   事件类型
     */
    <T extends BaseEvent> void publish(T event);

    /**
     * 同步发布领域事件到指定 Topic。
     *
     * @param topic 目标 Topic
     * @param event 领域事件
     * @param <T>   事件类型
     */
    <T extends BaseEvent> void publish(String topic, T event);

    /**
     * 同步发布领域事件到指定 Topic，并附加 Tag（用于消费侧精细过滤）。
     *
     * @param topic 目标 Topic
     * @param tag   消息 Tag
     * @param event 领域事件
     * @param <T>   事件类型
     */
    <T extends BaseEvent> void publish(String topic, String tag, T event);

    // -------------------------------------------------------------------------
    // 异步发送
    // -------------------------------------------------------------------------

    /**
     * 异步发布领域事件，Topic 自动推导。
     *
     * <p>异步发送不阻塞调用方线程；若发布失败，Future 以异常完成。
     *
     * @param event 领域事件
     * @param <T>   事件类型
     * @return 可感知发送结果的 Future
     */
    <T extends BaseEvent> CompletableFuture<Void> publishAsync(T event);

    /**
     * 异步发布领域事件到指定 Topic。
     *
     * @param topic 目标 Topic
     * @param event 领域事件
     * @param <T>   事件类型
     * @return 可感知发送结果的 Future
     */
    <T extends BaseEvent> CompletableFuture<Void> publishAsync(String topic, T event);

    // -------------------------------------------------------------------------
    // 延迟消息
    // -------------------------------------------------------------------------

    /**
     * 发布延迟领域事件，Topic 自动推导。
     *
     * <p>消费者将在 {@code delay} 时间后收到该消息。
     *
     * @param event 领域事件
     * @param delay 延迟时长（不能为零或负值）
     * @param <T>   事件类型
     */
    <T extends BaseEvent> void publishDelayed(T event, Duration delay);

    /**
     * 发布延迟领域事件到指定 Topic。
     *
     * @param topic 目标 Topic
     * @param event 领域事件
     * @param delay 延迟时长
     * @param <T>   事件类型
     */
    <T extends BaseEvent> void publishDelayed(String topic, T event, Duration delay);

    // -------------------------------------------------------------------------
    // 顺序消息（FIFO）
    // -------------------------------------------------------------------------

    /**
     * 发布顺序消息，Topic 自动推导。
     *
     * <p>相同 {@code messageGroup} 的消息将按发送顺序被消费。
     *
     * @param event        领域事件
     * @param messageGroup 消息分组键（如聚合根 ID）
     * @param <T>          事件类型
     */
    <T extends BaseEvent> void publishOrdered(T event, String messageGroup);

    /**
     * 发布顺序消息到指定 Topic。
     *
     * @param topic        目标 Topic
     * @param event        领域事件
     * @param messageGroup 消息分组键
     * @param <T>          事件类型
     */
    <T extends BaseEvent> void publishOrdered(String topic, T event, String messageGroup);
}
