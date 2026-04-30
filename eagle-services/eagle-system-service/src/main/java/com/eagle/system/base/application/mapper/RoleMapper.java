package com.eagle.system.base.application.mapper;

import com.eagle.system.base.domain.model.Role;
import com.eagle.system.base.web.dto.response.RoleResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    RoleResponse toResponse(Role role);
}
