package com.eagle.auth.core.application.command;

import com.eagle.auth.core.domain.model.enums.FreezeReason;

import java.time.LocalDateTime;

/**
 * 冻结账号命令
 *
 * @author sunshixiong
 */
public record FreezeAccountCommand(
        FreezeReason reason,
        LocalDateTime freezeUntil,
        String remark,
        Long operatorId,
        String operatorName) {
}
