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
        log.debug("Tenant resolved: {}", tenantId);
        return chain.filter(exchange)
                .contextWrite(context -> context.put(TENANT_ID_CONTEXT_KEY, tenantId));
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
