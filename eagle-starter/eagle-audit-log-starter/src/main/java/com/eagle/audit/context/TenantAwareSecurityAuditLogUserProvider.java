package com.eagle.audit.context;

import com.eagle.tenant.TenantContextHolder;

/**
 * {@link SecurityAuditLogUserProvider} 的多租户增强版。
 *
 * <p>覆写 {@link #getCurrentTenantId()},从 {@link TenantContextHolder} 拿当前请求的租户 ID。
 *
 * <p>启用条件:消费方类路径有 {@code eagle-tenant-starter}。
 * {@link com.eagle.audit.config.EagleAuditLogSecurityAutoConfiguration} 通过
 * {@code ClassUtils.isPresent} 检测后路由到本实现。
 *
 * @author eagle
 */
public class TenantAwareSecurityAuditLogUserProvider extends SecurityAuditLogUserProvider {

    @Override
    public String getCurrentTenantId() {
        return TenantContextHolder.getTenantId();
    }
}
