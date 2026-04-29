package com.eagle.idempotency.controller;

import com.eagle.idempotency.properties.IdempotencyProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 幂等 Token 生成接口。
 *
 * <p>客户端在执行写操作前，需先调用此接口申请一次性 Token，
 * 然后将 Token 携带在请求 Header {@code X-Idempotency-Token} 中发起业务请求。
 *
 * @author sunshixiong
 */
@Slf4j
@RestController
@RequestMapping("/idempotency")
@RequiredArgsConstructor
public class IdempotencyTokenController {

    private final RedissonClient redissonClient;
    private final IdempotencyProperties properties;

    /**
     * 生成幂等 Token。
     *
     * <p>返回一个 UUID 格式的 Token，存入 Redis 并设置 TTL（{@code eagle.idempotency.token-expire-seconds}）。
     * Token 仅可使用一次，使用后立即失效。
     *
     * @return UUID Token 字符串
     */
    @GetMapping("/token")
    public String generateToken() {
        String token = UUID.randomUUID().toString().replace("-", "");
        String redisKey = properties.getKeyPrefix() + "token:" + token;

        RBucket<String> bucket = redissonClient.getBucket(redisKey);
        bucket.set("1", properties.getTokenExpireSeconds(), TimeUnit.SECONDS);

        log.debug("Generated idempotency token: {}, ttl: {}s", token, properties.getTokenExpireSeconds());
        return token;
    }
}
