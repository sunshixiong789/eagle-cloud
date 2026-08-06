package com.eagle.system.base.interfaces.dto.response;

import java.time.LocalDateTime;

public record LogResponse(
        Long id,
        String logType,
        String title,
        Long userId,
        String username,
        String remoteAddr,
        String userAgent,
        String requestUri,
        String method,
        String params,
        String result,
        Long time,
        String exception,
        String serviceId,
        String status,
        LocalDateTime createTime
) {
}
