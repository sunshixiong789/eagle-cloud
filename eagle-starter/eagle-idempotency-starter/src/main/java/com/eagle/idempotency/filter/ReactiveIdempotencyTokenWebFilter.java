package com.eagle.idempotency.filter;

import com.eagle.idempotency.support.ReactiveIdempotencyTokenContext;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Captures idempotency request headers for WebFlux handler invocation.
 *
 * @author 孙士雄
 */
public class ReactiveIdempotencyTokenWebFilter implements WebFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        Map<String, String> headers = new HashMap<>();
        exchange.getRequest().getHeaders().forEach((name, values) -> {
            if (!values.isEmpty()) {
                headers.put(name, values.get(0));
                headers.put(name.toLowerCase(), values.get(0));
            }
        });
        ReactiveIdempotencyTokenContext.setAll(headers);
        return chain.filter(exchange)
                .doFinally(signalType -> ReactiveIdempotencyTokenContext.clear());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 60;
    }
}
