package com.eagle.websocket.session;

import com.eagle.websocket.metrics.WebSocketMetrics;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * WebSocket 消息推送管理器。
 *
 * <p>封装 {@link SimpMessagingTemplate}，提供语义化的推送 API：
 * <ul>
 *   <li>{@link #sendToUser} — 点对点推送，仅指定用户收到</li>
 *   <li>{@link #broadcast} — 广播推送，所有订阅该 topic 的客户端收到</li>
 * </ul>
 *
 * <p>若上下文中存在 {@link WebSocketMetrics} Bean，则每次成功推送后自动上报指标；
 * 未引入 Micrometer 依赖时可传入 {@code null} 禁用指标收集，不影响正常推送功能。
 *
 * <p>使用示例：
 * <pre>{@code
 * // 推送订单状态变更给指定用户
 * sessionManager.sendToUser(userId.toString(), "/queue/order-status", orderStatusDto);
 *
 * // 广播系统公告
 * sessionManager.broadcast("/topic/announcement", announcementDto);
 * }</pre>
 *
 * @author eagle
 */
@Slf4j
public class WebSocketSessionManager {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 可选指标收集器，为 null 时跳过指标上报
     */
    @Nullable
    private final WebSocketMetrics metrics;

    /**
     * 构造消息推送管理器（不带指标收集）。
     *
     * @param messagingTemplate Spring STOMP 消息模板
     */
    public WebSocketSessionManager(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.metrics = null;
    }

    /**
     * 构造消息推送管理器（带可选指标收集）。
     *
     * @param messagingTemplate Spring STOMP 消息模板
     * @param metrics           指标收集器，传入 {@code null} 表示禁用指标
     */
    public WebSocketSessionManager(SimpMessagingTemplate messagingTemplate,
                                   @Nullable WebSocketMetrics metrics) {
        this.messagingTemplate = messagingTemplate;
        this.metrics = metrics;
    }

    /**
     * 向指定用户推送点对点消息。
     *
     * <p>客户端需订阅 {@code /user/{userId}/queue/{destination}} 接收。
     * 推送成功后若配置了 {@link WebSocketMetrics}，自动上报消息发送指标。
     *
     * @param userId      目标用户 ID（字符串形式）
     * @param destination 消息目标（如 "/queue/order-status"）
     * @param payload     消息体（自动序列化为 JSON）
     */
    public void sendToUser(String userId, String destination, Object payload) {
        try {
            messagingTemplate.convertAndSendToUser(userId, destination, payload);
            log.debug("[WebSocket] Sent to user {}, destination: {}", userId, destination);
            if (metrics != null) {
                metrics.onMessageSent();
            }
        } catch (Exception e) {
            log.warn("[WebSocket] Failed to send message to user {}, destination: {}: {}",
                    userId, destination, e.getMessage());
        }
    }

    /**
     * 广播消息到指定 Topic，所有订阅该 Topic 的客户端均可收到。
     *
     * <p>推送成功后若配置了 {@link WebSocketMetrics}，自动上报消息发送指标。
     *
     * @param destination Topic 路径（如 "/topic/announcement"），不含前缀
     * @param payload     消息体
     */
    public void broadcast(String destination, Object payload) {
        try {
            messagingTemplate.convertAndSend(destination, payload);
            log.debug("[WebSocket] Broadcast to {}", destination);
            if (metrics != null) {
                metrics.onMessageSent();
            }
        } catch (Exception e) {
            log.warn("[WebSocket] Failed to broadcast to {}: {}", destination, e.getMessage());
        }
    }
}
