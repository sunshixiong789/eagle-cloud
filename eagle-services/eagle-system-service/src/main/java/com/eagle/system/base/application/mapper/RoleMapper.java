package com.eagle.system.base.application.mapper;

import com.eagle.system.base.domain.model.Role;
import com.eagle.system.base.interfaces.dto.response.RoleResponse;
import org.springframework.stereotype.Component;

/**
 * 角色映射器（纯 Java 实现）。
 */
@Component
public class RoleMapper {

    public RoleResponse toResponse(Role role) {
        if (role == null) {
            return null;
        }
        RoleResponse response = new RoleResponse();
        response.setId(role.getId());
        response.setRoleName(role.getRoleName());
        response.setRoleCode(role.getRoleCode());
        response.setRoleDesc(role.getRoleDesc());
        response.setRoleType(role.getRoleType() != null ? role.getRoleType().name() : null);
        response.setDataScope(role.getDataScope() != null ? role.getDataScope().name() : null);
        response.setSortOrder(role.getSortOrder());
        response.setStatus(role.getStatus() != null ? role.getStatus().name() : null);
        response.setCreateTime(role.getCreateTime());
        return response;
    }
}
