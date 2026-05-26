package com.eagle.auth.core.domain.event;

import com.eagle.auth.core.domain.model.enums.FreezeReason;

import java.time.LocalDateTime;

/**
 * 账号已冻结事件（auth 域内 + 跨域订阅）
 *
 * @author sunshixiong
 */
public record AccountFrozenEvent(
        Long accountId,
        String username,
        FreezeReason reason,
        LocalDateTime freezeUntil,
        Long operatorId) {
}
