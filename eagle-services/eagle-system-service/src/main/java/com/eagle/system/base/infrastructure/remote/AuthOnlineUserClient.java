package com.eagle.system.base.infrastructure.remote;

import com.eagle.system.base.infrastructure.remote.dto.OnlineUserSnapshot;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

/**
 * 在线用户内部 API 客户端(调 auth-service /internal/online-users/**)。
 * <p>
 * 由 {@code RemoteClientConfiguration} 通过
 * {@code EagleRestServiceClientFactory.createLoadBalancedClient(...,"eagle-auth-service")}
 * 创建代理 bean。Authorization / X-Tenant-Id / Seata XID 由 restclient-starter
 * 自动透传。
 */
@HttpExchange("/internal/online-users")
public interface AuthOnlineUserClient {

    /** 列出所有在线用户。 */
    @GetExchange
    List<OnlineUserSnapshot> listOnlineUsers();

    /** 反查某账号当前所有在线 JTI(空集合 = 未在线)。 */
    @GetExchange("/by-account/{accountId}")
    List<String> listJtisByAccount(@PathVariable Long accountId);

    /** 强制下线指定 token。 */
    @DeleteExchange("/{tokenId}")
    void forceLogout(@PathVariable String tokenId);
}
