package com.eagle.system.upms.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户已分配角色响应 DTO
 *
 * @author sunshixiong
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignedRoleResponse {

    /** 角色 ID */
    private Long id;

    /** 角色名称 */
    private String roleName;

    /** 角色标识 */
    private String roleCode;

    /** 角色状态：ENABLE（启用）或 DISABLE（禁用）*/
    private String status;
}
