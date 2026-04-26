package com.eagle.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * 请求日志全局过滤器。
 *
 * <p>记录请求方法、路径、状态码、耗时。
 *
 * @author 孙士雄
 */
@Slf4j
@Component
public class RequestLoggingGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Instant start = Instant.now();
        ServerHttpRequest request = exchange.getRequest();

        String method = request.getMethod().name();
        String path = request.getURI().getPath();
        String clientIp = getClientIp(request);

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    ServerHttpResponse response = exchange.getResponse();
                    long millis = Duration.between(start, Instant.now()).toMillis();
                    log.info("[Gateway] {} {} {} {}ms from {}",
                            method, path,
                            response.getStatusCode() != null ? response.getStatusCode().value() : 0,
                            millis, clientIp);
                });
    }

    /**
     * 获取客户端真实 IP。
     *
     * @param request ServerHttpRequest
     * @return IP 地址
     */
    private String getClientIp(ServerHttpRequest request) {
        String ip = request.getHeaders().getFirst("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeaders().getFirst("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddress() != null
                    ? request.getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
        }
        return ip.split(",")[0].trim();
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 100;
    }
}
