package com.eagle.websocket.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

/**
 * WebSocket STOMP 消息通道拦截器。
 *
 * <p>功能：
 * <ol>
 *   <li>连接时（CONNECT 帧）提取 {@code Authorization} 头，调用 {@link #onConnect} 回调</li>
 *   <li>记录订阅和发送消息的 DEBUG 日志，便于排查消息流向问题</li>
 *   <li>支持子类扩展：覆盖 {@link #onConnect}、{@link #onSubscribe}、{@link #onSend}
 *       添加 token 验证、订阅权限校验、消息内容校验等自定义逻辑</li>
 * </ol>
 *
 * <p>注册方式：在 {@link org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer}
 * 的 {@code configureClientInboundChannel} 方法中注册此拦截器：
 * <pre>{@code
 * @Override
 * public void configureClientInboundChannel(ChannelRegistration registration) {
 *     registration.interceptors(webSocketChannelInterceptor);
 * }
 * }</pre>
 *
 * <p>子类扩展示例（token 验证）：
 * <pre>{@code
 * @Component
 * public class JwtWebSocketChannelInterceptor extends WebSocketChannelInterceptor {
 *
 *     private final JwtTokenService jwtTokenService;
 *
 *     public JwtWebSocketChannelInterceptor(JwtTokenService jwtTokenService) {
 *         this.jwtTokenService = jwtTokenService;
 *     }
 *
 *     @Override
 *     protected void onConnect(StompHeaderAccessor accessor, String token) {
 *         if (token != null) {
 *             String userId = jwtTokenService.extractUserId(token);
 *             accessor.setUser(() -> userId);
 *         }
 *     }
 * }
 * }</pre>
 *
 * @author eagle
 */
@Slf4j
public class WebSocketChannelInterceptor implements ChannelInterceptor {

    /**
     * 在消息发送到通道之前拦截，根据 STOMP 命令类型分发处理。
     *
     * @param message 待发送的消息
     * @param channel 目标消息通道
     * @return 处理后的消息，返回 {@code null} 表示丢弃该消息
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECT.equals(command)) {
            String token = accessor.getFirstNativeHeader("Authorization");
            onConnect(accessor, token);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            log.debug("[WebSocket] Subscribe: sessionId={}, destination={}",
                    accessor.getSessionId(), accessor.getDestination());
            onSubscribe(accessor);
        } else if (StompCommand.SEND.equals(command)) {
            log.debug("[WebSocket] Send: sessionId={}, destination={}",
                    accessor.getSessionId(), accessor.getDestination());
            onSend(accessor, message);
        }

        return message;
    }

    /**
     * 客户端发起 CONNECT 帧时的回调。
     *
     * <p>默认实现仅记录 DEBUG 日志，子类可覆盖此方法实现 token 验证、
     * 设置 {@link java.security.Principal} 等操作。
     *
     * @param accessor STOMP 头部访问器（可修改会话属性）
     * @param token    {@code Authorization} 头部值，未携带时为 {@code null}
     */
    protected void onConnect(StompHeaderAccessor accessor, String token) {
        log.debug("[WebSocket] Client connected: sessionId={}", accessor.getSessionId());
    }

    /**
     * 客户端发起 SUBSCRIBE 帧时的回调。
     *
     * <p>默认实现为空，子类可覆盖此方法实现订阅权限校验。
     * 若订阅不被允许，可抛出 {@link org.springframework.security.access.AccessDeniedException}。
     *
     * @param accessor STOMP 头部访问器（含目标地址和会话信息）
     */
    protected void onSubscribe(StompHeaderAccessor accessor) {
        // 默认无操作，子类按需覆盖
    }

    /**
     * 客户端发起 SEND 帧时的回调。
     *
     * <p>默认实现为空，子类可覆盖此方法实现消息内容校验或限流控制。
     *
     * @param accessor STOMP 头部访问器
     * @param message  原始消息（含消息体和头部）
     */
    protected void onSend(StompHeaderAccessor accessor, Message<?> message) {
        // 默认无操作，子类按需覆盖
    }
}
