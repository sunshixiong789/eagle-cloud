package com.eagle.auth.infrastructure.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 登录频率限制服务（Redis 分布式实现）。
 *
 * <p>基于 IP 追踪登录失败次数，超过阈值后才开始 30 分钟封锁倒计时，防止暴力破解。
 * 多实例部署共享同一计数空间；Redis 故障时降级为放行（fail-open），不影响正常登录。
 *
 * <p>关键语义：
 * <ul>
 *   <li>计数 key：{@code auth:login-fail:{ip}}，每次失败 INCR + 设置过期时间</li>
 *   <li>封锁 key：{@code auth:login-block:{ip}}，达到阈值时写入，TTL 30 分钟</li>
 *   <li>登录成功：同时删除计数 key 与封锁 key</li>
 * </ul>
 *
 * @author sunshixiong
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    /**
     * 同一 IP 在统计窗口内允许的最大失败次数
     */
    private static final int MAX_ATTEMPTS = 5;

    /**
     * 失败计数 key 的统计窗口
     */
    private static final Duration ATTEMPT_WINDOW = Duration.ofMinutes(10);

    /**
     * 达到阈值后封锁持续时长
     */
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(30);

    private static final String ATTEMPT_KEY_PREFIX = "auth:login-fail:";
    private static final String BLOCK_KEY_PREFIX = "auth:login-block:";

    private final StringRedisTemplate redisTemplate;

    /**
     * 记录一次登录失败。
     *
     * <p>失败计数滚动窗口 10 分钟；达到 {@link #MAX_ATTEMPTS} 后写入封锁标记，
     * 触发后续 {@link #isBlocked(String)} 命中。
     *
     * @param ip 客户端 IP
     */
    public void registerFailure(String ip) {
        if (ip == null || ip.isBlank()) {
            return;
        }
        try {
            String attemptKey = ATTEMPT_KEY_PREFIX + ip;
            Long count = redisTemplate.opsForValue().increment(attemptKey);
            if (count != null && count == 1L) {
                redisTemplate.expire(attemptKey, ATTEMPT_WINDOW);
            }
            if (count != null && count >= MAX_ATTEMPTS) {
                redisTemplate.opsForValue().set(BLOCK_KEY_PREFIX + ip, "1", BLOCK_DURATION);
            }
        } catch (Exception e) {
            log.warn("login attempt counter failed (fail-open), ip={}", ip, e);
        }
    }

    /**
     * 登录成功，重置失败计数和封锁状态。
     *
     * @param ip 客户端 IP
     */
    public void registerSuccess(String ip) {
        if (ip == null || ip.isBlank()) {
            return;
        }
        try {
            redisTemplate.delete(ATTEMPT_KEY_PREFIX + ip);
            redisTemplate.delete(BLOCK_KEY_PREFIX + ip);
        } catch (Exception e) {
            log.warn("login attempt reset failed, ip={}", ip, e);
        }
    }

    /**
     * 判断该 IP 是否已被封锁。Redis 异常时返回 false（fail-open）。
     *
     * @param ip 客户端 IP
     * @return true 表示已超过失败阈值，应拒绝请求
     */
    public boolean isBlocked(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(BLOCK_KEY_PREFIX + ip));
        } catch (Exception e) {
            log.warn("login attempt check failed (fail-open), ip={}", ip, e);
            return false;
        }
    }
}
