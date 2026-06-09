package com.eagle.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * 网关层禁止外部访问 {@code /internal/**} 内部 API。
 *
 * <p><strong>背景</strong>:服务发现路由({@code gateway.discovery.locator.enabled=true})开启后,
 * 任何注册到 Nacos 的服务路径都可通过 {@code https://gateway/{service-id}/**} 被外部访问;
 * 业务方手写的 {@code @RequestMapping("/internal/**")} 端点(本意仅供同集群其他服务调用)
 * 会被一并暴露,产生敏感数据泄露风险。
 *
 * <p><strong>策略</strong>:任何包含 {@code /internal/} 段的 URI(无论位于路径前还是被服务名包裹)
 * 一律返回 403。服务间调用走 {@code lb://{service-id}/internal/...} 由 Spring Cloud LoadBalancer
 * 直接路由,不经网关,不受此过滤器影响。
 *
 * <p><strong>排序</strong>:order = HIGHEST_PRECEDENCE + 10,先于路由匹配,晚于
 * {@link RequestEnrichmentGlobalFilter}(让 requestId 注入完成,日志可关联)。
 *
 * <p><strong>纵深防御</strong>:这是第一层。第二层是各服务的 SecurityConfig
 * permitAll 范围只允许同集群网络可达的请求;第三层可加 mTLS / IP 白名单。
 *
 * @author sunshixiong
 */
@Slf4j
@Component
public class InternalPathBlockingGlobalFilter implements GlobalFilter, Ordered {

    /**
     * 路径中含本字符串(忽略大小写)即视为内部端点,拒绝外部访问。
     * 同时拦截:
     * <ul>
     *   <li>{@code /internal/online-users}</li>
     *   <li>{@code /auth/internal/online-users}(服务发现路由)</li>
     *   <li>{@code /system/internal/authorization/123}</li>
     * </ul>
     */
    private static final String INTERNAL_SEGMENT = "/internal/";

    private static final String FORBIDDEN_BODY =
            "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"internal API not exposed via gateway\"}";

    @Override
    public @NonNull Mono<Void> filter(ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        // 同时拿 decoded path 与 raw path:外部攻击可能用 %2Finternal%2F 形式编码,
        // 部分反代/Tomcat 配置不会自动 decode 到 path,需对原始字符串再做一次 URL 解码后比对。
        String path = request.getURI().getPath();
        String rawPath = request.getURI().getRawPath();

        if (containsInternalSegment(path) || containsInternalSegment(safeUrlDecode(rawPath))) {
            String clientIp = (String) exchange.getAttributes()
                    .getOrDefault(RequestEnrichmentGlobalFilter.CLIENT_IP_ATTRIBUTE, "unknown");
            log.warn("blocked external access to internal API: path={}, rawPath={}, clientIp={}, method={}",
                    path, rawPath, clientIp, request.getMethod());

            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.FORBIDDEN);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            DataBuffer body = response.bufferFactory()
                    .wrap(FORBIDDEN_BODY.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(body));
        }

        return chain.filter(exchange);
    }

    private boolean containsInternalSegment(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        // 同时覆盖 /internal/x、/{svc}/internal/x、大小写变体(/Internal/、/INTERNAL/ 等)
        String lower = path.toLowerCase();
        return lower.startsWith("/internal/")
                || lower.contains(INTERNAL_SEGMENT)
                || lower.endsWith("/internal");
    }

    /**
     * 对 raw path 做一次 URL 解码;失败(畸形输入)时返回原值,留给后续匹配链处理。
     * 不抛异常以避免攻击者通过畸形输入触发 500。
     *
     * <p>包级可见以便单测直接覆盖畸形输入分支
     * (Spring 上层 URI 解析会先拒绝畸形请求,本方法的 catch 通常是死代码,
     * 保留作为深度防御; 测试用 {@code safeUrlDecodeHandlesMalformedInput} 显式锁定行为)。
     */
    String safeUrlDecode(String rawPath) {
        if (rawPath == null || rawPath.isEmpty()) {
            return rawPath;
        }
        try {
            return URLDecoder.decode(rawPath, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            // 非法编码序列 — 拒绝即可(下方 containsInternalSegment 对原值 false 时不影响 chain)
            return rawPath;
        }
    }

    @Override
    public int getOrder() {
        // RequestEnrichmentGlobalFilter 是 HIGHEST_PRECEDENCE(用于注入 requestId / clientIp)
        // 本过滤器紧随其后,先于所有业务路由匹配
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
