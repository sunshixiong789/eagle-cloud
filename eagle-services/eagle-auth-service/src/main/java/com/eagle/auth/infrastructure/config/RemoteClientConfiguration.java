package com.eagle.auth.infrastructure.config;

import com.eagle.auth.infrastructure.remote.SystemAuthorizationClient;
import com.eagle.http.client.support.EagleRestServiceClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 注册 auth-service 对 system-service 的同步 HTTP 客户端代理。
 * <p>
 * 服务 ID {@code eagle-system-service} 通过 Nacos 服务发现解析,
 * restclient-starter 自动注入负载均衡 + Authorization / X-Tenant-Id / TX_XID 透传。
 */
@Configuration(proxyBeanMethods = false)
class RemoteClientConfiguration {

    private static final String SYSTEM_SERVICE_ID = "system";

    @Bean
    SystemAuthorizationClient systemAuthorizationClient(EagleRestServiceClientFactory factory) {
        return factory.createLoadBalancedClient(SystemAuthorizationClient.class, SYSTEM_SERVICE_ID);
    }
}
