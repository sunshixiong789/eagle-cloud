package com.eagle.tenant.filter;

import com.eagle.tenant.TenantContextHolder;
import com.eagle.tenant.properties.TenantProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Reactive tenant ID resolver for WebFlux applications.
 *
 * <p>把 tenantId 同时写入：
 * <ul>
 *   <li>{@link ServerWebExchange#getAttributes()}</li>
 *   <li>{@link TenantContextHolder}（ThreadLocal，配合 Reactor Context 自动传播在响应式链上跨线程可见）</li>
 *   <li>Reactor Context（业务侧也可直接 deferContextual 读）</li>
 * </ul>
 *
 * <p>ThreadLocal 的跨线程透传依赖
 * {@link com.eagle.tenant.config.TenantContextPropagationRegistrar}
 * 注册的 {@code ThreadLocalAccessor}，以及 common-starter 在 WebFlux 启动时调用的
 * {@code Hooks.enableAutomaticContextPropagation()}。
 *
 * @author 孙士雄
 */
@Slf4j
@RequiredArgsConstructor
public class ReactiveTenantIdWebFilter implements WebFilter, Ordered {

    /**
     * Reactor context key that stores the resolved tenant id.
     */
    public static final String TENANT_ID_CONTEXT_KEY = TenantContextHolder.class.getName() + ".tenantId";

    private final TenantProperties properties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String tenantId = resolveTenantId(exchange.getRequest().getHeaders());
        exchange.getAttributes().put(TENANT_ID_CONTEXT_KEY, tenantId);
        TenantContextHolder.setTenantId(tenantId);
        log.debug("Tenant resolved: {}", tenantId);
        try {
            return chain.filter(exchange)
                    .contextWrite(context -> context.put(TENANT_ID_CONTEXT_KEY, tenantId));
        } finally {
            // 过滤器线程的 ThreadLocal 立即清理，下游线程依赖 ContextRegistry 自动传播。
            TenantContextHolder.clear();
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 50;
    }

    private String resolveTenantId(HttpHeaders headers) {
        String tenantId = headers.getFirst(properties.getHeaderName());
        if (tenantId == null || tenantId.isBlank()) {
            return properties.getDefaultTenantId();
        }
        return tenantId;
    }
}
