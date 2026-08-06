package com.eagle.amqp.idempotency;

import java.time.Duration;

/**
 * 消费侧幂等去重 SPI。
 *
 * <p>RocketMQ 保证至少一次投递,消费者必须自己去重。本接口提供两套语义等价的方法,
 * 让调用方按可读性选择:
 * <ul>
 *   <li>{@link #firstTime(String)} / {@link #firstTime(String, Duration)} — "首次见到这个 eventId 吗?"
 *       返回 {@code true} = 没见过,继续处理;返回 {@code false} = 重复,跳过</li>
 *   <li>{@link #isDuplicate(String)} / {@link #isDuplicate(String, Duration)} — "是重复消息吗?"
 *       返回 {@code true} = 重复,跳过;返回 {@code false} = 没见过,继续处理</li>
 * </ul>
 *
 * <p>幂等 Key 必须使用 {@link com.eagle.common.event.BaseEvent#getEventId() BaseEvent.eventId}
 * (UUID v7,生产侧自动生成),<strong>不要</strong>用 RocketMQ 自身的 MsgId(重投递时会变)。
 *
 * <p>默认实现 {@link RedisIdempotencyChecker} 用 Redis {@code SET NX EX} 命令,TTL 由
 * {@code eagle.amqp.idempotency.default-ttl} 配置,缺省 24 小时。
 *
 * @author sunshixiong
 */
public interface IdempotencyChecker {

    /**
     * 用默认 TTL 标记 eventId。
     *
     * @return {@code true} 首次见到该 eventId(已成功占位,继续处理);
     *         {@code false} 重复(已存在占位,跳过处理)
     */
    boolean firstTime(String eventId);

    /**
     * 用自定义 TTL 标记 eventId。
     *
     * @return 同 {@link #firstTime(String)}
     */
    boolean firstTime(String eventId, Duration ttl);

    /**
     * "是否重复"语义糖,等价于 {@code !firstTime(eventId)}。
     *
     * <p>用于符合 29-event-driven.md 中的 "if (idempotencyChecker.isDuplicate(eventId)) return;" 写法。
     *
     * @return {@code true} 重复(应跳过);{@code false} 首次(应处理)
     */
    default boolean isDuplicate(String eventId) {
        return !firstTime(eventId);
    }

    /**
     * "是否重复"语义糖,等价于 {@code !firstTime(eventId, ttl)}。
     */
    default boolean isDuplicate(String eventId, Duration ttl) {
        return !firstTime(eventId, ttl);
    }
}
