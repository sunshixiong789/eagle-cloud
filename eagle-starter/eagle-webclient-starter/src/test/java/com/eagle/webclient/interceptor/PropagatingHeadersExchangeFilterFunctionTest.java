package com.eagle.webclient.interceptor;

import com.eagle.common.http.HttpClientProperties;
import com.eagle.common.pressuretest.PressureTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.filter.reactive.ServerWebExchangeContextFilter;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PropagatingHeadersExchangeFilterFunction} 单元测试。
 *
 * @author eagle
 */
class PropagatingHeadersExchangeFilterFunctionTest {

    private final HttpClientProperties properties = new HttpClientProperties();
    private final PropagatingHeadersExchangeFilterFunction filter =
            new PropagatingHeadersExchangeFilterFunction(properties);

    @AfterEach
    void tearDown() {
        PressureTestContext.clear();
    }

    @Test
    @DisplayName("入站 Header 名单内的值会透传到下游 ClientRequest")
    void shouldPropagateInboundHeadersFromExchangeContext() {
        MockServerHttpRequest inbound = MockServerHttpRequest.get("/api/in")
                .header("Authorization", "Bearer xyz")
                .header("Accept-Language", "zh-CN")
                .header("X-Request-Id", "req-123")
                .header("X-Custom-Not-Propagated", "secret")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(inbound);

        AtomicReference<ClientRequest> captured = new AtomicReference<>();
        ExchangeFunction next = req -> {
            captured.set(req);
            return Mono.just(ClientResponse.create(org.springframework.http.HttpStatus.OK).build());
        };

        ClientRequest out = ClientRequest.create(HttpMethod.GET, URI.create("http://downstream/api")).build();

        filter.filter(out, next)
                .contextWrite(injectExchange(exchange))
                .block();

        ClientRequest actual = captured.get();
        assertThat(actual).isNotNull();
        assertThat(actual.headers().getFirst("Authorization")).isEqualTo("Bearer xyz");
        assertThat(actual.headers().getFirst("Accept-Language")).isEqualTo("zh-CN");
        assertThat(actual.headers().getFirst("X-Request-Id")).isEqualTo("req-123");
        assertThat(actual.headers().getFirst("X-Custom-Not-Propagated")).isNull();
    }

    @Test
    @DisplayName("PressureTestContext 标记位 → 自动注入压测 Header")
    void shouldInjectPressureTestHeaderWhenContextMarked() {
        PressureTestContext.mark();

        AtomicReference<ClientRequest> captured = new AtomicReference<>();
        ExchangeFunction next = req -> {
            captured.set(req);
            return Mono.just(ClientResponse.create(org.springframework.http.HttpStatus.OK).build());
        };

        ClientRequest out = ClientRequest.create(HttpMethod.GET, URI.create("http://downstream/api")).build();

        filter.filter(out, next).block();

        ClientRequest actual = captured.get();
        assertThat(actual).isNotNull();
        assertThat(actual.headers().getFirst(PressureTestContext.PRESSURE_TEST_HEADER))
                .isEqualTo("true");
    }

    @Test
    @DisplayName("Reactor Context 中无 ServerWebExchange → 跳过入站 Header 透传，请求原样下发")
    void shouldPassThroughWhenNoExchangeInContext() {
        AtomicReference<ClientRequest> captured = new AtomicReference<>();
        ExchangeFunction next = req -> {
            captured.set(req);
            return Mono.just(ClientResponse.create(org.springframework.http.HttpStatus.OK).build());
        };

        ClientRequest out = ClientRequest.create(HttpMethod.GET, URI.create("http://downstream/api")).build();

        filter.filter(out, next).block();

        ClientRequest actual = captured.get();
        assertThat(actual).isNotNull();
        assertThat(actual.headers().getFirst("Authorization")).isNull();
    }

    private static java.util.function.Function<Context, Context> injectExchange(ServerWebExchange exchange) {
        return ctx -> ctx.put(ServerWebExchangeContextFilter.EXCHANGE_CONTEXT_ATTRIBUTE, exchange);
    }
}
