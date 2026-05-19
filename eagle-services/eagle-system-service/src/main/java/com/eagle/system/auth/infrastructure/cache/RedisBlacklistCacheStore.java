package com.eagle.system.auth.infrastructure.cache;

import com.eagle.system.auth.domain.model.enums.BlacklistType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * BlacklistCacheStore 的 Redis Set 实现
 *
 * <p>Key 格式：{@code auth:blacklist:{tenantId}:{TYPE}}
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisBlacklistCacheStore implements BlacklistCacheStore {

    private static final String KEY_PREFIX = "auth:blacklist:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void add(String tenantId, BlacklistType type, String value) {
        try {
            redisTemplate.opsForSet().add(key(tenantId, type), value);
        } catch (Exception e) {
            log.warn("blacklist cache add failed: tenant={}, type={}, value={}",
                    tenantId, type, value, e);
        }
    }

    @Override
    public void remove(String tenantId, BlacklistType type, String value) {
        try {
            redisTemplate.opsForSet().remove(key(tenantId, type), value);
        } catch (Exception e) {
            log.warn("blacklist cache remove failed: tenant={}, type={}, value={}",
                    tenantId, type, value, e);
        }
    }

    @Override
    public boolean isMember(String tenantId, BlacklistType type, String value) {
        try {
            Boolean hit = redisTemplate.opsForSet().isMember(key(tenantId, type), value);
            return Boolean.TRUE.equals(hit);
        } catch (Exception e) {
            log.warn("blacklist cache check failed (fallback to DB): tenant={}, type={}, value={}",
                    tenantId, type, value, e);
            return false;
        }
    }

    private String key(String tenantId, BlacklistType type) {
        return KEY_PREFIX + (tenantId != null ? tenantId : "0") + ":" + type.name();
    }
}
