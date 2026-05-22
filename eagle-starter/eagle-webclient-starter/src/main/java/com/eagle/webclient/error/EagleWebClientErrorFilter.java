package com.eagle.webclient.error;

import com.eagle.common.dto.ErrorResult;
import com.eagle.common.exception.codes.ExternalErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

/**
 * WebClient 错误响应过滤器：把下游 4xx/5xx 响应转换为类型化的项目异常。
 *
 * <p>{@code eagle-restclient-starter} 的 {@code EagleResponseErrorHandler} 反应式等价物。
 *
 * @author 孙士雄
 */
@Slf4j
@RequiredArgsConstructor
public class EagleWebClientErrorFilter implements ExchangeFilterFunction {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        return next.exchange(request)
                .flatMap(response -> {
                    if (!response.statusCode().isError()) {
                        return Mono.just(response);
                    }
                    return response.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .flatMap(body -> {
                                int status = response.statusCode().value();
                                String message = parseMessage(body, status);
                                log.error("WebClient call failed: method={}, url={}, status={}, body={}",
                                        request.method(), request.url(), status, body);
                                return Mono.error(buildException(status, message));
                            });
                });
    }

    private RuntimeException buildException(int status, String message) {
        return switch (status) {
            case 404 -> ExternalErrorCode.EXTERNAL_SERVICE_DETAIL.toNotFoundException(message);
            case 409 -> ExternalErrorCode.EXTERNAL_SERVICE_DETAIL.toConflictException(message);
            case 400 -> ExternalErrorCode.EXTERNAL_SERVICE_DETAIL.toDomainException(message);
            default -> ExternalErrorCode.EXTERNAL_SERVICE_DETAIL.toServiceException(message);
        };
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
            // 非项目标准 ErrorResult 响应，直接透传原始响应体
        }
        return body.length() > 200 ? body.substring(0, 200) + "..." : body;
    }
}
