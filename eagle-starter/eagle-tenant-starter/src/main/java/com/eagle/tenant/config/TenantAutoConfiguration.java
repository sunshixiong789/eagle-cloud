package com.eagle.tenant.config;

import com.eagle.tenant.aspect.TenantDatabaseRoutingAspect;
import com.eagle.tenant.aspect.TenantFilterAspect;
import com.eagle.tenant.filter.TenantIdFilter;
import com.eagle.tenant.properties.TenantProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 多租户自动配置。
 *
 * @author 孙士雄
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(TenantProperties.class)
@ConditionalOnProperty(prefix = "eagle.tenant", name = "enabled", havingValue = "true")
public class TenantAutoConfiguration {

    @Bean
    public TenantIdFilter tenantIdFilter(TenantProperties properties) {
        return new TenantIdFilter(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "eagle.tenant", name = "mode", havingValue = "column")
    public TenantFilterAspect tenantFilterAspect() {
        log.info("Tenant COLUMN mode enabled");
        return new TenantFilterAspect();
    }

    @Bean
    @ConditionalOnProperty(prefix = "eagle.tenant", name = "mode", havingValue = "database")
    public TenantDatabaseRoutingAspect tenantDatabaseRoutingAspect() {
        log.info("Tenant DATABASE mode enabled");
        return new TenantDatabaseRoutingAspect();
    }
}
