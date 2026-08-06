package com.eagle.system.base.interfaces.dto.request;

import jakarta.validation.constraints.Size;

/**
 * 更新角色请求
 * <p>
 * 字段为 null 表示「不修改」。
 */
public record UpdateRoleRequest(

        @Size(max = 64)
        String roleName,

        @Size(max = 255)
        String roleDesc,

        Integer sortOrder
) {
}
