package com.eagle.auth.core.infrastructure.adapter;

import com.eagle.auth.core.domain.port.OnlineUserInfo;
import com.eagle.auth.core.domain.port.OnlineUserPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * {@link OnlineUserPort} 的 Redis 实现。
 *
 * <p>Redis Key 规范：
 * <ul>
 *   <li>{@code online:users:{jti}}        — OnlineUserInfo JSON，TTL = token 有效期</li>
 *   <li>{@code account:online:{accountId}} — Set&lt;jti&gt; 反向索引，TTL = token 有效期</li>
 *   <li>{@code token:blacklist:{jti}}     — "1"，TTL = token 剩余有效期</li>
 * </ul>
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OnlineUserAdapter implements OnlineUserPort {

    private static final String ONLINE_KEY_PREFIX = "online:users:";
    private static final String ACCOUNT_INDEX_PREFIX = "account:online:";
    private static final String BLACKLIST_KEY_PREFIX = "token:blacklist:";
    private static final long DEFAULT_TTL_SECONDS = 3600L;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void trackLogin(OnlineUserInfo info) {
        try {
            String json = objectMapper.writeValueAsString(info);
            redisTemplate.opsForValue().set(
                    ONLINE_KEY_PREFIX + info.tokenId(), json,
                    info.expiresIn(), TimeUnit.SECONDS);
            if (info.userId() != null) {
                String indexKey = ACCOUNT_INDEX_PREFIX + info.userId();
                redisTemplate.opsForSet().add(indexKey, info.tokenId());
                redisTemplate.expire(indexKey, info.expiresIn(), TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.warn("failed to track online user, redis may be unavailable: tokenId={}",
                    info.tokenId(), e);
        }
    }

    @Override
    public List<OnlineUserInfo> listOnlineUsers() {
        List<OnlineUserInfo> result = new ArrayList<>();
        try (var cursor = redisTemplate.scan(ScanOptions.scanOptions()
                .match(ONLINE_KEY_PREFIX + "*").count(100).build())) {
            cursor.forEachRemaining(key -> {
                String json = redisTemplate.opsForValue().get(key);
                if (json != null) {
                    try {
                        result.add(objectMapper.readValue(json, OnlineUserInfo.class));
                    } catch (Exception e) {
                        log.warn("Skipping malformed OnlineUserInfo for key: {}", key, e);
                    }
                }
            });
        } catch (Exception e) {
            log.warn("failed to list online users from redis, returning empty list", e);
        }
        return result;
    }

    @Override
    public List<String> listJtisByAccount(Long accountId) {
        if (accountId == null) {
            return List.of();
        }
        try {
            Set<String> jtis = redisTemplate.opsForSet().members(ACCOUNT_INDEX_PREFIX + accountId);
            return jtis == null ? List.of() : new ArrayList<>(jtis);
        } catch (Exception e) {
            log.warn("failed to list jtis by accountId={}", accountId, e);
            return List.of();
        }
    }

    @Override
    public void forceLogout(String tokenId) {
        try {
            log.info("Force logout, tokenId: {}", tokenId);
            String onlineKey = ONLINE_KEY_PREFIX + tokenId;
            Long ttl = redisTemplate.getExpire(onlineKey, TimeUnit.SECONDS);
            String json = redisTemplate.opsForValue().get(onlineKey);

            if (json != null) {
                try {
                    OnlineUserInfo info = objectMapper.readValue(json, OnlineUserInfo.class);
                    if (info.userId() != null) {
                        redisTemplate.opsForSet()
                                .remove(ACCOUNT_INDEX_PREFIX + info.userId(), tokenId);
                    }
                } catch (Exception parseEx) {
                    log.warn("force logout: malformed online info, jti={}", tokenId, parseEx);
                }
            }

            redisTemplate.delete(onlineKey);
            long blacklistTtl = (ttl != null && ttl > 0) ? ttl : DEFAULT_TTL_SECONDS;
            redisTemplate.opsForValue().set(
                    BLACKLIST_KEY_PREFIX + tokenId, "1",
                    blacklistTtl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("failed to force logout, redis may be unavailable: tokenId={}", tokenId, e);
        }
    }

    @Override
    public boolean isBlacklisted(String jti) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + jti));
        } catch (Exception e) {
            log.warn("failed to check blacklist, defaulting to not-blacklisted: jti={}", jti, e);
            return false;
        }
    }
}
