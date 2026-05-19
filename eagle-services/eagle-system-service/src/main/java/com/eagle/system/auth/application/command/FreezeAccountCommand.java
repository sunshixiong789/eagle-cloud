package com.eagle.system.auth.application.command;

import com.eagle.system.auth.domain.model.enums.FreezeReason;

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
