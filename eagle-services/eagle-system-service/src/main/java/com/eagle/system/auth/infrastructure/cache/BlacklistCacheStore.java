package com.eagle.system.auth.infrastructure.cache;

import com.eagle.system.auth.domain.model.enums.BlacklistType;

/**
 * 黑名单 Redis 缓存读写抽象
 *
 * @author sunshixiong
 */
public interface BlacklistCacheStore {
    void add(BlacklistType type, String value);
    void remove(BlacklistType type, String value);
    boolean isMember(BlacklistType type, String value);
}
