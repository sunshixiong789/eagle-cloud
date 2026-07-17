package com.eagle.auth.core.infrastructure.adapter;

import com.eagle.auth.core.domain.port.BindTicket;
import com.eagle.auth.core.domain.port.BindTicketStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

/**
 * {@link BindTicketStore} 的 Redis 实现。
 *
 * <p>Redis Key：{@code auth:bind:ticket:{ticketId}} — BindTicket JSON，TTL 10 分钟。
 * ticketId 为 32 字节 SecureRandom 的 URL-safe Base64（43 字符），消费用
 * {@code GETDEL} 保证一次性。
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisBindTicketStore implements BindTicketStore {

    private static final String KEY_PREFIX = "auth:bind:ticket:";
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public String save(BindTicket ticket) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String ticketId = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String json = objectMapper.writeValueAsString(ticket);
        redisTemplate.opsForValue().set(KEY_PREFIX + ticketId, json, TTL);
        return ticketId;
    }

    @Override
    public Optional<BindTicket> consume(String ticketId) {
        String json = redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + ticketId);
        if (json == null) {
            return Optional.empty();
        }
        return Optional.of(objectMapper.readValue(json, BindTicket.class));
    }
}
