package com.eagle.feign.decoder;

import com.eagle.common.exception.codes.ExternalErrorCode;
import com.eagle.common.exception.DomainException;
import com.eagle.common.exception.NotFoundException;
import com.eagle.common.exception.ServiceException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Feign 错误解码器：将下游服务的 HTTP 错误转换为类型化异常。
 *
 * @author 孙士雄
 */
@Slf4j
public class FeignErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        String body = extractBody(response);
        int status = response.status();

        log.error("Feign call failed, methodKey: {}, status: {}, body: {}",
                methodKey, status, body);

        return switch (status) {
            case 404 -> ExternalErrorCode.EXTERNAL_SERVICE_ERROR
                    .toNotFoundException("下游服务资源不存在: " + methodKey + ", " + body);
            case 409 -> ExternalErrorCode.EXTERNAL_SERVICE_ERROR
                    .toDomainException("下游服务资源冲突: " + methodKey + ", " + body);
            case 400 -> ExternalErrorCode.EXTERNAL_SERVICE_ERROR
                    .toDomainException("下游服务参数错误: " + methodKey + ", " + body);
            case 403 -> ExternalErrorCode.EXTERNAL_SERVICE_ERROR
                    .toServiceException("下游服务无权限: " + methodKey);
            case 429 -> ExternalErrorCode.EXTERNAL_SERVICE_ERROR
                    .toServiceException("下游服务限流: " + methodKey);
            default -> ExternalErrorCode.EXTERNAL_SERVICE_ERROR
                    .toServiceException("下游服务调用失败: " + methodKey + ", status=" + status + ", body=" + body);
        };
    }

    /**
     * 提取响应体内容。
     *
     * @param response Feign Response
     * @return 响应体字符串
     */
    private String extractBody(Response response) {
        if (response.body() == null) {
            return "";
        }
        try (Response.Body body = response.body()) {
            return new String(body.asInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Failed to read Feign error response body", e);
            return "";
        }
    }
}
