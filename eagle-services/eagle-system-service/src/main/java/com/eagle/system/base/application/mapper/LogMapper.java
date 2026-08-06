package com.eagle.system.base.application.mapper;

import com.eagle.system.base.domain.model.SysLog;
import com.eagle.system.base.interfaces.dto.response.LogResponse;
import org.springframework.stereotype.Component;

/**
 * 系统日志映射器（纯 Java 实现）。
 */
@Component
public class LogMapper {

    public LogResponse toResponse(SysLog log) {
        if (log == null) {
            return null;
        }
        return new LogResponse(
                log.getId(),
                log.getLogType() != null ? log.getLogType().name() : null,
                log.getTitle(),
                log.getUserId(),
                log.getUsername(),
                log.getRemoteAddr(),
                log.getUserAgent(),
                log.getRequestUri(),
                log.getMethod(),
                log.getParams(),
                log.getResult(),
                log.getTime(),
                log.getException(),
                log.getServiceId(),
                log.getStatus() != null ? log.getStatus().name() : null,
                log.getCreateTime());
    }
}
