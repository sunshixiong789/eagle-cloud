package com.eagle.system.auth.domain.port;

import java.util.List;

/**
 * 在线用户管理端口（Driven Port，六边形架构）。
 * <p>
 * 由 {@code auth/infrastructure/adapter/OnlineUserAdapter} 通过 Redis 实现。
 */
public interface OnlineUserPort {

    /**
     * 记录用户登录，将在线用户信息写入 Redis，TTL = {@link OnlineUserInfo#expiresIn()}。
     *
     * @param info 在线用户信息
     */
    void trackLogin(OnlineUserInfo info);

    /**
     * 获取所有在线用户列表。
     *
     * @return 当前在线用户列表（不含已过期的 token）
     */
    List<OnlineUserInfo> listOnlineUsers();

    /**
     * 强制下线：删除 Redis 在线记录并将 JTI 写入黑名单。
     *
     * @param tokenId JWT JTI
     */
    void forceLogout(String tokenId);

    /**
     * 检查 JWT JTI 是否已被加入黑名单（token 已被吊销）。
     *
     * @param jti JWT JTI
     * @return {@code true} 表示 token 已失效
     */
    boolean isBlacklisted(String jti);
}
