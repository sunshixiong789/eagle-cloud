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
        LogResponse response = new LogResponse();
        response.setId(log.getId());
        response.setLogType(log.getLogType() != null ? log.getLogType().name() : null);
        response.setTitle(log.getTitle());
        response.setUserId(log.getUserId());
        response.setUsername(log.getUsername());
        response.setRemoteAddr(log.getRemoteAddr());
        response.setUserAgent(log.getUserAgent());
        response.setRequestUri(log.getRequestUri());
        response.setMethod(log.getMethod());
        response.setParams(log.getParams());
        response.setResult(log.getResult());
        response.setTime(log.getTime());
        response.setException(log.getException());
        response.setServiceId(log.getServiceId());
        response.setStatus(log.getStatus() != null ? log.getStatus().name() : null);
        response.setCreateTime(log.getCreateTime());
        return response;
    }
}
