package com.eagle.system.base.infrastructure.remote.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

/**
 * 在线用户信息快照(base 端独立 POJO,反序列化 auth-service /internal/online-users 响应)。
 * <p>
 * 与 auth-service 的 {@code com.eagle.auth.core.domain.port.OnlineUserInfo} 字段对齐,
 * 双边各自维护;新增字段保持向后兼容,缺失字段忽略。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OnlineUserSnapshot(
        String tokenId,
        Long userId,
        String username,
        String ip,
        LocalDateTime loginTime,
        LocalDateTime lastActiveTime,
        String browser,
        String os,
        long expiresIn
) {
}
