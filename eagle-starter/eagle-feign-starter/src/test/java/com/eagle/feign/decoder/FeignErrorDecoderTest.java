package com.eagle.feign.decoder;

import com.eagle.common.exception.ConflictException;
import com.eagle.common.exception.DomainException;
import com.eagle.common.exception.NotFoundException;
import com.eagle.common.exception.ServiceException;
import com.eagle.common.exception.codes.ExternalErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * FeignErrorDecoder 单元测试。
 *
 * <p>验证：下游 HTTP 错误 → 正确类型的 AppException，且 message 从 ErrorResult JSON 提取。
 *
 * @author 孙士雄
 */
class FeignErrorDecoderTest {

    private static final String METHOD_KEY = "InventoryFeignClient#getItem(Long)";

    private FeignErrorDecoder decoder;

    @BeforeEach
    void setUp() {
        decoder = new FeignErrorDecoder(new ObjectMapper());
    }

    // ==================== 辅助方法 ====================

    private Response buildResponse(int status, String body) {
        Request dummyRequest = Request.create(
                Request.HttpMethod.GET,
                "http://eagle-inventory-server/api/items/1",
                Map.of(),
                null,
                StandardCharsets.UTF_8,
                null);
        return Response.builder()
                .status(status)
                .reason("")
                .headers(Map.of())
                .body(body, StandardCharsets.UTF_8)
                .request(dummyRequest)
                .build();
    }

    private Response buildResponse(int status) {
        return buildResponse(status, null);
    }

    // ==================== 正常路径：下游返回标准 ErrorResult JSON ====================

    @Nested
    @DisplayName("下游返回标准 ErrorResult JSON")
    class WithValidErrorResultBody {

        @Test
        @DisplayName("404 → NotFoundException，message 从 JSON 提取")
        void shouldReturnNotFoundExceptionWith404() {
            String body = """
                    {"status":404,"message":"用户不存在","errorCode":10001,"path":"/api/users/99"}
                    """;
            Response response = buildResponse(404, body);

            Exception ex = decoder.decode(METHOD_KEY, response);

            assertInstanceOf(NotFoundException.class, ex);
            assertEquals(ExternalErrorCode.EXTERNAL_SERVICE_DETAIL,
                    ((NotFoundException) ex).getErrorCode());
            // messageArgs[0] 是提取的下游 message
            assertEquals("用户不存在", ((NotFoundException) ex).getMessageArgs()[0]);
        }

        @Test
        @DisplayName("400 → DomainException，message 从 JSON 提取")
        void shouldReturnDomainExceptionWith400() {
            String body = """
                    {"status":400,"message":"订单金额不能为负数","errorCode":30001}
                    """;

            Exception ex = decoder.decode(METHOD_KEY, buildResponse(400, body));

            assertInstanceOf(DomainException.class, ex);
            assertEquals("订单金额不能为负数", ((DomainException) ex).getMessageArgs()[0]);
        }

        @Test
        @DisplayName("409 → ConflictException，message 从 JSON 提取")
        void shouldReturnConflictExceptionWith409() {
            String body = """
                    {"status":409,"message":"客户端 ID 已存在","errorCode":15001}
                    """;

            Exception ex = decoder.decode(METHOD_KEY, buildResponse(409, body));

            assertInstanceOf(ConflictException.class, ex);
            assertEquals("客户端 ID 已存在", ((ConflictException) ex).getMessageArgs()[0]);
        }

        @Test
        @DisplayName("403 → ServiceException，message 从 JSON 提取")
        void shouldReturnServiceExceptionWith403() {
            String body = """
                    {"status":403,"message":"无权限访问","errorCode":10003}
                    """;

            Exception ex = decoder.decode(METHOD_KEY, buildResponse(403, body));

            assertInstanceOf(ServiceException.class, ex);
            assertEquals("无权限访问", ((ServiceException) ex).getMessageArgs()[0]);
        }

        @Test
        @DisplayName("429 → ServiceException，message 从 JSON 提取")
        void shouldReturnServiceExceptionWith429() {
            String body = """
                    {"status":429,"message":"请求过于频繁，请稍后再试","errorCode":10002}
                    """;

            Exception ex = decoder.decode(METHOD_KEY, buildResponse(429, body));

            assertInstanceOf(ServiceException.class, ex);
            assertEquals("请求过于频繁，请稍后再试", ((ServiceException) ex).getMessageArgs()[0]);
        }

        @Test
        @DisplayName("500 → ServiceException，message 从 JSON 提取")
        void shouldReturnServiceExceptionWith500() {
            String body = """
                    {"status":500,"message":"服务器内部错误","errorCode":50001}
                    """;

            Exception ex = decoder.decode(METHOD_KEY, buildResponse(500, body));

            assertInstanceOf(ServiceException.class, ex);
            assertEquals("服务器内部错误", ((ServiceException) ex).getMessageArgs()[0]);
        }
    }

    // ==================== 降级路径：body 异常或缺失 ====================

    @Nested
    @DisplayName("body 异常或缺失时的降级行为")
    class Fallback {

        @Test
        @DisplayName("body 为空 → 回退到 HTTP 状态码描述")
        void shouldFallbackToHttpStatusWhenBodyIsNull() {
            Exception ex = decoder.decode(METHOD_KEY, buildResponse(404));

            assertInstanceOf(NotFoundException.class, ex);
            assertEquals("HTTP 404", ((NotFoundException) ex).getMessageArgs()[0]);
        }

        @Test
        @DisplayName("body 为空字符串 → 回退到 HTTP 状态码描述")
        void shouldFallbackToHttpStatusWhenBodyIsBlank() {
            Exception ex = decoder.decode(METHOD_KEY, buildResponse(500, ""));

            assertInstanceOf(ServiceException.class, ex);
            assertEquals("HTTP 500", ((ServiceException) ex).getMessageArgs()[0]);
        }

        @Test
        @DisplayName("body 为非 JSON（如 HTML 错误页）→ 使用原始 body 文本")
        void shouldUseRawBodyWhenNotJson() {
            String htmlBody = "<html><body>503 Service Unavailable</body></html>";
            Exception ex = decoder.decode(METHOD_KEY, buildResponse(503, htmlBody));

            assertInstanceOf(ServiceException.class, ex);
            assertEquals(htmlBody, ((ServiceException) ex).getMessageArgs()[0]);
        }

        @Test
        @DisplayName("body 超过 200 字符 → 截断并追加省略号")
        void shouldTruncateLongNonJsonBody() {
            String longBody = "x".repeat(300);
            Exception ex = decoder.decode(METHOD_KEY, buildResponse(500, longBody));

            String message = (String) ((ServiceException) ex).getMessageArgs()[0];
            assertEquals(203, message.length()); // 200 + "..."
            assertEquals("...", message.substring(200));
        }

        @Test
        @DisplayName("JSON 中 message 字段为空 → 回退到原始 body")
        void shouldFallbackToRawBodyWhenMessageIsBlank() {
            String body = """
                    {"status":404,"message":"","errorCode":10001}
                    """.strip();
            Exception ex = decoder.decode(METHOD_KEY, buildResponse(404, body));

            assertInstanceOf(NotFoundException.class, ex);
            // message 为空，回退使用原始 body
            assertEquals(body, ((NotFoundException) ex).getMessageArgs()[0]);
        }

        @Test
        @DisplayName("JSON 中无 message 字段 → 回退到原始 body")
        void shouldFallbackToRawBodyWhenMessageFieldMissing() {
            String body = """
                    {"status":404,"errorCode":10001}
                    """.strip();
            Exception ex = decoder.decode(METHOD_KEY, buildResponse(404, body));

            assertInstanceOf(NotFoundException.class, ex);
            assertEquals(body, ((NotFoundException) ex).getMessageArgs()[0]);
        }
    }

    // ==================== 错误码验证 ====================

    @Nested
    @DisplayName("所有情况均使用 EXTERNAL_SERVICE_DETAIL 错误码")
    class ErrorCodeVerification {

        @Test
        @DisplayName("404 使用 EXTERNAL_SERVICE_DETAIL")
        void notFoundUsesDetailErrorCode() {
            Exception ex = decoder.decode(METHOD_KEY, buildResponse(404, "{}"));
            assertEquals(ExternalErrorCode.EXTERNAL_SERVICE_DETAIL,
                    ((NotFoundException) ex).getErrorCode());
        }

        @Test
        @DisplayName("500 使用 EXTERNAL_SERVICE_DETAIL")
        void serverErrorUsesDetailErrorCode() {
            Exception ex = decoder.decode(METHOD_KEY, buildResponse(500, "{}"));
            assertEquals(ExternalErrorCode.EXTERNAL_SERVICE_DETAIL,
                    ((ServiceException) ex).getErrorCode());
        }
    }
}
