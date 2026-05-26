package com.eagle.system.base.infrastructure.remote.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 账号黑名单快照(base 端独立 POJO,反序列化 auth-service
 * /internal/account-blacklist/{accountId} 响应,204 表示无记录)。
 * <p>
 * 与 auth-service 的 {@code com.eagle.auth.core.domain.port.AccountBlacklistInfo} 字段对齐。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountBlacklistSnapshot(
        Long id,
        String value
) {
}
