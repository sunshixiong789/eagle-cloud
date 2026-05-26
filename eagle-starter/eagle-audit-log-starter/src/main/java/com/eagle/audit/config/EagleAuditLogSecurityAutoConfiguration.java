package com.eagle.audit.config;

import com.eagle.audit.context.AuditLogUserProvider;
import com.eagle.audit.context.SecurityAuditLogUserProvider;
import com.eagle.audit.context.TenantAwareSecurityAuditLogUserProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.ClassUtils;

/**
 * 基于 Spring Security 的 {@link AuditLogUserProvider} 自动配置。
 *
 * <p>类路径有 SecurityContextHolder + Jwt 时启用,覆盖主配置中的匿名 Provider。
 *
 * <p>租户感知:进一步检测 {@code com.eagle.tenant.TenantContextHolder} 是否在类路径,
 * 若存在则路由到 {@link TenantAwareSecurityAuditLogUserProvider}
 * (从 ThreadLocal 透传当前 tenantId 到审计 entry);否则回退到普通
 * {@link SecurityAuditLogUserProvider}(tenantId 为 null)。
 *
 * <p>{@code ClassUtils.isPresent} 在 starter 类加载时检测一次,后续 new 安全;
 * JVM 字节码 lazy verify,只有 isPresent 返回 true 才会实际加载 TenantAware 类。
 *
 * @author eagle
 */
@AutoConfiguration
@AutoConfigureBefore(EagleAuditLogAutoConfiguration.class)
@ConditionalOnClass({SecurityContextHolder.class, Jwt.class})
public class EagleAuditLogSecurityAutoConfiguration {

    private static final String TENANT_CONTEXT_HOLDER_CLASS = "com.eagle.tenant.TenantContextHolder";

    @Bean
    @ConditionalOnMissingBean(AuditLogUserProvider.class)
    public AuditLogUserProvider securityAuditLogUserProvider() {
        ClassLoader classLoader = EagleAuditLogSecurityAutoConfiguration.class.getClassLoader();
        if (ClassUtils.isPresent(TENANT_CONTEXT_HOLDER_CLASS, classLoader)) {
            return new TenantAwareSecurityAuditLogUserProvider();
        }
        return new SecurityAuditLogUserProvider();
    }
}
