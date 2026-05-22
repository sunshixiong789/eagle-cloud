package com.eagle.common.pressuretest;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 全链路压测流量识别过滤器（WebFlux）。
 *
 * <p>读取请求头 {@code X-Eagle-Gray: true}，调用 {@link PressureTestContext#mark()}。
 * 结合 {@link com.eagle.common.observability.ContextPropagationConfig} 的 Reactor 自动 Context 传播，
 * ThreadLocal 在响应式调用链上自动可见。
 *
 * <p>order 与 servlet 端 {@link PressureTestFilter} 对齐
 * （{@code HIGHEST_PRECEDENCE + 20}），保证压测标记在租户 / 业务过滤器之前就绪。
 *
 * @author 孙士雄
 */
@Slf4j
public class ReactivePressureTestWebFilter implements WebFilter, Ordered {

    @Override
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, WebFilterChain chain) {
        String grayHeader = exchange.getRequest().getHeaders().getFirst(PressureTestContext.PRESSURE_TEST_HEADER);
        boolean pressure = "true".equalsIgnoreCase(grayHeader);
        if (pressure) {
            PressureTestContext.mark();
            log.debug("[PressureTest] Pressure test request detected, uri: {}",
                    exchange.getRequest().getPath().value());
        }
        try {
            return chain.filter(exchange);
        } finally {
            // 过滤器线程上的 ThreadLocal 立即清理，避免污染后续请求。
            // 真正传递到下游线程依赖 Reactor 自动 Context 传播 +
            // ContextRegistry 注册的 ThreadLocalAccessor。
            PressureTestContext.clear();
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
