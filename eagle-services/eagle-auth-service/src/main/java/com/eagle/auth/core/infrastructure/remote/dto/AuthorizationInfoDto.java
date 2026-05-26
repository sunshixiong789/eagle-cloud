package com.eagle.auth.core.infrastructure.remote.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Set;

/**
 * 授权信息远程 DTO(auth-service 端 RestClient 反序列化用)。
 * <p>
 * 与 system-service 端 {@code com.eagle.system.base.interfaces.dto.response.AuthorizationView}
 * 字段对齐;两侧各自维护,新增字段保持向后兼容。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthorizationInfoDto(
        String name,
        String avatar,
        Set<String> roleCodes
) {
}
