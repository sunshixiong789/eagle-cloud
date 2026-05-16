package com.eagle.system.auth.infrastructure.adapter;

import com.alibaba.fastjson2.JSON;
import com.eagle.system.auth.domain.port.OnlineUserInfo;
import com.eagle.system.auth.domain.port.OnlineUserPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * {@link OnlineUserPort} 的 Redis 实现。
 *
 * <p>Redis Key 规范：
 * <ul>
 *   <li>{@code online:users:{jti}}    — OnlineUserInfo JSON，TTL = token 有效期（秒）</li>
 *   <li>{@code token:blacklist:{jti}} — "1"，TTL = token 剩余有效期（秒）</li>
 * </ul>
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OnlineUserAdapter implements OnlineUserPort {

    private static final String ONLINE_KEY_PREFIX = "online:users:";
    private static final String BLACKLIST_KEY_PREFIX = "token:blacklist:";
    private static final long DEFAULT_TTL_SECONDS = 3600L;

    private final StringRedisTemplate redisTemplate;

    @Override
    public void trackLogin(OnlineUserInfo info) {
        try {
            String json = JSON.toJSONString(info);
            redisTemplate.opsForValue().set(
                    ONLINE_KEY_PREFIX + info.tokenId(), json,
                    info.expiresIn(), TimeUnit.SECONDS);
        } catch (Exception e) {
            // Redis 不可用时降级，不阻断登录流程；但必须保留堆栈以便运维定位
            log.warn("failed to track online user, redis may be unavailable: tokenId={}", info.tokenId(), e);
        }
    }

    @Override
    public List<OnlineUserInfo> listOnlineUsers() {
        List<OnlineUserInfo> result = new ArrayList<>();
        try (var cursor = redisTemplate.scan(
                org.springframework.data.redis.core.ScanOptions.scanOptions()
                        .match(ONLINE_KEY_PREFIX + "*")
                        .count(100)
                        .build())) {
            cursor.forEachRemaining(key -> {
                String json = redisTemplate.opsForValue().get(key);
                if (json != null) {
                    try {
                        result.add(JSON.parseObject(json, OnlineUserInfo.class));
                    } catch (Exception e) {
                        log.warn("Skipping malformed OnlineUserInfo for key: {}", key, e);
                    }
                }
            });
        } catch (Exception e) {
            // Redis 不可用时返回空列表；运维需要堆栈定位
            log.warn("failed to list online users from redis, returning empty list", e);
        }
        return result;
    }

    @Override
    public void forceLogout(String tokenId) {
        try {
            log.info("Force logout, tokenId: {}", tokenId);
            String onlineKey = ONLINE_KEY_PREFIX + tokenId;
            Long ttl = redisTemplate.getExpire(onlineKey, TimeUnit.SECONDS);
            redisTemplate.delete(onlineKey);
            long blacklistTtl = (ttl != null && ttl > 0) ? ttl : DEFAULT_TTL_SECONDS;
            redisTemplate.opsForValue().set(
                    BLACKLIST_KEY_PREFIX + tokenId, "1",
                    blacklistTtl, TimeUnit.SECONDS);
        } catch (Exception e) {
            // Redis 不可用时降级处理；保留堆栈
            log.warn("failed to force logout, redis may be unavailable: tokenId={}", tokenId, e);
        }
    }

    @Override
    public boolean isBlacklisted(String jti) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + jti));
        } catch (Exception e) {
            // Redis 不可用时默认认为未拉黑（允许访问）；保留堆栈以排查 Redis 故障
            log.warn("failed to check blacklist, defaulting to not-blacklisted: jti={}", jti, e);
            return false;
        }
    }
}
