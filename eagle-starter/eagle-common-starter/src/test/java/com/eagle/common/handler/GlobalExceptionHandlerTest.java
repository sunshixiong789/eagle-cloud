package com.eagle.common.handler;

import com.eagle.common.dto.ErrorResult;
import com.eagle.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link GlobalExceptionHandler} 状态码映射测试。
 *
 * <p>重点是兜底分支不得吞掉 Spring MVC 自带语义状态码的异常
 * （历史缺陷：缺请求头 / 路径无映射一律返回 500）。
 *
 * @author eagle
 */
@DisplayName("Servlet 全局异常处理器")
class GlobalExceptionHandlerTest {

    private enum TestErrorCode implements ErrorCode {

        RESOURCE_FORBIDDEN(99002, "error.test.forbidden", "无权访问他人的资源");

        private final Meta meta;

        TestErrorCode(int code, String messageKey, String defaultMessage) {
            this.meta = new Meta(code, messageKey, defaultMessage);
        }

        @Override
        public Meta meta() {
            return meta;
        }
    }

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(new StaticMessageSource());
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/withdrawals");

    @Nested
    @DisplayName("Spring MVC 标准异常按自带状态码返回")
    class BuiltInMvcException {

        @Test
        @DisplayName("缺必填请求头返回 400 而非 500")
        void shouldReturn400WhenRequiredHeaderMissing() throws Exception {
            MethodParameter parameter = new MethodParameter(
                    Sample.class.getDeclaredMethod("apply", String.class), 0);
            ResponseEntity<ErrorResult> response = handler.handleException(
                    new MissingRequestHeaderException("Idempotency-Key", parameter),
                    request, Locale.CHINA);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
        }

        @Test
        @DisplayName("路径无映射返回 404 而非 500")
        void shouldReturn404WhenNoHandlerMatched() {
            ResponseEntity<ErrorResult> response = handler.handleException(
                    new NoResourceFoundException(org.springframework.http.HttpMethod.GET,
                            "/admin/rebate-policies", "/admin/rebate-policies"),
                    request, Locale.CHINA);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("非 Web 异常仍兜底 500")
        void shouldFallbackTo500() {
            ResponseEntity<ErrorResult> response =
                    handler.handleException(new RuntimeException("boom"), request, Locale.CHINA);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("鉴权与越权")
    class Authorization {

        @Test
        @DisplayName("@PreAuthorize 拒绝返回 403")
        void shouldReturn403WhenAuthorizationDenied() {
            ErrorResult result = handler.handleAccessDenied(
                    new AuthorizationDeniedException("Access Denied", new AuthorizationDecision(false)),
                    request, Locale.CHINA);

            assertThat(result.getStatus()).isEqualTo(403);
        }

        @Test
        @DisplayName("业务 ForbiddenException 返回 403 且携带 errorCode")
        void shouldReturn403WithErrorCode() {
            ErrorResult result = handler.handleForbiddenException(
                    TestErrorCode.RESOURCE_FORBIDDEN.toForbiddenException(), request, Locale.CHINA);

            assertThat(result.getStatus()).isEqualTo(403);
            assertThat(result.getErrorCode()).isEqualTo(99002);
        }
    }

    @SuppressWarnings("unused")
    private static final class Sample {
        void apply(String idempotencyKey) {
        }
    }
}
