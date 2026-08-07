package com.eagle.gateway.handler;

import com.eagle.common.dto.ErrorResult;
import com.eagle.gateway.filter.RequestEnrichmentGlobalFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
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
 * <p><strong>只处理路由过程中的基础设施级异常</strong>（下游不可达、超时、连接中断），
 * 统一返回与 {@link ErrorResult} 格式一致的 JSON 响应。识别不了的异常一律回抛，
 * 交给 common-starter 的 {@code ReactiveGlobalExceptionHandler}
 * （{@code HIGHEST_PRECEDENCE + 10}）按业务语义处理——未匹配路由的 404 就走那条路径。
 *
 * <p>{@code @Order(HIGHEST_PRECEDENCE)} 让本处理器排在 {@code ReactiveGlobalExceptionHandler} 之前。
 * 历史上本类为 {@code @Order(-2)}，被后者的 {@code HIGHEST_PRECEDENCE} 完全遮蔽，
 * 下面的 502 / 503 / 504 映射从未生效，下游故障一律呈现为 500。
 *
 * @author eagle
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
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
        if (status == null) {
            // 非基础设施异常：回抛给 ReactiveGlobalExceptionHandler
            return Mono.error(ex);
        }
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
     * 根据异常类型推断 HTTP 状态码，本处理器不负责的异常返回 {@code null}（由调用方回抛）。
     *
     * <p>注意：{@code ConnectTimeoutException} 是 {@code ConnectException} 的子类，
     * 需先判断超时类异常，避免被父类 catch 覆盖。
     */
    private @Nullable HttpStatus resolveStatus(Throwable ex) {
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
        // 其余（含 ResponseStatusException / AppException / 未匹配路由的 404）交给
        // ReactiveGlobalExceptionHandler，避免在网关重复一套业务语义映射
        return null;
    }

    /**
     * 根据状态码返回面向用户的中文消息。
     */
    private String resolveMessage(HttpStatus status) {
        return switch (status.value()) {
            case 502 -> "上游服务异常中断";
            case 503 -> "服务暂时不可用，请稍后重试";
            case 504 -> "上游服务响应超时";
            default -> "服务器内部错误";
        };
    }
}
