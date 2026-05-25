package com.eagle.system.base.infrastructure.config;

import com.eagle.http.client.support.EagleRestServiceClientFactory;
import com.eagle.system.base.infrastructure.remote.AuthAccountBlacklistClient;
import com.eagle.system.base.infrastructure.remote.AuthAccountClient;
import com.eagle.system.base.infrastructure.remote.AuthOnlineUserClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 注册 base 模块对 auth-service 的同步 HTTP 客户端代理。
 * <p>
 * 服务 ID {@code auth}(对应 auth-service 的 spring.application.name)通过 Nacos 服务发现解析,
 * restclient-starter 自动注入负载均衡 + Authorization / X-Tenant-Id / TX_XID 透传。
 */
@Configuration(proxyBeanMethods = false)
class RemoteClientConfiguration {

    private static final String AUTH_SERVICE_ID = "auth";

    @Bean
    AuthOnlineUserClient authOnlineUserClient(EagleRestServiceClientFactory factory) {
        return factory.createLoadBalancedClient(AuthOnlineUserClient.class, AUTH_SERVICE_ID);
    }

    @Bean
    AuthAccountBlacklistClient authAccountBlacklistClient(EagleRestServiceClientFactory factory) {
        return factory.createLoadBalancedClient(AuthAccountBlacklistClient.class, AUTH_SERVICE_ID);
    }

    @Bean
    AuthAccountClient authAccountClient(EagleRestServiceClientFactory factory) {
        return factory.createLoadBalancedClient(AuthAccountClient.class, AUTH_SERVICE_ID);
    }
}
