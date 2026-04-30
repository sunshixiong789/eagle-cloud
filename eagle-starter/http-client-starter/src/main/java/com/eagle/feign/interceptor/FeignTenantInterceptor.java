package com.eagle.feign.interceptor;

import com.eagle.tenant.TenantContextHolder;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;

/**
 * Feign 租户 ID 透传拦截器。
 *
 * <p>将当前线程的租户 ID（由 {@link TenantContextHolder} 持有）透传到下游服务的
 * {@code X-Tenant-Id} 请求头，保证多租户场景下端到端的租户上下文一致性。
 *
 * <p>此拦截器仅在 {@code eagle-tenant-starter} 存在于类路径时由自动配置注册。
 *
 * @author 孙士雄
 */
@Slf4j
public class FeignTenantInterceptor implements RequestInterceptor {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    @Override
    public void apply(RequestTemplate template) {
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null && !tenantId.isBlank()) {
            template.header(TENANT_HEADER, tenantId);
            log.debug("Tenant ID propagated to downstream: {}", tenantId);
        }
    }
}
