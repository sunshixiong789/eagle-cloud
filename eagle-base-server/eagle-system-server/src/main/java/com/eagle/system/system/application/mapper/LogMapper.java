package com.eagle.system.system.application.mapper;

import com.eagle.system.domain.model.SysLog;
import com.eagle.system.web.dto.response.LogResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LogMapper {

    LogResponse toResponse(SysLog log);
}
