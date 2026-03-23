package com.eagle.system.base.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateRoleRequest {

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64)
    private String roleName;

    @NotBlank(message = "角色标识不能为空")
    @Size(max = 64)
    private String roleCode;

    @Size(max = 255)
    private String roleDesc;

    @NotNull(message = "排序值不能为空")
    private Integer sortOrder;
}
