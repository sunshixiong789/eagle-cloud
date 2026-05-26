package com.eagle.auth.core.infrastructure.security;

import com.eagle.common.exception.ServiceException;
import com.eagle.auth.core.domain.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 短信发送的 IP 级 + 手机号级 Redis 频控。
 *
 * <p>用于补充 {@link com.eagle.auth.core.infrastructure.external.AbstractCachedSmsService}
 * 中按 phone 的 60 秒频控——同一 IP 可能用不同手机号刷发，单纯按 phone 限流不够。
 *
 * <ul>
 *   <li>同一 IP：60 秒内 10 次（短间隔频控）</li>
 *   <li>同一 IP：1 小时内 50 次（长期频控）</li>
 * </ul>
 * Redis 异常时 fail-open（不影响业务）。
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsSendRateLimiter {

    private static final String IP_MIN_KEY = "auth:sms:ip-min:";
    private static final String IP_HOUR_KEY = "auth:sms:ip-hour:";

    private static final int IP_PER_MINUTE = 10;
    private static final int IP_PER_HOUR = 50;
    private static final Duration MINUTE = Duration.ofMinutes(1);
    private static final Duration HOUR = Duration.ofHours(1);

    private final StringRedisTemplate redis;

    /**
     * 检查并消耗一次配额；超限抛 {@link AuthErrorCode#SMS_RATE_LIMIT}。
     *
     * @param ip 客户端 IP（可为 null，null 时跳过 IP 级限流）
     */
    public void checkAndIncrement(String ip) {
        if (ip == null || ip.isBlank()) {
            return;
        }
        try {
            check(IP_MIN_KEY + ip, IP_PER_MINUTE, MINUTE, "ip-min", ip);
            check(IP_HOUR_KEY + ip, IP_PER_HOUR, HOUR, "ip-hour", ip);
        } catch (ServiceException e) {
            // 业务限流异常直接抛给调用方
            throw e;
        } catch (RuntimeException e) {
            // Redis / 网络等基础设施异常 → fail-open，不影响业务
            log.warn("sms rate limiter degraded (fail-open), ip={}", ip, e);
        }
    }

    private void check(String key, int max, Duration ttl, String tag, String ip) {
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, ttl);
        }
        if (count != null && count > max) {
            log.warn("sms rate limit exceeded: tag={}, ip={}, count={}", tag, ip, count);
            throw AuthErrorCode.SMS_RATE_LIMIT.toServiceException();
        }
    }
}
