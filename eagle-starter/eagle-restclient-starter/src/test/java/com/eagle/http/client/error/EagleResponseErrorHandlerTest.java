package com.eagle.http.client.error;

import com.eagle.common.exception.ConflictException;
import com.eagle.common.exception.DomainException;
import com.eagle.common.exception.NotFoundException;
import com.eagle.common.exception.ServiceException;
import com.eagle.common.exception.codes.ExternalErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link EagleResponseErrorHandler} 单元测试。
 *
 * <p>验证：RestClient 下游 HTTP 错误 → 正确类型的 AppException，
 * 且 message 从 ErrorResult JSON 提取。
 *
 * @author 孙士雄
 */
class EagleResponseErrorHandlerTest {

    private static final URI URL = URI.create("http://eagle-inventory-server/api/items/1");

    private final EagleResponseErrorHandler handler = new EagleResponseErrorHandler(new ObjectMapper());

    private MockClientHttpResponse response(int status, String body) {
        byte[] bytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        return new MockClientHttpResponse(bytes, status);
    }

    @Nested
    @DisplayName("下游返回标准 ErrorResult JSON")
    class WithValidErrorResultBody {

        @Test
        @DisplayName("404 → NotFoundException，message 从 JSON 提取")
        void shouldReturnNotFoundExceptionWith404() {
            String body = """
                    {"status":404,"message":"用户不存在","errorCode":10001,"path":"/api/users/99"}
                    """;

            Exception ex = assertThrows(Exception.class,
                    () -> handler.handleError(URL, HttpMethod.GET, response(404, body)));

            assertInstanceOf(NotFoundException.class, ex);
            assertEquals(ExternalErrorCode.EXTERNAL_SERVICE_DETAIL,
                    ((NotFoundException) ex).getErrorCode());
            assertEquals("用户不存在", ((NotFoundException) ex).getMessageArgs()[0]);
        }

        @Test
        @DisplayName("400 → DomainException，message 从 JSON 提取")
        void shouldReturnDomainExceptionWith400() {
            String body = """
                    {"status":400,"message":"订单金额不能为负数","errorCode":30001}
                    """;

            Exception ex = assertThrows(Exception.class,
                    () -> handler.handleError(URL, HttpMethod.POST, response(400, body)));

            assertInstanceOf(DomainException.class, ex);
            assertEquals("订单金额不能为负数", ((DomainException) ex).getMessageArgs()[0]);
        }

        @Test
        @DisplayName("409 → ConflictException，message 从 JSON 提取")
        void shouldReturnConflictExceptionWith409() {
            String body = """
                    {"status":409,"message":"客户端 ID 已存在","errorCode":15001}
                    """;

            Exception ex = assertThrows(Exception.class,
                    () -> handler.handleError(URL, HttpMethod.PUT, response(409, body)));

            assertInstanceOf(ConflictException.class, ex);
            assertEquals("客户端 ID 已存在", ((ConflictException) ex).getMessageArgs()[0]);
        }

        @Test
        @DisplayName("403 / 429 / 500 → ServiceException，message 从 JSON 提取")
        void shouldReturnServiceExceptionForOtherFailures() {
            String body = """
                    {"status":500,"message":"服务器内部错误","errorCode":50001}
                    """;

            Exception ex = assertThrows(Exception.class,
                    () -> handler.handleError(URL, HttpMethod.GET, response(500, body)));

            assertInstanceOf(ServiceException.class, ex);
            assertEquals("服务器内部错误", ((ServiceException) ex).getMessageArgs()[0]);
        }
    }

    @Nested
    @DisplayName("body 异常或缺失时的降级行为")
    class Fallback {

        @Test
        @DisplayName("body 为空 → 回退到 HTTP 状态码描述")
        void shouldFallbackToHttpStatusWhenBodyIsEmpty() {
            Exception ex = assertThrows(Exception.class,
                    () -> handler.handleError(URL, HttpMethod.GET, response(404, null)));

            assertInstanceOf(NotFoundException.class, ex);
            assertEquals("HTTP 404", ((NotFoundException) ex).getMessageArgs()[0]);
        }

        @Test
        @DisplayName("body 为非 JSON → 使用原始 body 文本")
        void shouldUseRawBodyWhenNotJson() {
            String htmlBody = "<html><body>503 Service Unavailable</body></html>";

            Exception ex = assertThrows(Exception.class,
                    () -> handler.handleError(URL, HttpMethod.GET, response(503, htmlBody)));

            assertInstanceOf(ServiceException.class, ex);
            assertEquals(htmlBody, ((ServiceException) ex).getMessageArgs()[0]);
        }

        @Test
        @DisplayName("body 超过 200 字符 → 截断并追加省略号")
        void shouldTruncateLongNonJsonBody() {
            String longBody = "x".repeat(300);

            Exception ex = assertThrows(Exception.class,
                    () -> handler.handleError(URL, HttpMethod.GET, response(500, longBody)));

            String message = (String) ((ServiceException) ex).getMessageArgs()[0];
            assertEquals(203, message.length());
            assertEquals("...", message.substring(200));
        }
    }
}
