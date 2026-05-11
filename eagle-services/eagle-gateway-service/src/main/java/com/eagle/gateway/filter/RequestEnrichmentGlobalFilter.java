package com.eagle.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 请求增强全局过滤器：注入 Request ID + 透传真实客户端 IP。
 *
 * <p>order = HIGHEST_PRECEDENCE —— 必须先于所有业务过滤器执行,确保后续过滤器、下游服务、
 * 全局异常处理器都能拿到 requestId 与真实客户端 IP（X-Real-IP / X-Forwarded-For）。
 *
 * <p>requestId 优先级：上游已带 X-Request-Id > 网关生成 UUID。
 * 真实 IP 优先级：上游已带 X-Real-IP > X-Forwarded-For 首段 > remoteAddress。
 *
 * @author 孙士雄
 */
@Slf4j
@Component
public class RequestEnrichmentGlobalFilter implements GlobalFilter, Ordered {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REAL_IP_HEADER = "X-Real-IP";
    public static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
    public static final String REQUEST_ID_ATTRIBUTE = "eagle.requestId";
    public static final String CLIENT_IP_ATTRIBUTE = "eagle.clientIp";

    @Override
    public @NonNull Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String requestId = resolveRequestId(request);
        String clientIp = resolveClientIp(request);

        ServerHttpRequest mutatedRequest = request.mutate()
                .header(REQUEST_ID_HEADER, requestId)
                .header(REAL_IP_HEADER, clientIp)
                .build();

        // 写入 exchange attribute,供后续 filter 与 ErrorWebExceptionHandler 复用,避免重复解析
        exchange.getAttributes().put(REQUEST_ID_ATTRIBUTE, requestId);
        exchange.getAttributes().put(CLIENT_IP_ATTRIBUTE, clientIp);

        // 响应提交前注入 X-Request-Id（set 覆盖语义）：
        // SCG 默认透传上游响应头,若上游 system / tracing-starter 也注入了 X-Request-Id,
        // 直接 add 会导致响应头出现多个值;set 在 beforeCommit 阶段执行,保证最终唯一。
        exchange.getResponse().beforeCommit(() -> {
            exchange.getResponse().getHeaders().set(REQUEST_ID_HEADER, requestId);
            return Mono.empty();
        });

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private String resolveRequestId(ServerHttpRequest request) {
        String upstream = request.getHeaders().getFirst(REQUEST_ID_HEADER);
        if (upstream != null && !upstream.isBlank()) {
            return upstream;
        }
        return UUID.randomUUID().toString();
    }

    private String resolveClientIp(ServerHttpRequest request) {
        String upstreamRealIp = request.getHeaders().getFirst(REAL_IP_HEADER);
        if (upstreamRealIp != null && !upstreamRealIp.isBlank()) {
            return upstreamRealIp;
        }
        String xff = request.getHeaders().getFirst(FORWARDED_FOR_HEADER);
        if (xff != null && !xff.isBlank() && !"unknown".equalsIgnoreCase(xff)) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
