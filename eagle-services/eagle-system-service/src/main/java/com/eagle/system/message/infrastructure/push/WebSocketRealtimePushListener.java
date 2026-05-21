package com.eagle.system.message.infrastructure.push;

import com.eagle.system.message.domain.model.UserMessageCreatedEvent;
import com.eagle.websocket.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * 消息落库后通过 WebSocket 实时推送给在线用户。
 *
 * <p>触发时机：{@link UserMessageCreatedEvent} 在事务提交后异步处理，
 * 确保前端能立即看到列表中已存在该消息（避免推送早于落库）。
 *
 * <p>用户离线时推送无效——下次登录拉历史可见，由前端轮询/订阅自行兜底。
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketRealtimePushListener {

    /** 客户端订阅路径：{@code /user/queue/messages}（Spring 自动加 {@code /user/{userId}/} 前缀）。 */
    private static final String DESTINATION = "/queue/messages";

    private final WebSocketSessionManager webSocketSessionManager;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @EventListener
    public void onMessageCreated(UserMessageCreatedEvent event) {
        try {
            Map<String, Object> payload = Map.of(
                    "id", event.getMessageId(),
                    "category", event.getCategory().name(),
                    "title", event.getTitle(),
                    "content", event.getContent()
            );
            webSocketSessionManager.sendToUser(String.valueOf(event.getUserId()), DESTINATION, payload);
            log.debug("user-message pushed via WebSocket: id={}, userId={}",
                    event.getMessageId(), event.getUserId());
        } catch (Exception e) {
            // 推送失败不影响业务——用户下次拉列表能看到历史
            log.warn("user-message WebSocket push failed: id={}, userId={}",
                    event.getMessageId(), event.getUserId(), e);
        }
    }
}
