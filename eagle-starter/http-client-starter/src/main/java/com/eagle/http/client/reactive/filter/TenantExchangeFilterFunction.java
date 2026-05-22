package com.eagle.http.client.reactive.filter;

import com.eagle.tenant.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

/**
 * WebClient 拦截器：透传当前租户 ID。
 *
 * <p>依赖 {@code eagle-tenant-starter} 的 WebFlux 适配（通过 {@code ThreadLocalAccessor}
 * 把 {@code TenantContextHolder} 与 Reactor Context 绑定）。
 *
 * @author 孙士雄
 */
@Slf4j
public class TenantExchangeFilterFunction implements ExchangeFilterFunction {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return next.exchange(request);
        }
        ClientRequest mutated = ClientRequest.from(request).header(TENANT_HEADER, tenantId).build();
        if (log.isDebugEnabled()) {
            log.debug("Tenant ID propagated to downstream (reactive): {}", tenantId);
        }
        return next.exchange(mutated);
    }
}
