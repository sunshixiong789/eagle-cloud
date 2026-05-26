package com.eagle.auth.core.domain.port;

import java.time.LocalDateTime;

/**
 * 在线用户信息（用于 Redis 存储和跨模块传递）。
 *
 * @param tokenId        JWT JTI，唯一标识一次 token
 * @param userId         用户 ID（system.User 的主键，可能为 null）
 * @param username       登录用户名
 * @param ip             客户端 IP 地址
 * @param loginTime      登录时间
 * @param lastActiveTime 最后活跃时间
 * @param browser        浏览器类型
 * @param os             操作系统
 * @param expiresIn      token 有效期（秒），用于 Redis TTL
 */
public record OnlineUserInfo(
        String tokenId,
        Long userId,
        String username,
        String ip,
        LocalDateTime loginTime,
        LocalDateTime lastActiveTime,
        String browser,
        String os,
        long expiresIn
) {
}
