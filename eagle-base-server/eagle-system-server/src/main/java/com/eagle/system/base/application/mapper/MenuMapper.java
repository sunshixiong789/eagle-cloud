package com.eagle.system.base.application.mapper;

import com.eagle.system.base.domain.model.Menu;
import com.eagle.system.base.web.dto.response.MenuResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MenuMapper {

    MenuResponse toResponse(Menu menu);
}
