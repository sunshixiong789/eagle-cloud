package com.eagle.system.system.application.mapper;

import com.eagle.system.domain.model.Role;
import com.eagle.system.web.dto.response.RoleResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    RoleResponse toResponse(Role role);
}
