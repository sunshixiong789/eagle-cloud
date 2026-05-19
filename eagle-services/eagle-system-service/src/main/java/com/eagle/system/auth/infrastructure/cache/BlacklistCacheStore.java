package com.eagle.system.auth.infrastructure.cache;

import com.eagle.system.auth.domain.model.enums.BlacklistType;

/**
 * 黑名单 Redis 缓存读写抽象
 *
 * @author sunshixiong
 */
public interface BlacklistCacheStore {
    void add(String tenantId, BlacklistType type, String value);
    void remove(String tenantId, BlacklistType type, String value);
    boolean isMember(String tenantId, BlacklistType type, String value);
}
