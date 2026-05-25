package com.eagle.system.base.infrastructure.remote.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Account 快照(base 端独立 POJO,反序列化 auth-service /internal/accounts/by-username 响应)。
 * <p>
 * 与 auth-service 的 {@code AccountInternalController.AccountSnapshot} 字段对齐,
 * 双边各自维护;新增字段保持向后兼容,缺失字段忽略。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountSnapshot(
        Long accountId,
        String username,
        String phone
) {
}
