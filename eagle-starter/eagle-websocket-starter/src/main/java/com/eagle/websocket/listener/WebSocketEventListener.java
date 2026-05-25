package com.eagle.websocket.listener;

import com.eagle.websocket.metrics.WebSocketMetrics;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * WebSocket 会话生命周期事件监听器。
 *
 * <p>监听 STOMP 连接 / 断开事件，负责：
 * <ul>
 *   <li>记录连接日志，便于排查握手问题</li>
 *   <li>驱动 {@link WebSocketMetrics} 更新在线连接数指标（可选，Micrometer 不存在时自动跳过）</li>
 * </ul>
 *
 * <p>由 {@code eagle-websocket-starter} 自动注册，消费方无需手动声明。
 *
 * @author eagle
 */
@Slf4j
public class WebSocketEventListener {

    @Nullable
    private final WebSocketMetrics webSocketMetrics;

    public WebSocketEventListener(@Nullable WebSocketMetrics webSocketMetrics) {
        this.webSocketMetrics = webSocketMetrics;
    }

    /**
     * 处理 STOMP 连接建立事件。
     */
    @EventListener
    public void onSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        log.debug("[WebSocket] 连接建立: sessionId={}", accessor.getSessionId());
        if (webSocketMetrics != null) {
            webSocketMetrics.onConnect();
        }
    }

    /**
     * 处理 STOMP 连接断开事件。
     */
    @EventListener
    public void onSessionDisconnected(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        log.debug("[WebSocket] 连接断开: sessionId={}, reason={}",
                accessor.getSessionId(), event.getCloseStatus());
        if (webSocketMetrics != null) {
            webSocketMetrics.onDisconnect();
        }
    }
}