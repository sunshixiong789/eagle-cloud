package com.eagle.webclient.interceptor;

import com.eagle.tenant.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

/**
 * WebClient 请求过滤器：透传当前租户 ID。
 *
 * <p>{@link TenantContextHolder} 已通过 {@code eagle-common-starter} 的
 * {@code ContextPropagationConfig} 与 Reactor Context 双向桥接，因此 reactive 链路下
 * 也能正确读取当前租户 ID。
 *
 * @author eagle
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
        ClientRequest mutated = ClientRequest.from(request)
                .header(TENANT_HEADER, tenantId)
                .build();
        log.debug("Tenant ID propagated to downstream (reactive): {}", tenantId);
        return next.exchange(mutated);
    }
}
