package com.eagle.http.client.reactive.filter;

import com.eagle.common.pressuretest.PressureTestContext;
import com.eagle.http.client.properties.HttpClientProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 {@link PropagatingHeadersExchangeFilterFunction} 把入站 Header 与压测标记透传到下游请求。
 *
 * @author 孙士雄
 */
class PropagatingHeadersExchangeFilterFunctionTest {

    private static final URI DOWNSTREAM = URI.create("http://downstream/api");

    private final HttpClientProperties properties = new HttpClientProperties();
    private final PropagatingHeadersExchangeFilterFunction filter =
            new PropagatingHeadersExchangeFilterFunction(properties);

    @AfterEach
    void clearPressureTest() {
        PressureTestContext.clear();
    }

    @Test
    void propagatesInboundHeadersWhenExchangeAvailable() {
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/upstream")
                .header(HttpHeaders.AUTHORIZATION, "Bearer abc")
                .header(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN")
                .header("X-Request-Id", "req-1")
                .build());

        AtomicReference<ClientRequest> captured = new AtomicReference<>();
        ExchangeFunction next = req -> {
            captured.set(req);
            return Mono.just(emptyOk());
        };

        ClientRequest request = ClientRequest.create(HttpMethod.GET, DOWNSTREAM).build();
        filter.filter(request, next)
                .contextWrite(Context.of(ServerWebExchange.class, exchange))
                .block();

        HttpHeaders headers = captured.get().headers();
        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer abc");
        assertThat(headers.getFirst(HttpHeaders.ACCEPT_LANGUAGE)).isEqualTo("zh-CN");
        assertThat(headers.getFirst("X-Request-Id")).isEqualTo("req-1");
    }

    @Test
    void propagatesPressureTestFlagWhenMarked() {
        PressureTestContext.mark();

        AtomicReference<ClientRequest> captured = new AtomicReference<>();
        ExchangeFunction next = req -> {
            captured.set(req);
            return Mono.just(emptyOk());
        };

        ClientRequest request = ClientRequest.create(HttpMethod.GET, DOWNSTREAM).build();
        filter.filter(request, next).block();

        assertThat(captured.get().headers().getFirst(PressureTestContext.PRESSURE_TEST_HEADER))
                .isEqualTo("true");
    }

    @Test
    void passesThroughWhenNoInboundExchange() {
        properties.setPropagatedHeaders(List.of(HttpHeaders.AUTHORIZATION));

        AtomicReference<ClientRequest> captured = new AtomicReference<>();
        ExchangeFunction next = req -> {
            captured.set(req);
            return Mono.just(emptyOk());
        };

        ClientRequest request = ClientRequest.create(HttpMethod.GET, DOWNSTREAM).build();
        filter.filter(request, next).block();

        assertThat(captured.get().headers().getFirst(HttpHeaders.AUTHORIZATION)).isNull();
    }

    private static ClientResponse emptyOk() {
        return ClientResponse.create(HttpStatus.OK).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
