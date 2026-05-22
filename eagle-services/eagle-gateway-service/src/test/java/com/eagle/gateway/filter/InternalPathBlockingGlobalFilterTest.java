package com.eagle.gateway.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("InternalPathBlockingGlobalFilter")
class InternalPathBlockingGlobalFilterTest {

    private final InternalPathBlockingGlobalFilter filter = new InternalPathBlockingGlobalFilter();

    private static MockServerWebExchange exchangeFor(String path) {
        MockServerHttpRequest request = MockServerHttpRequest.get(path).build();
        return MockServerWebExchange.from(request);
    }

    @Nested
    @DisplayName("拦截外部访问 /internal/**")
    class Block {

        @Test
        @DisplayName("/internal/online-users 应返回 403")
        void blocksDirectInternalPath() {
            MockServerWebExchange exchange = exchangeFor("/internal/online-users");
            GatewayFilterChain chain = mock(GatewayFilterChain.class);

            filter.filter(exchange, chain).block();

            assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
            verify(chain, never()).filter(exchange);
        }

        @Test
        @DisplayName("服务发现路由 /eagle-auth-service/internal/online-users 应返回 403")
        void blocksDiscoveryRoutedInternalPath() {
            MockServerWebExchange exchange =
                    exchangeFor("/eagle-auth-service/internal/online-users");
            GatewayFilterChain chain = mock(GatewayFilterChain.class);

            filter.filter(exchange, chain).block();

            assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
            verify(chain, never()).filter(exchange);
        }

        @Test
        @DisplayName("大小写变体 /Internal/foo 应被拦截")
        void blocksCaseInsensitive() {
            MockServerWebExchange exchange = exchangeFor("/Internal/foo");
            GatewayFilterChain chain = mock(GatewayFilterChain.class);

            filter.filter(exchange, chain).block();

            assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        }

        @Test
        @DisplayName("末尾路径 /system/internal 应被拦截")
        void blocksTrailingInternal() {
            MockServerWebExchange exchange = exchangeFor("/system/internal");
            GatewayFilterChain chain = mock(GatewayFilterChain.class);

            filter.filter(exchange, chain).block();

            assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        }
    }

    @Nested
    @DisplayName("放行非内部路径")
    class Pass {

        @Test
        @DisplayName("/api/users 应转发到链路")
        void passesBusinessPath() {
            MockServerWebExchange exchange = exchangeFor("/api/users");
            GatewayFilterChain chain = mock(GatewayFilterChain.class);
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            filter.filter(exchange, chain).block();

            assertNull(exchange.getResponse().getStatusCode());
            verify(chain).filter(exchange);
        }

        @Test
        @DisplayName("/swagger-ui/index.html 应转发")
        void passesSwagger() {
            MockServerWebExchange exchange = exchangeFor("/swagger-ui/index.html");
            GatewayFilterChain chain = mock(GatewayFilterChain.class);
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            filter.filter(exchange, chain).block();

            verify(chain).filter(exchange);
        }

        @Test
        @DisplayName("路径含 internal 字串但非路径段不应误判 (/api/external-internal-data)")
        void doesNotMatchPartialWord() {
            // "/api/external-internal-data" 不含 "/internal/" 字串,应放行
            MockServerWebExchange exchange = exchangeFor("/api/external-internal-data");
            GatewayFilterChain chain = mock(GatewayFilterChain.class);
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            filter.filter(exchange, chain).block();

            verify(chain).filter(exchange);
        }
    }

    @Test
    @DisplayName("order 应早于业务路由匹配")
    void orderIsEarly() {
        assertTrue(filter.getOrder() < 0, "实际 order=" + filter.getOrder());
    }
}
