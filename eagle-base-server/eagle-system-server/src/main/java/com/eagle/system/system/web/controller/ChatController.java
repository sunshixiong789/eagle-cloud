package com.eagle.system.system.web.controller;

import com.eagle.common.exception.codes.OperationErrorCode;
import com.eagle.system.web.dto.ChatMessage;
import com.eagle.system.web.dto.PrivateMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

/**
 * WebSocket 消息控制器
 *
 * @author 孙士雄
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 广播消息
     */
    @MessageMapping("/broadcast-message")
    public void sendMessage(@Payload ChatMessage message, Principal principal) {
        if (message == null || message.getContent() == null || message.getContent().isBlank()) {
            throw OperationErrorCode.MESSAGE_REQUIRED.toDomainException();
        }

        log.info("用户 {} 发送广播消息: {}", principal != null ? principal.getName() : "匿名", message.getContent());
        messagingTemplate.convertAndSend("/topic/public", message);
    }

    /**
     * 私信消息
     */
    @MessageMapping("/message-to-one")
    public void sendPrivateMessage(@Payload PrivateMessage message, Principal principal) {
        if (message == null || message.getTo() == null || message.getTo().isBlank()) {
            throw OperationErrorCode.RECIPIENT_REQUIRED.toDomainException();
        }
        if (message.getContent() == null || message.getContent().isBlank()) {
            throw OperationErrorCode.MESSAGE_REQUIRED.toDomainException();
        }

        log.info("用户 {} 发送私信给 {}: {}",
                principal != null ? principal.getName() : "匿名",
                message.getTo(),
                message.getContent());

        messagingTemplate.convertAndSendToUser(
                message.getTo(),
                "/queue/private",
                message
        );
    }

    /**
     * WebSocket 连接建立事件
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        log.info("WebSocket 连接建立: sessionId={}", sessionId);
    }

    /**
     * WebSocket 连接断开事件
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        log.info("WebSocket 连接断开: sessionId={}", sessionId);
    }

    /**
     * 消息处理异常
     */
    @MessageExceptionHandler
    public void handleException(Exception e) {
        log.error("WebSocket 消息处理异常", e);
    }
}
