package com.eagle.tenant.config;

import com.eagle.tenant.TenantContextHolder;
import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ThreadLocalAccessor;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;

/**
 * 把 {@link TenantContextHolder} 的 ThreadLocal 注册到 {@link ContextRegistry}，
 * 让 Reactor 链上的 tenantId 自动跨线程传播。
 *
 * <p>仅在 WebFlux 环境激活；Servlet 环境下 ThreadLocal 由
 * {@link com.eagle.tenant.filter.TenantIdFilter} 同步管理，无需额外桥接。
 *
 * @author 孙士雄
 */
@Slf4j
@ConditionalOnClass({ContextRegistry.class, ThreadLocalAccessor.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class TenantContextPropagationRegistrar {

    /**
     * 租户在 {@link ContextRegistry} 中的统一 key。
     */
    public static final String TENANT_CONTEXT_KEY = "eagle.tenant.id";

    @PostConstruct
    public void register() {
        ContextRegistry.getInstance().registerThreadLocalAccessor(new TenantAccessor());
        log.info("Tenant ThreadLocalAccessor registered: key={}", TENANT_CONTEXT_KEY);
    }

    private static final class TenantAccessor implements ThreadLocalAccessor<String> {

        @Override
        public Object key() {
            return TENANT_CONTEXT_KEY;
        }

        @Override
        public String getValue() {
            return TenantContextHolder.getTenantId();
        }

        @Override
        public void setValue(String value) {
            TenantContextHolder.setTenantId(value);
        }

        @Override
        public void setValue() {
            TenantContextHolder.clear();
        }
    }
}
