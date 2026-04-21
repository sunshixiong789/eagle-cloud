package com.eagle.system.upms.web.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateRoleRequest {

    @Size(max = 64)
    private String roleName;

    @Size(max = 255)
    private String roleDesc;

    private Integer sortOrder;
}
