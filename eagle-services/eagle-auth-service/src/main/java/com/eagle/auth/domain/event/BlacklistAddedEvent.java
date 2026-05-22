package com.eagle.auth.domain.event;

import com.eagle.auth.domain.model.enums.BlacklistType;

import java.time.LocalDateTime;

/**
 * 黑名单新增事件
 *
 * @author sunshixiong
 */
public record BlacklistAddedEvent(
        Long id,
        BlacklistType type,
        String value,
        LocalDateTime expiresAt) {
}
