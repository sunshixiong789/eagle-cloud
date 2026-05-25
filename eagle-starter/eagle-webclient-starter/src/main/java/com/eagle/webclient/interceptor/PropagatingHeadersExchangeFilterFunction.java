package com.eagle.webclient.interceptor;

import com.eagle.common.http.HttpClientProperties;
import com.eagle.common.pressuretest.PressureTestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.reactive.ServerWebExchangeContextFilter;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * WebClient 请求过滤器：透传入站请求 Header 与压测标记（reactive 版）。
 *
 * <h2>入站 Header 透传</h2>
 * <p>从 Reactor Context 读取当前 {@link ServerWebExchange}（由 Spring WebFlux 的
 * {@link ServerWebExchangeContextFilter} 自动注入），把
 * {@link HttpClientProperties#getPropagatedHeaders()} 名单内的 header 复制到下游 ClientRequest。
 *
 * <p>仅在请求链路位于 WebFlux web 处理流程时有效；非 web 上下文（独立 reactive 任务）
 * 会跳过此步骤。
 *
 * <h2>压测标记</h2>
 * <p>{@link PressureTestContext} 已通过 {@code eagle-common-starter} 的
 * {@code ContextPropagationConfig} 桥接到 Reactor Context，因此 reactive 链路下
 * 仍可通过 {@code isPressureTest()} 读取压测标志，独立于 ServerWebExchange。
 *
 * @author eagle
 */
@Slf4j
public class PropagatingHeadersExchangeFilterFunction implements ExchangeFilterFunction {

    private final List<String> propagatedHeaders;
    private final boolean pressureTestHeaderEnabled;

    public PropagatingHeadersExchangeFilterFunction(HttpClientProperties properties) {
        this.propagatedHeaders = List.copyOf(properties.getPropagatedHeaders());
        this.pressureTestHeaderEnabled = properties.isPressureTestHeaderEnabled();
    }

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        return Mono.deferContextual(ctx -> {
            ClientRequest.Builder mutated = ClientRequest.from(request);
            boolean changed = false;

            if (pressureTestHeaderEnabled && PressureTestContext.isPressureTest()) {
                mutated.header(PressureTestContext.PRESSURE_TEST_HEADER, "true");
                changed = true;
            }

            ServerWebExchange exchange = ServerWebExchangeContextFilter.getExchange(ctx).orElse(null);
            if (exchange != null) {
                HttpHeaders inbound = exchange.getRequest().getHeaders();
                for (String headerName : propagatedHeaders) {
                    List<String> values = inbound.get(headerName);
                    if (values != null && !values.isEmpty()) {
                        mutated.header(headerName, values.toArray(new String[0]));
                        changed = true;
                        log.debug("HTTP header propagated (reactive): {}", headerName);
                    }
                }
            }

            return next.exchange(changed ? mutated.build() : request);
        });
    }
}
