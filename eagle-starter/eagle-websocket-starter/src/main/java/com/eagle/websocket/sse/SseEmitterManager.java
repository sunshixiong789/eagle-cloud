package com.eagle.websocket.sse;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE（Server-Sent Events）推送管理器。
 *
 * <p>适用于不需要双向通信、只需服务端主动推送的场景（如订单状态通知、进度推送）。
 * 相比 WebSocket 更轻量，客户端实现更简单（原生浏览器支持，无需额外库）。
 *
 * <p>每个用户可建立多个 SSE 连接（多标签页），通过 {@link CopyOnWriteArrayList} 管理。
 *
 * <p>使用示例（Controller）：
 * <pre>{@code
 * @GetMapping(value = "/sse/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
 * public SseEmitter subscribe(@PathVariable String userId) {
 *     return sseEmitterManager.connect(userId, 60_000L);
 * }
 * }</pre>
 *
 * @author eagle
 */
@Slf4j
@RequiredArgsConstructor
public class SseEmitterManager {

    private static final long DEFAULT_TIMEOUT_MS = 60_000L;

    /**
     * userId → 该用户的所有 SSE 连接列表
     */
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> emitters =
            new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    /**
     * 为指定用户建立 SSE 连接。
     *
     * <p>连接超时或发生错误时，自动从管理器中移除，避免内存泄漏。
     *
     * @param userId    用户 ID
     * @param timeoutMs 连接超时时长（毫秒），超时后客户端会自动重连
     * @return {@link SseEmitter} 实例，由 Spring 框架管理连接生命周期
     */
    public SseEmitter connect(String userId, long timeoutMs) {
        SseEmitter emitter = new SseEmitter(timeoutMs);
        CopyOnWriteArrayList<SseEmitter> userEmitters =
                emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
        userEmitters.add(emitter);

        // 连接完成/超时/错误时自动清理
        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(e -> {
            log.debug("[SSE] Connection error for user {}: {}", userId, e.getMessage());
            removeEmitter(userId, emitter);
        });

        log.debug("[SSE] New connection for user {}, total connections: {}",
                userId, userEmitters.size());
        return emitter;
    }

    /**
     * 建立默认超时（60秒）的 SSE 连接。
     *
     * @param userId 用户 ID
     * @return {@link SseEmitter} 实例
     */
    public SseEmitter connect(String userId) {
        return connect(userId, DEFAULT_TIMEOUT_MS);
    }

    /**
     * 向指定用户的所有 SSE 连接推送命名事件。
     *
     * <p>如果某条连接推送失败，将其移除（避免积累失效连接）并继续推送其余连接。
     *
     * @param userId  目标用户 ID
     * @param event   事件名称（客户端通过 {@code eventSource.addEventListener(event, ...)} 监听）
     * @param payload 消息体（自动序列化为 JSON）
     */
    public void sendToUser(String userId, String event, Object payload) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            log.debug("[SSE] No active connections for user {}", userId);
            return;
        }

        String json = serialize(payload);
        SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
                .name(event)
                .data(json);

        userEmitters.forEach(emitter -> {
            try {
                emitter.send(eventBuilder);
            } catch (Exception e) {
                // IOException: network error; IllegalStateException: emitter already completed
                log.debug("[SSE] Failed to send to user {}, removing connection: {}", userId, e.getMessage());
                removeEmitter(userId, emitter);
            }
        });
    }

    /**
     * 向所有在线用户广播命名事件。
     *
     * @param event   事件名称
     * @param payload 消息体
     */
    public void broadcast(String event, Object payload) {
        String json = serialize(payload);
        SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
                .name(event)
                .data(json);

        emitters.forEach((userId, userEmitters) ->
                userEmitters.forEach(emitter -> {
                    try {
                        emitter.send(eventBuilder);
                    } catch (Exception e) {
                        log.debug("[SSE] Broadcast failed for user {}, removing: {}", userId, e.getMessage());
                        removeEmitter(userId, emitter);
                    }
                })
        );
        log.debug("[SSE] Broadcast event '{}' to {} users", event, emitters.size());
    }

    /**
     * 查询指定用户的在线连接数。
     *
     * @param userId 用户 ID
     * @return 连接数，用户不在线返回 0
     */
    public int getConnectionCount(String userId) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        return userEmitters == null ? 0 : userEmitters.size();
    }

    /**
     * 断开并移除指定用户的所有连接（如账号被踢下线）。
     *
     * @param userId 用户 ID
     */
    public void disconnectUser(String userId) {
        List<SseEmitter> userEmitters = emitters.remove(userId);
        if (userEmitters != null) {
            userEmitters.forEach(SseEmitter::complete);
            log.info("[SSE] Disconnected all connections for user {}", userId);
        }
    }

    private void removeEmitter(String userId, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
            if (userEmitters.isEmpty()) {
                emitters.remove(userId);
            }
        }
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JacksonException e) {
            log.warn("[SSE] Failed to serialize payload: {}", e.getMessage());
            return "{}";
        }
    }
}
