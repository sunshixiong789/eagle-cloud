package com.eagle.gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
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
 *
 * @author 孙士雄
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private final ReactiveJwtDecoder reactiveJwtDecoder;

    private static final String AUTHORIZATION_PREFIX = "Bearer ";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USERNAME_HEADER = "X-Username";
    private static final String ROLES_HEADER = "X-Roles";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final List<String> publicPaths = List.of(
            "/public/**",
            "/actuator/health",
            "/actuator/info",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**",
            "/oauth2/**",
            "/login",
            "/error"
    );

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

        return validateToken(token)
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
     * 校验 JWT Token。
     *
     * @param token JWT Token
     * @return Mono<Jwt>
     */
    private Mono<Jwt> validateToken(String token) {
        return reactiveJwtDecoder.decode(token);
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
    private ServerHttpRequest buildRequestWithUserInfo(ServerHttpRequest request, Jwt jwt) {
        Long userId = jwt.getClaimAsString("id") != null
                ? Long.valueOf(jwt.getClaimAsString("id"))
                : null;
        String username = jwt.getClaimAsString("userName");
        @SuppressWarnings("unchecked")
        List<String> roles = jwt.getClaimAsStringList("roles");

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
        return builder.build();
    }

    /**
     * 判断是否为公开路径。
     *
     * @param path 请求路径
     * @return true 表示无需鉴权
     */
    private boolean isPublicPath(String path) {
        return publicPaths.stream().anyMatch(p -> pathMatcher.match(p, path));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
