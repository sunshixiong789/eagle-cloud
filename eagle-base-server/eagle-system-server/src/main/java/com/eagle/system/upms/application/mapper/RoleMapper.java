package com.eagle.system.upms.application.mapper;

import com.eagle.system.upms.domain.model.Role;
import com.eagle.system.upms.web.dto.response.RoleResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    RoleResponse toResponse(Role role);
}
