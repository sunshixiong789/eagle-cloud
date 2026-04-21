package com.eagle.system.upms.application.mapper;

import com.eagle.system.upms.domain.model.SysLog;
import com.eagle.system.upms.web.dto.response.LogResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LogMapper {

    LogResponse toResponse(SysLog log);
}
