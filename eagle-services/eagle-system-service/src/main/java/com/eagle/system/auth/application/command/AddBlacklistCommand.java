package com.eagle.system.auth.application.command;

import com.eagle.system.auth.domain.model.enums.BlacklistType;

import java.time.LocalDateTime;

/**
 * 添加黑名单命令
 *
 * @author sunshixiong
 */
public record AddBlacklistCommand(
        BlacklistType type,
        String value,
        String reason,
        LocalDateTime expiresAt,
        Long operatorId,
        String operatorName) {
}
