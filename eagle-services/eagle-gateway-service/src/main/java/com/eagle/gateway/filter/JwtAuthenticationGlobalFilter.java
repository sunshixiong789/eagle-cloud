package com.eagle.gateway.filter;

import com.eagle.gateway.config.GatewayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * JWT 鉴权全局过滤器。
 *
 * <p>校验请求中的 Authorization Header，解析 JWT Token 并将用户信息透传到下游。
 * 公开路径列表通过 {@code eagle.gateway.security.public-paths} 配置，支持外部化扩展。
 *
 * @author 孙士雄
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private static final String AUTHORIZATION_PREFIX = "Bearer ";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USERNAME_HEADER = "X-Username";
    private static final String ROLES_HEADER = "X-Roles";
    private static final String TENANT_ID_HEADER = "X-Tenant-Id";
    private final ReactiveJwtDecoder reactiveJwtDecoder;
    private final GatewayProperties gatewayProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            log.warn("Missing Authorization header, path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        return reactiveJwtDecoder.decode(token)
                .flatMap(jwt -> {
                    ServerHttpRequest mutatedRequest = buildRequestWithUserInfo(request, jwt);
                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                })
                .onErrorResume(e -> {
                    log.warn("JWT validation failed, path: {}, error: {}", path, e.getMessage());
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                });
    }

    /**
     * 从请求中提取 Token。
     *
     * @param request ServerHttpRequest
     * @return Token 字符串（不含 Bearer 前缀）
     */
    private String extractToken(ServerHttpRequest request) {
        List<String> headers = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (CollectionUtils.isEmpty(headers)) {
            return null;
        }
        String bearer = headers.get(0);
        if (StringUtils.hasText(bearer) && bearer.startsWith(AUTHORIZATION_PREFIX)) {
            return bearer.substring(AUTHORIZATION_PREFIX.length());
        }
        return null;
    }

    /**
     * 构建带用户信息的请求（透传给下游服务）。
     *
     * @param request 原始请求
     * @param jwt     解析后的 JWT
     * @return 新请求
     */
    private ServerHttpRequest buildRequestWithUserInfo(
            ServerHttpRequest request,
            org.springframework.security.oauth2.jwt.Jwt jwt) {
        Long userId = parseUserId(jwt.getClaimAsString("id"));
        String username = jwt.getClaimAsString("userName");
        @SuppressWarnings("unchecked")
        List<String> roles = jwt.getClaimAsStringList("roles");
        String tenantId = jwt.getClaimAsString("tenantId");

        ServerHttpRequest.Builder builder = request.mutate();
        if (userId != null) {
            builder.header(USER_ID_HEADER, String.valueOf(userId));
        }
        if (StringUtils.hasText(username)) {
            builder.header(USERNAME_HEADER, username);
        }
        if (!CollectionUtils.isEmpty(roles)) {
            builder.header(ROLES_HEADER, String.join(",", roles));
        }
        // 优先从 JWT claim 提取租户 ID，其次透传请求头（内网服务间调用场景）
        if (StringUtils.hasText(tenantId)) {
            builder.header(TENANT_ID_HEADER, tenantId);
        } else {
            String requestTenantId = request.getHeaders().getFirst(TENANT_ID_HEADER);
            if (StringUtils.hasText(requestTenantId)) {
                builder.header(TENANT_ID_HEADER, requestTenantId);
            }
        }
        return builder.build();
    }

    /**
     * 安全解析 JWT 中的用户 ID，避免格式非法时抛出 NumberFormatException。
     */
    private Long parseUserId(String userIdStr) {
        if (!StringUtils.hasText(userIdStr)) {
            return null;
        }
        try {
            return Long.valueOf(userIdStr);
        } catch (NumberFormatException e) {
            log.warn("Invalid user id claim in JWT: {}", userIdStr);
            return null;
        }
    }

    /**
     * 判断是否为公开路径（从配置读取，支持外部化扩展）。
     *
     * @param path 请求路径
     * @return true 表示无需鉴权
     */
    private boolean isPublicPath(String path) {
        return gatewayProperties.getSecurity().getPublicPaths()
                .stream()
                .anyMatch(p -> pathMatcher.match(p, path));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
