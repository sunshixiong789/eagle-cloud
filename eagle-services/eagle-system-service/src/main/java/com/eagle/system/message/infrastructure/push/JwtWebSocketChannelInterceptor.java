package com.eagle.system.message.infrastructure.push;

import com.eagle.common.constant.SecurityConstants;
import com.eagle.resource.server.config.EagleJwtAuthenticationConverter;
import com.eagle.websocket.interceptor.WebSocketChannelInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * STOMP CONNECT 帧的 JWT 鉴权拦截器。
 *
 * <p>HTTP 层 {@code /ws-stomp/**} 被 {@code eagle.resource-server.permit-paths} 放行
 * (SockJS 探针无法注入 Authorization 头),真实身份校验在 STOMP CONNECT 帧上完成:
 * <ul>
 *   <li>读取 STOMP 原生 {@code Authorization: Bearer <jwt>} 头</li>
 *   <li>用 {@link JwtDecoder} 校验签名 / 过期 / issuer (由 starter 配置)</li>
 *   <li>用 {@link EagleJwtAuthenticationConverter} 转换为含 {@code EagleUser} 的 Authentication</li>
 *   <li>包装为 {@link WebSocketUserPrincipal} (getName 返回 userId 字符串),
 *       对齐业务侧 {@code sendToUser(userId, ...)} 的路由键</li>
 * </ul>
 *
 * <p>校验失败抛 {@link AuthenticationCredentialsNotFoundException} / {@link BadCredentialsException},
 * Spring STOMP 框架会向客户端发送 STOMP ERROR 帧并关闭 WebSocket 会话。
 */
@Slf4j
@Component
public class JwtWebSocketChannelInterceptor extends WebSocketChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtDecoder jwtDecoder;
    private final EagleJwtAuthenticationConverter converter;

    public JwtWebSocketChannelInterceptor(JwtDecoder jwtDecoder,
                                          EagleJwtAuthenticationConverter converter) {
        this.jwtDecoder = jwtDecoder;
        this.converter = converter;
    }

    @Override
    protected void onConnect(StompHeaderAccessor accessor, String token) {
        if (!StringUtils.hasText(token)) {
            log.warn("[WebSocket] STOMP CONNECT 缺少 Authorization 头, sessionId={}",
                    accessor.getSessionId());
            throw new AuthenticationCredentialsNotFoundException(
                    "STOMP CONNECT 缺少 Authorization 头");
        }

        String raw = token.startsWith(BEARER_PREFIX) ? token.substring(BEARER_PREFIX.length()) : token;

        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(raw);
        } catch (JwtException ex) {
            log.warn("[WebSocket] STOMP CONNECT JWT 校验失败, sessionId={}, reason={}",
                    accessor.getSessionId(), ex.getMessage());
            throw new BadCredentialsException("STOMP CONNECT JWT 校验失败", ex);
        }

        Long userId = jwt.getClaim(SecurityConstants.DETAILS_USER_ID);
        if (userId == null) {
            log.warn("[WebSocket] STOMP CONNECT JWT 缺少 {} claim, sessionId={}",
                    SecurityConstants.DETAILS_USER_ID, accessor.getSessionId());
            throw new BadCredentialsException(
                    "JWT 缺少 " + SecurityConstants.DETAILS_USER_ID + " claim");
        }

        AbstractAuthenticationToken auth = converter.convert(jwt);
        accessor.setUser(new WebSocketUserPrincipal(userId.toString(), auth));

        log.debug("[WebSocket] STOMP CONNECT 认证通过, sessionId={}, userId={}",
                accessor.getSessionId(), userId);
    }
}
