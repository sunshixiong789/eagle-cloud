package com.eagle.http.client.error;

import com.eagle.common.dto.ErrorResult;
import com.eagle.common.exception.codes.ExternalErrorCode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * RestClient 错误处理器：将下游服务 HTTP 错误转换为类型化业务异常。
 *
 * @author 孙士雄
 */
@Slf4j
@RequiredArgsConstructor
public class EagleResponseErrorHandler implements ResponseErrorHandler {

    private final ObjectMapper objectMapper;

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return response.getStatusCode().isError();
    }

    @Override
    public void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        String body = extractBody(response);
        String downstreamMessage = parseMessage(body, status);

        log.error("RestClient call failed: method={}, url={}, status={}, body={}",
                method, url, status, body);

        throw switch (status) {
            case 404 -> ExternalErrorCode.EXTERNAL_SERVICE_DETAIL
                    .toNotFoundException(downstreamMessage);
            case 409 -> ExternalErrorCode.EXTERNAL_SERVICE_DETAIL
                    .toConflictException(downstreamMessage);
            case 400 -> ExternalErrorCode.EXTERNAL_SERVICE_DETAIL
                    .toDomainException(downstreamMessage);
            default -> ExternalErrorCode.EXTERNAL_SERVICE_DETAIL
                    .toServiceException(downstreamMessage);
        };
    }

    /**
     * RestClient {@code defaultStatusHandler} 使用的错误判定。
     */
    public boolean isError(HttpStatusCode statusCode) {
        return statusCode.isError();
    }

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
            // 非项目标准 ErrorResult 响应时，直接透传原始响应体。
        }
        return body.length() > 200 ? body.substring(0, 200) + "..." : body;
    }

    private String extractBody(ClientHttpResponse response) throws IOException {
        return new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
    }
}
