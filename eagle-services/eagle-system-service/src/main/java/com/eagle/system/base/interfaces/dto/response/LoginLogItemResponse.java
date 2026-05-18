package com.eagle.system.base.interfaces.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 登录日志列表项响应（前端友好字段）
 */
@Data
@Builder
public class LoginLogItemResponse {

    private Long id;
    private Long userId;
    private String username;

    /** 登录 IP（来自 remoteAddr） */
    private String ip;

    /** 浏览器（解析自 userAgent） */
    private String browser;

    /** 操作系统（解析自 userAgent） */
    private String os;

    /** 状态：SUCCESS 或 FAIL（FAILURE 映射为 FAIL） */
    private String status;

    /** 登录时间（来自 createTime） */
    private String loginTime;

    /** 失败原因（来自 exception 字段） */
    private String failReason;
}
