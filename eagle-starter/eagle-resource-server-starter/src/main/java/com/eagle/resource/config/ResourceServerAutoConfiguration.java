package com.eagle.resource.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * 资源服务器自动配置
 * 当 eagle.security.oauth2.resource-server.enabled=true 时生效
 *
 * @author 孙士雄
 */
@AutoConfiguration
@ConditionalOnProperty(
        prefix = "eagle.security.oauth2.resource-server",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@EnableConfigurationProperties(ResourceServerProperties.class)
@Import({
        ResourceServerSecurityConfig.class,
        EagleJwtAuthenticationConverter.class
})
public class ResourceServerAutoConfiguration {
}
