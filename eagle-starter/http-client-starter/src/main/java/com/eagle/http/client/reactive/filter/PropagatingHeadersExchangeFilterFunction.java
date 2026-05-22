package com.eagle.http.client.reactive.filter;

import com.eagle.common.pressuretest.PressureTestContext;
import com.eagle.http.client.properties.HttpClientProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * WebClient 拦截器：透传当前入站请求的 Header 与压测标记。
 *
 * <p>响应式版本：从 {@link reactor.util.context.Context} 中取 {@link ServerWebExchange}，
 * 读取入站请求 Header；当不在 WebFlux 请求线程中（如调度任务）时跳过。
 *
 * @author 孙士雄
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
            ClientRequest.Builder builder = ClientRequest.from(request);

            if (pressureTestHeaderEnabled && PressureTestContext.isPressureTest()) {
                builder.header(PressureTestContext.PRESSURE_TEST_HEADER, "true");
            }

            ServerWebExchange exchange = ctx.getOrDefault(ServerWebExchange.class, null);
            if (exchange != null) {
                HttpHeaders inbound = exchange.getRequest().getHeaders();
                for (String headerName : propagatedHeaders) {
                    String value = inbound.getFirst(headerName);
                    if (value != null) {
                        builder.header(headerName, value);
                        if (log.isDebugEnabled()) {
                            log.debug("HTTP header propagated (reactive): {}", headerName);
                        }
                    }
                }
            }
            return next.exchange(builder.build());
        });
    }
}
