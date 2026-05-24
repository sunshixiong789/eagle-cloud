package com.eagle.rocketmq.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * 基于 Redis {@code SET key value NX EX ttl} 的 {@link IdempotencyChecker} 默认实现。
 *
 * <p>原子操作: {@code setIfAbsent} 在 Redis 单线程模型下天然保证"只有一个进程能写入成功",
 * 之后所有副本消费者都会拿到 {@code Boolean.FALSE} 跳过处理。
 *
 * <p>故障容忍: Redis 不可用时 {@code setIfAbsent} 会抛 {@code DataAccessException},
 * 本实现统一捕获并降级为"按首次处理"(保守策略:宁可重复消费也不丢消息) —— 同时打 WARN
 * 日志,运维侧应配 alarm 监听 Redis 故障。
 *
 * @author sunshixiong
 */
@Slf4j
@RequiredArgsConstructor
public class RedisIdempotencyChecker implements IdempotencyChecker {

    private final StringRedisTemplate redisTemplate;
    private final IdempotencyProperties properties;

    @Override
    public boolean firstTime(String eventId) {
        return firstTime(eventId, properties.getDefaultTtl());
    }

    @Override
    public boolean firstTime(String eventId, Duration ttl) {
        if (eventId == null || eventId.isEmpty()) {
            // 缺 eventId 说明生产侧未走 BaseEvent,无法去重;允许通过但打 WARN
            log.warn("idempotency check skipped: eventId is null/empty");
            return true;
        }
        String key = properties.getKeyPrefix() + eventId;
        try {
            Boolean first = redisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
            // setIfAbsent 在 Redis 5.x+ 返回 Boolean,理论上不为 null,但防御性处理
            return !Boolean.FALSE.equals(first);
        } catch (RuntimeException ex) {
            // Redis 故障 → 按首次处理(可能重复消费,业务侧二次幂等兜底)
            log.warn("idempotency check failed, fallback to first-time: eventId={}", eventId, ex);
            return true;
        }
    }
}
