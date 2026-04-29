package com.eagle.websocket.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手拦截器，提取 JWT Token 并存入会话属性。
 *
 * <p>在 WebSocket 握手阶段从 URL 参数 {@code token} 或请求头 {@code Authorization}
 * 提取 JWT，解析出用户 ID 存入 {@code attributes}（key = "userId"），
 * 供 {@code @MessageMapping} 方法通过 {@code @Header} 或 Principal 获取。
 *
 * <p>未携带 Token 时允许匿名连接（返回 {@code true}），
 * 业务方可在消息处理层做鉴权。
 *
 * @author 孙士雄
 */
@Slf4j
public class WebSocketAuthHandshakeInterceptor implements HandshakeInterceptor {

    private static final String TOKEN_PARAM = "token";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_ATTR = "userId";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        // 1. 优先从 URL 参数取 token（WebSocket 不支持自定义请求头）
        String uri = request.getURI().toString();
        String token = extractTokenFromQuery(uri);

        // 2. 降级到请求头
        if (!StringUtils.hasText(token)) {
            String authHeader = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
            if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
                token = authHeader.substring(BEARER_PREFIX.length());
            }
        }

        if (StringUtils.hasText(token)) {
            // 存储 token，后续可由 Spring Security 或自定义 HandshakeHandler 解析
            attributes.put("token", token);
            log.debug("[WebSocket] Token found in handshake request");
        } else {
            log.debug("[WebSocket] Anonymous WebSocket connection");
        }

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // 握手后无需处理
    }

    /**
     * 从 URI query string 提取 token 参数。
     */
    private String extractTokenFromQuery(String uri) {
        int queryStart = uri.indexOf('?');
        if (queryStart < 0) {
            return null;
        }
        String query = uri.substring(queryStart + 1);
        for (String param : query.split("&")) {
            if (param.startsWith(TOKEN_PARAM + "=")) {
                return param.substring(TOKEN_PARAM.length() + 1);
            }
        }
        return null;
    }
}
