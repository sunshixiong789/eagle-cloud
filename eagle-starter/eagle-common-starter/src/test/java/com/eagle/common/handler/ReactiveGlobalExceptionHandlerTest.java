package com.eagle.common.handler;

import com.eagle.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ReactiveGlobalExceptionHandler} 状态码映射测试。
 *
 * <p>聚焦三条曾经退化成 500 的路径：未匹配路由、方法级鉴权拒绝、业务越权。
 *
 * @author eagle
 */
@DisplayName("WebFlux 全局异常处理器")
class ReactiveGlobalExceptionHandlerTest {

    private enum TestErrorCode implements ErrorCode {

        RESOURCE_FORBIDDEN(99001, "error.test.forbidden", "无权访问他人的资源");

        private final Meta meta;

        TestErrorCode(int code, String messageKey, String defaultMessage) {
            this.meta = new Meta(code, messageKey, defaultMessage);
        }

        @Override
        public Meta meta() {
            return meta;
        }
    }

    private final ReactiveGlobalExceptionHandler handler =
            new ReactiveGlobalExceptionHandler(new ObjectMapper(), new StaticMessageSource());

    private MockServerWebExchange handle(Throwable ex) {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/nope"));
        handler.handle(exchange, ex).block();
        return exchange;
    }

    private String body(MockServerWebExchange exchange) {
        return exchange.getResponse().getBodyAsString().block();
    }

    @Nested
    @DisplayName("Spring 内建 Web 异常")
    class BuiltInWebException {

        @Test
        @DisplayName("未匹配路由的 ResponseStatusException(404) 返回 404 而非 500")
        void shouldReturn404WhenNoRouteMatched() {
            MockServerWebExchange exchange = handle(new ResponseStatusException(HttpStatus.NOT_FOUND));

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(body(exchange)).contains("\"status\":404");
        }

        @Test
        @DisplayName("下游无实例的 ResponseStatusException(503) 透传 503")
        void shouldPropagateServiceUnavailable() {
            MockServerWebExchange exchange = handle(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE));

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Nested
    @DisplayName("鉴权与越权")
    class Authorization {

        @Test
        @DisplayName("@PreAuthorize 抛的 AuthorizationDeniedException(AccessDeniedException 子类) 返回 403")
        void shouldReturn403WhenAuthorizationDenied() {
            MockServerWebExchange exchange = handle(
                    new AuthorizationDeniedException("Access Denied", new AuthorizationDecision(false)));

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("业务 ForbiddenException 返回 403 且携带 errorCode")
        void shouldReturn403WithErrorCode() {
            MockServerWebExchange exchange = handle(TestErrorCode.RESOURCE_FORBIDDEN.toForbiddenException());

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(body(exchange)).contains("\"errorCode\":99001");
        }
    }

    @Test
    @DisplayName("未识别异常仍兜底为 500")
    void shouldFallbackTo500() {
        MockServerWebExchange exchange = handle(new RuntimeException("boom"));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(body(exchange).getBytes(StandardCharsets.UTF_8)).isNotEmpty();
    }
}
