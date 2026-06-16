package com.eagle.gateway.handler;

import com.eagle.common.dto.ErrorResult;
import com.eagle.gateway.filter.RequestEnrichmentGlobalFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * 网关全局异常处理器。
 *
 * <p>处理路由过程中的基础设施级异常（下游不可达、超时、连接中断等），
 * 统一返回与 {@link ErrorResult} 格式一致的 JSON 响应，替代 Spring Boot 默认的 Whitelabel Error Page。
 *
 * <p>{@code @Order(-2)} 优先于 Spring Boot 默认的 {@code DefaultErrorWebExceptionHandler(-1)} 执行。
 *
 * @author eagle
 */
@Slf4j
@Order(-2)
@Component
@RequiredArgsConstructor
public class GatewayWebExceptionHandler implements WebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public @NonNull Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        // 响应已提交（如流式响应中途断开），无法再写入，直接传播异常
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status = resolveStatus(ex);
        String path = exchange.getRequest().getURI().getPath();

        if (status.is5xxServerError()) {
            log.error("Gateway error [{} {}]: {}", status.value(), path, ex.getMessage(), ex);
        } else {
            log.warn("Gateway error [{} {}]: {}", status.value(), path, ex.getMessage());
        }

        ErrorResult body = ErrorResult.of(status, resolveMessage(status), path);
        // WebFlux 不使用 MDC,显式从 RequestEnrichmentGlobalFilter 写入的 attribute 取 requestId
        Object requestId = exchange.getAttribute(RequestEnrichmentGlobalFilter.REQUEST_ID_ATTRIBUTE);
        if (requestId instanceof String rid && !rid.isBlank()) {
            body.setRequestId(rid);
        }
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            bytes = "{}".getBytes(StandardCharsets.UTF_8);
        }

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * 根据异常类型推断 HTTP 状态码。
     *
     * <p>注意：{@code ConnectTimeoutException} 是 {@code ConnectException} 的子类，
     * 需先判断超时类异常，避免被父类 catch 覆盖。
     */
    private HttpStatus resolveStatus(Throwable ex) {
        // 504：各类超时（先于 ConnectException 判断，因为 ConnectTimeoutException extends ConnectException）
        if (ex instanceof TimeoutException
                || ex instanceof io.netty.channel.ConnectTimeoutException
                || ex instanceof io.netty.handler.timeout.ReadTimeoutException) {
            return HttpStatus.GATEWAY_TIMEOUT;
        }
        // 503：下游服务拒绝连接（服务未启动或不可达）
        if (ex instanceof ConnectException) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        // 502：下游服务异常中断（连接建立后提前关闭）
        if (ex instanceof reactor.netty.http.client.PrematureCloseException) {
            return HttpStatus.BAD_GATEWAY;
        }
        // 透传 ResponseStatusException 携带的状态码（如路由找不到 404）
        if (ex instanceof ResponseStatusException rse) {
            return HttpStatus.valueOf(rse.getStatusCode().value());
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /**
     * 根据状态码返回面向用户的中文消息。
     */
    private String resolveMessage(HttpStatus status) {
        return switch (status.value()) {
            case 502 -> "上游服务异常中断";
            case 503 -> "服务暂时不可用，请稍后重试";
            case 504 -> "上游服务响应超时";
            case 404 -> "请求的资源不存在";
            default -> "服务器内部错误";
        };
    }
}
