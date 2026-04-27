package com.eagle.system.base.application.mapper;

import com.eagle.system.base.domain.model.SysLog;
import com.eagle.system.base.web.dto.response.LogResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LogMapper {

    LogResponse toResponse(SysLog log);
}
