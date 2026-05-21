package com.eagle.tenant.config;

import com.eagle.tenant.aspect.TenantDatabaseRoutingAspect;
import com.eagle.tenant.aspect.TenantFilterAspect;
import com.eagle.tenant.filter.ReactiveTenantIdWebFilter;
import com.eagle.tenant.filter.TenantIdFilter;
import com.eagle.tenant.properties.TenantProperties;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.server.WebFilter;

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

    /**
     * 注册租户 ID 解析过滤器，显式指定优先级。
     */
    @Bean
    @ConditionalOnMissingBean(TenantIdFilter.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public FilterRegistrationBean<TenantIdFilter> tenantIdFilterRegistration(TenantProperties properties) {
        log.info("Tenant filter registered, headerName={}, defaultTenantId={}",
                properties.getHeaderName(), properties.getDefaultTenantId());
        FilterRegistrationBean<TenantIdFilter> registration =
                new FilterRegistrationBean<>(new TenantIdFilter(properties));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 50);
        registration.addUrlPatterns("/*");
        return registration;
    }

    /**
     * Reactive tenant ID resolver for WebFlux applications.
     */
    @Bean
    @ConditionalOnMissingBean(ReactiveTenantIdWebFilter.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @ConditionalOnClass(DispatcherHandler.class)
    public WebFilter reactiveTenantIdWebFilter(TenantProperties properties) {
        log.info("Reactive tenant filter registered, headerName={}, defaultTenantId={}",
                properties.getHeaderName(), properties.getDefaultTenantId());
        return new ReactiveTenantIdWebFilter(properties);
    }

    /**
     * COLUMN 模式：通过 Hibernate Filter 实现共享库行级隔离。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(EntityManager.class)
    @ConditionalOnProperty(prefix = "eagle.tenant", name = "mode", havingValue = "column")
    public TenantFilterAspect tenantFilterAspect(EntityManager entityManager) {
        log.info("Tenant COLUMN mode enabled");
        return new TenantFilterAspect(entityManager);
    }

    /**
     * DATABASE 模式：通过动态数据源路由实现独立数据库隔离。
     * 仅在 {@code eagle-dynamic-datasource-starter} 存在于类路径时注册。
     */
    @Slf4j
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.eagle.datasource.routing.DataSourceContextHolder")
    @ConditionalOnProperty(prefix = "eagle.tenant", name = "mode", havingValue = "database")
    static class DatabaseModeConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public TenantDatabaseRoutingAspect tenantDatabaseRoutingAspect() {
            log.info("Tenant DATABASE mode enabled");
            return new TenantDatabaseRoutingAspect();
        }
    }
}
