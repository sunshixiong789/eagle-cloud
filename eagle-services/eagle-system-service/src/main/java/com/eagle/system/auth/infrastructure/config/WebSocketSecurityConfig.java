package com.eagle.system.auth.infrastructure.config;

import com.eagle.resource.server.config.EagleJwtAuthenticationConverter;
import com.eagle.system.auth.infrastructure.security.BlacklistAwareJwtDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket STOMP 鉴权配置。
 *
 * <p>SockJS HTTP 探针在 {@code SecurityConfig} 中已 permitAll，鉴权下沉到 STOMP 应用层：
 * STOMP {@code CONNECT} 帧到达时，从 {@code Authorization: Bearer ...} 解析 JWT，
 * 通过 {@link BlacklistAwareJwtDecoder} 验证（含黑名单），用 {@link EagleJwtAuthenticationConverter}
 * 转换为 {@code EagleAuthentication} 后 {@link StompHeaderAccessor#setUser} 绑定到会话——
 * 之后 {@code SimpMessagingTemplate#convertAndSendToUser} 才能按用户名定位订阅，
 * 业务 {@code @MessageMapping} 方法形参 {@code Principal} 也能拿到 {@code EagleUser}。
 *
 * <p>未带 token 或 token 无效一律抛 {@link AccessDeniedException}，STOMP 客户端收到 ERROR 帧后断开。
 *
 * @author 孙士雄
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

    private static final String BEARER_PREFIX = "Bearer ";

    private final BlacklistAwareJwtDecoder jwtDecoder;
    private final EagleJwtAuthenticationConverter jwtAuthenticationConverter;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new JwtAuthChannelInterceptor());
    }

    private class JwtAuthChannelInterceptor implements ChannelInterceptor {

        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor =
                    MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
                return message;
            }

            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
                throw new AccessDeniedException("Missing Authorization header in STOMP CONNECT");
            }

            String token = authHeader.substring(BEARER_PREFIX.length()).trim();
            try {
                Jwt jwt = jwtDecoder.decode(token);
                AbstractAuthenticationToken authentication = jwtAuthenticationConverter.convert(jwt);
                accessor.setUser(authentication);
                log.debug("[WebSocket] STOMP CONNECT authenticated: principal={}",
                        authentication != null ? authentication.getName() : null);
            } catch (JwtException e) {
                throw new AccessDeniedException("Invalid JWT in STOMP CONNECT: " + e.getMessage(), e);
            }
            return message;
        }
    }
}
