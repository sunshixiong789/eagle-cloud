package com.eagle.gateway.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 别名路由配置：仅当 {@code eagle.gateway.alias.enabled=true} 且类路径存在
 * {@link DiscoveryClient}（即接入 Nacos）时启用。本地无注册中心场景应在
 * profile 中关闭，回退到 yml 静态路由。
 *
 * @author 孙士雄
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GatewayAliasProperties.class)
@ConditionalOnProperty(name = "eagle.gateway.alias.enabled", havingValue = "true", matchIfMissing = true)
public class AliasRouteConfig {

    @Bean
    public AliasRouteDefinitionLocator aliasRouteDefinitionLocator(
            DiscoveryClient discoveryClient,
            GatewayAliasProperties properties) {
        return new AliasRouteDefinitionLocator(discoveryClient, properties);
    }
}
