package com.eagle.auth.interfaces.controller.internal;

import com.eagle.auth.domain.port.OnlineUserInfo;
import com.eagle.auth.domain.port.OnlineUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 在线用户相关的内部 API（仅供 system-service 通过服务发现调用）。
 *
 * <p>此控制器隶属 {@code /internal/**} 命名空间，由网关 IP 白名单 + client-credentials
 * OAuth2 scope 鉴权（参见 application.yml）。
 *
 * @author sunshixiong
 */
@RestController
@RequestMapping("/internal/online-users")
@RequiredArgsConstructor
public class OnlineUserInternalController {

    private final OnlineUserPort onlineUserPort;

    /**
     * 列出所有在线用户。
     */
    @GetMapping
    public List<OnlineUserInfo> listOnlineUsers() {
        return onlineUserPort.listOnlineUsers();
    }

    /**
     * 反查某账号当前所有在线 JTI（空集合表示未在线）。
     */
    @GetMapping("/by-account/{accountId}")
    public List<String> listJtisByAccount(@PathVariable Long accountId) {
        return onlineUserPort.listJtisByAccount(accountId);
    }

    /**
     * 强制下线指定 token。
     */
    @DeleteMapping("/{tokenId}")
    public void forceLogout(@PathVariable String tokenId) {
        onlineUserPort.forceLogout(tokenId);
    }
}
