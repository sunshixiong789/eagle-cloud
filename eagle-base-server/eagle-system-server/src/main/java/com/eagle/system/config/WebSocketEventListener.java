package com.eagle.system.config;

import com.eagle.websocket.metrics.WebSocketMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.lang.Nullable;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
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
 * <p>将会话事件从 {@link com.eagle.system.base.web.controller.ChatController} 中剥离，
 * 遵循单一职责原则——消息控制器只处理业务消息，连接管理由本类负责。
 *
 * @author 孙士雄
 */
@Slf4j
@Component
public class WebSocketEventListener {

    /** 可选，未引入 Micrometer 时为 null */
    @Nullable
    private final WebSocketMetrics webSocketMetrics;

    @Autowired
    public WebSocketEventListener(@Nullable WebSocketMetrics webSocketMetrics) {
        this.webSocketMetrics = webSocketMetrics;
    }

    /**
     * 处理 STOMP 连接建立事件。
     *
     * @param event 连接事件
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
     *
     * @param event 断开事件
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
