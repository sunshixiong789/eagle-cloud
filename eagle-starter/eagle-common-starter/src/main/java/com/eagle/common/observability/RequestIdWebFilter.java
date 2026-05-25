package com.eagle.common.observability;

import com.eagle.common.dto.ErrorResult;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
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
 * <p>同时把 requestId 写入：
 * <ul>
 *   <li>响应头 {@code X-Request-Id}</li>
 *   <li>{@link ServerWebExchange#getAttributes()}（响应序列化 / 异常处理直接读）</li>
 *   <li>{@link MDC}（配合 {@link ContextPropagationConfig} 在 Reactor 链上自动透传，
 *       日志格式 {@code %X{requestId}} 即可生效）</li>
 *   <li>Reactor Context（业务代码通过 {@code deferContextual} 显式取也可）</li>
 * </ul>
 *
 * @author eagle
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
        MDC.put(ErrorResult.MDC_REQUEST_ID, requestId);
        try {
            return chain.filter(exchange)
                    .contextWrite(context -> context.put(ErrorResult.MDC_REQUEST_ID, requestId));
        } finally {
            // 同步阶段已通过 MDC 写入；reactor 链上的可见性由 ContextRegistry 的 ThreadLocalAccessor 保证。
            // 这里立即清理避免污染过滤器线程的后续请求。
            MDC.remove(ErrorResult.MDC_REQUEST_ID);
        }
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
