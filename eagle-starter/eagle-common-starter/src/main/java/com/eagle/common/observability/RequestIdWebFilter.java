package com.eagle.common.observability;

import com.eagle.common.dto.ErrorResult;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Request ID propagation filter for WebFlux applications.
 *
 * @author 孙士雄
 */
public class RequestIdWebFilter implements WebFilter, Ordered {

    /**
     * Exchange attribute that stores the resolved request id.
     */
    public static final String REQUEST_ID_ATTRIBUTE = RequestIdWebFilter.class.getName() + ".requestId";

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, WebFilterChain chain) {
        String requestId = resolveRequestId(exchange);
        exchange.getAttributes().put(REQUEST_ID_ATTRIBUTE, requestId);
        exchange.getResponse().getHeaders().set(REQUEST_ID_HEADER, requestId);
        return chain.filter(exchange).contextWrite(context -> context.put(ErrorResult.MDC_REQUEST_ID, requestId));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private String resolveRequestId(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        String requestId = headers.getFirst(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return requestId;
    }
}
