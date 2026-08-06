package com.eagle.system.base.interfaces.dto.response;

import java.time.LocalDateTime;

public record RoleResponse(
        Long id,
        String roleName,
        String roleCode,
        String roleDesc,
        String roleType,
        String dataScope,
        Integer sortOrder,
        String status,
        LocalDateTime createTime
) {
}
