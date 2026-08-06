package com.eagle.system.base.interfaces.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRoleRequest(

        @NotBlank(message = "角色名称不能为空")
        @Size(max = 64)
        String roleName,

        @NotBlank(message = "角色标识不能为空")
        @Size(max = 64)
        String roleCode,

        @Size(max = 255)
        String roleDesc,

        @NotNull(message = "排序值不能为空")
        Integer sortOrder
) {
}
