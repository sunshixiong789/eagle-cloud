package com.eagle.system.auth.domain.event;

import com.eagle.system.auth.domain.model.enums.BlacklistType;

/**
 * 黑名单删除事件
 *
 * @author sunshixiong
 */
public record BlacklistRemovedEvent(
        Long id,
        BlacklistType type,
        String value) {
}
