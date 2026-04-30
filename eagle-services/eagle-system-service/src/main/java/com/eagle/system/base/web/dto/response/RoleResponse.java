package com.eagle.system.base.web.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoleResponse {

    private Long id;
    private String roleName;
    private String roleCode;
    private String roleDesc;
    private String roleType;
    private String dataScope;
    private Integer sortOrder;
    private String status;
    private LocalDateTime createTime;
}
