package com.eagle.system.base.interfaces.dto.response;

import java.time.LocalDateTime;

/**
 * 在线用户响应
 *
 * @param tokenId        JWT JTI，唯一标识本次 token
 * @param userId         用户 ID
 * @param username       登录用户名
 * @param ip             客户端 IP
 * @param loginTime      登录时间
 * @param lastActiveTime 最后活跃时间
 * @param browser        浏览器类型
 * @param os             操作系统
 */
public record OnlineUserResponse(
        String tokenId,
        Long userId,
        String username,
        String ip,
        LocalDateTime loginTime,
        LocalDateTime lastActiveTime,
        String browser,
        String os
) {
}
