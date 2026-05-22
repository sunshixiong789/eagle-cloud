package com.eagle.auth.infrastructure.cache;

import com.eagle.auth.domain.model.enums.BlacklistType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * BlacklistCacheStore 的 Redis Set 实现
 *
 * <p>Key 格式：{@code auth:blacklist:{TYPE}}
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
    public void add(BlacklistType type, String value) {
        try {
            redisTemplate.opsForSet().add(key(type), value);
        } catch (Exception e) {
            log.warn("blacklist cache add failed: type={}, value={}", type, value, e);
        }
    }

    @Override
    public void remove(BlacklistType type, String value) {
        try {
            redisTemplate.opsForSet().remove(key(type), value);
        } catch (Exception e) {
            log.warn("blacklist cache remove failed: type={}, value={}", type, value, e);
        }
    }

    @Override
    public boolean isMember(BlacklistType type, String value) {
        try {
            Boolean hit = redisTemplate.opsForSet().isMember(key(type), value);
            return Boolean.TRUE.equals(hit);
        } catch (Exception e) {
            log.warn("blacklist cache check failed (fallback to DB): type={}, value={}",
                    type, value, e);
            return false;
        }
    }

    private String key(BlacklistType type) {
        return KEY_PREFIX + type.name();
    }
}
