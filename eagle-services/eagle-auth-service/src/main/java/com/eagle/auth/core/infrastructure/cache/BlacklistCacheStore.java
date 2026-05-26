package com.eagle.auth.core.infrastructure.cache;

import com.eagle.auth.core.domain.model.enums.BlacklistType;

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
