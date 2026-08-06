package com.eagle.audit.config;

import com.eagle.audit.context.AuditLogUserProvider;
import com.eagle.audit.context.SecurityAuditLogUserProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * 基于 Spring Security 的 {@link AuditLogUserProvider} 自动配置。
 *
 * <p>类路径有 SecurityContextHolder + Jwt 时启用,覆盖主配置中的匿名 Provider。
 *
 * <p><b>多租户支持已移除</b>：随 {@code eagle-tenant-starter} 一并下线，
 * 原先按 {@code com.eagle.tenant.TenantContextHolder} 是否在类路径来路由到
 * {@code TenantAwareSecurityAuditLogUserProvider} 的逻辑不再需要，
 * 审计 entry 的 tenantId 恒为 null。
 *
 * @author eagle
 */
@AutoConfiguration
@AutoConfigureBefore(EagleAuditLogAutoConfiguration.class)
@ConditionalOnClass({SecurityContextHolder.class, Jwt.class})
public class EagleAuditLogSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AuditLogUserProvider.class)
    public AuditLogUserProvider securityAuditLogUserProvider() {
        return new SecurityAuditLogUserProvider();
    }
}
