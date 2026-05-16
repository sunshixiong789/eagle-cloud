package com.eagle.system.base.interfaces.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 在线用户响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnlineUserResponse {

    /**
     * JWT JTI，唯一标识本次 token
     */
    private String tokenId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 登录用户名
     */
    private String username;

    /**
     * 客户端 IP
     */
    private String ip;

    /**
     * 登录时间
     */
    private LocalDateTime loginTime;

    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveTime;

    /**
     * 浏览器类型
     */
    private String browser;

    /**
     * 操作系统
     */
    private String os;
}
