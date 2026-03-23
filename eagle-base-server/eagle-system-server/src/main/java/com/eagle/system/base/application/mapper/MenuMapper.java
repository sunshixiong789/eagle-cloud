package com.eagle.system.base.application.mapper;

import com.eagle.system.base.domain.model.Menu;
import com.eagle.system.base.web.dto.response.MenuResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MenuMapper {

    MenuResponse toResponse(Menu menu);
}
