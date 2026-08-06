package com.eagle.system.base.interfaces.dto.response;

/**
 * 用户已分配角色响应 DTO
 *
 * @param id       角色 ID
 * @param roleName 角色名称
 * @param roleCode 角色标识
 * @param status   角色状态：ENABLE（启用）或 DISABLE（禁用）
 * @author sunshixiong
 */
public record AssignedRoleResponse(
        Long id,
        String roleName,
        String roleCode,
        String status
) {
}
