package com.eagle.system.base.interfaces.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LogResponse {

    private Long id;
    private String logType;
    private String title;
    private Long userId;
    private String username;
    private String remoteAddr;
    private String userAgent;
    private String requestUri;
    private String method;
    private String params;
    private String result;
    private Long time;
    private String exception;
    private String serviceId;
    private String status;
    private LocalDateTime createTime;
}
