package com.eagle.system.base.infrastructure.remote;

import com.eagle.system.base.infrastructure.remote.dto.AccountSnapshot;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Account 内部 API 客户端(调 auth-service /internal/accounts/**)。
 * <p>
 * 主用途:RoleDataInitializer 启动期主动拉 admin Account 兜底,消除"AccountRegisteredEvent
 * 只首次发布 + MQ 链路不通即永久丢失"的强耦合。
 * <p>
 * 由 {@code RemoteClientConfiguration} 通过
 * {@code EagleRestServiceClientFactory.createLoadBalancedClient(...,"auth")}
 * 创建代理 bean,JWT / X-Tenant-Id / Seata XID 由 restclient-starter 自动透传。
 */
@HttpExchange("/internal/accounts")
public interface AuthAccountClient {

    /** 按用户名查 Account 快照;Account 不存在时 RestClient 错误处理器抛 NotFoundException。 */
    @GetExchange("/by-username/{username}")
    AccountSnapshot findByUsername(@PathVariable String username);
}
