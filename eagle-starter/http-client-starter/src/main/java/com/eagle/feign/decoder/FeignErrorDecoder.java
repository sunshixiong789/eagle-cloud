package com.eagle.feign.decoder;

import com.eagle.common.dto.ErrorResult;
import com.eagle.common.exception.codes.ExternalErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Feign 错误解码器：将下游服务的 HTTP 错误转换为类型化异常。
 *
 * <p>解析下游返回的 {@link ErrorResult} JSON，提取 {@code message} 字段透传给调用方，
 * 使调用方能看到真实的业务错误原因，而非通用的"外部服务调用失败"。
 *
 * <p>解析失败（非 JSON 响应）时，回退到 HTTP 状态码描述，保证异常始终可读。
 *
 * @author 孙士雄
 */
@Slf4j
@RequiredArgsConstructor
public class FeignErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper;

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();
        String body = extractBody(response);

        log.error("Feign call failed: methodKey={}, status={}, body={}", methodKey, status, body);

        // 从下游响应中提取人类可读的 message，作为透传错误原因
        String downstreamMessage = parseMessage(body, status);

        return switch (status) {
            case 404 -> ExternalErrorCode.EXTERNAL_SERVICE_DETAIL
                    .toNotFoundException(downstreamMessage);
            case 409 -> ExternalErrorCode.EXTERNAL_SERVICE_DETAIL
                    .toConflictException(downstreamMessage);
            case 400 -> ExternalErrorCode.EXTERNAL_SERVICE_DETAIL
                    .toDomainException(downstreamMessage);
            case 403 -> ExternalErrorCode.EXTERNAL_SERVICE_DETAIL
                    .toServiceException(downstreamMessage);
            case 429 -> ExternalErrorCode.EXTERNAL_SERVICE_DETAIL
                    .toServiceException(downstreamMessage);
            default -> ExternalErrorCode.EXTERNAL_SERVICE_DETAIL
                    .toServiceException(downstreamMessage);
        };
    }

    /**
     * 从响应体提取 message 字段。
     *
     * <p>优先解析 {@link ErrorResult} JSON；解析失败时回退到原始 body；
     * body 为空时回退到 HTTP 状态码描述。
     */
    private String parseMessage(String body, int status) {
        if (body == null || body.isBlank()) {
            return "HTTP " + status;
        }
        try {
            ErrorResult errorResult = objectMapper.readValue(body, ErrorResult.class);
            if (errorResult.getMessage() != null && !errorResult.getMessage().isBlank()) {
                return errorResult.getMessage();
            }
        } catch (Exception ignored) {
            // 下游返回非 JSON 格式（如 HTML 错误页），直接使用原始 body
        }
        // body 存在但 message 为空，或非 JSON：截断到合理长度避免超长响应
        return body.length() > 200 ? body.substring(0, 200) + "..." : body;
    }

    /**
     * 读取响应体字符串（自动关闭流）。
     */
    private String extractBody(Response response) {
        if (response.body() == null) {
            return null;
        }
        try (Response.Body body = response.body()) {
            return new String(body.asInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Failed to read Feign error response body", e);
            return null;
        }
    }
}
