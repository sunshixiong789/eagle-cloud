package com.eagle.system.base.interfaces.dto.response;

/**
 * 登录日志列表项响应（前端友好字段）
 *
 * @param ip         登录 IP（来自 remoteAddr）
 * @param browser    浏览器（解析自 userAgent）
 * @param os         操作系统（解析自 userAgent）
 * @param status     状态：SUCCESS 或 FAIL（FAILURE 映射为 FAIL）
 * @param loginTime  登录时间（来自 createTime）
 * @param failReason 失败原因（来自 exception 字段）
 */
public record LoginLogItemResponse(
        Long id,
        Long userId,
        String username,
        String ip,
        String browser,
        String os,
        String status,
        String loginTime,
        String failReason
) {
}
