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
 * <p>把请求头快照写入 {@link ReactiveIdempotencyTokenContext} 后再触发链路。
 * 跨线程的可见性由 {@link com.eagle.idempotency.config.IdempotencyContextPropagationRegistrar}
 * 注册的 {@code ThreadLocalAccessor} 与 {@code Hooks.enableAutomaticContextPropagation()} 共同保证。
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
        try {
            return chain.filter(exchange)
                    .doFinally(signalType -> ReactiveIdempotencyTokenContext.clear());
        } finally {
            // 过滤器线程立即清理 ThreadLocal；下游线程依赖 ContextRegistry 自动恢复。
            ReactiveIdempotencyTokenContext.clear();
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 60;
    }
}
