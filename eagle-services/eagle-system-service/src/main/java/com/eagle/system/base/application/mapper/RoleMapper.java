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
        return new RoleResponse(
                role.getId(),
                role.getRoleName(),
                role.getRoleCode(),
                role.getRoleDesc(),
                role.getRoleType() != null ? role.getRoleType().name() : null,
                role.getDataScope() != null ? role.getDataScope().name() : null,
                role.getSortOrder(),
                role.getStatus() != null ? role.getStatus().name() : null,
                role.getCreateTime());
    }
}
