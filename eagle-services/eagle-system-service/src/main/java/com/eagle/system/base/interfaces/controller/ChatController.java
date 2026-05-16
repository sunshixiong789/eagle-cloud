package com.eagle.system.base.interfaces.controller;

import com.eagle.common.exception.codes.OperationErrorCode;
import com.eagle.system.base.interfaces.dto.ChatMessage;
import com.eagle.system.base.interfaces.dto.PrivateMessage;
import com.eagle.websocket.listener.WebSocketEventListener;
import com.eagle.websocket.session.WebSocketSessionManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket 消息控制器。
 *
 * <p>处理客户端通过 STOMP 发送的消息（{@link MessageMapping}），
 * 推送逻辑委托给 {@link WebSocketSessionManager}。
 * 连接 / 断开事件由 {@link WebSocketEventListener} 统一处理。
 *
 * <p>注:STOMP/@MessageMapping 端点不会出现在 Swagger UI 中,完整 WebSocket 接口契约
 * 见 {@code docs/websocket-api.md} 与 {@code docs/websocket-api.yaml}(AsyncAPI 3.0)。
 *
 * @author 孙士雄
 */
@Slf4j
@Tag(name = "WebSocket 消息", description = "基于 STOMP over WebSocket 的实时消息;Swagger 不渲染 @MessageMapping,详见 docs/websocket-api.md")
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final WebSocketSessionManager webSocketSessionManager;

    /**
     * 广播消息：将消息推送到所有订阅了 {@code /topic/public} 的客户端。
     *
     * <p>客户端发送路径（appPrefix + mapping）：{@code /message/broadcast-message}
     *
     * @param message   消息内容
     * @param principal 当前用户身份（可为 null，表示匿名连接）
     */
    @Operation(summary = "发送广播消息",
            description = "客户端通过 STOMP SEND 到 /message/broadcast-message;服务端转发到 /topic/public,所有订阅者收到。content 必填,空白抛 OperationErrorCode.MESSAGE_REQUIRED(13005)。")
    @MessageMapping("/broadcast-message")
    public void sendMessage(@Payload ChatMessage message, Principal principal) {
        if (message == null || message.getContent() == null || message.getContent().isBlank()) {
            throw OperationErrorCode.MESSAGE_REQUIRED.toDomainException();
        }
        log.debug("用户 {} 发送广播消息", principal != null ? principal.getName() : "匿名");
        webSocketSessionManager.broadcast("/topic/public", message);
    }

    /**
     * 私信消息：将消息推送给指定用户。
     *
     * <p>客户端发送路径：{@code /message/message-to-one}；
     * 目标用户订阅 {@code /user/queue/private} 接收。
     *
     * @param message   私信内容（含 {@code to} 收件人字段）
     * @param principal 当前用户身份
     */
    @Operation(summary = "发送私信",
            description = "客户端 STOMP SEND 到 /message/message-to-one;服务端推送给指定用户的 /user/queue/private。to+content 必填,缺失分别抛 RECIPIENT_REQUIRED(13006)/MESSAGE_REQUIRED(13005)。")
    @MessageMapping("/message-to-one")
    public void sendPrivateMessage(@Payload PrivateMessage message, Principal principal) {
        if (message == null || message.getTo() == null || message.getTo().isBlank()) {
            throw OperationErrorCode.RECIPIENT_REQUIRED.toDomainException();
        }
        if (message.getContent() == null || message.getContent().isBlank()) {
            throw OperationErrorCode.MESSAGE_REQUIRED.toDomainException();
        }
        log.debug("用户 {} 发送私信给 {}", principal != null ? principal.getName() : "匿名", message.getTo());
        webSocketSessionManager.sendToUser(message.getTo(), "/queue/private", message);
    }

    /**
     * 统一处理 {@link MessageMapping} 方法抛出的异常。
     *
     * @param e 异常
     */
    @MessageExceptionHandler
    public void handleException(Exception e) {
        log.error("WebSocket 消息处理异常", e);
    }
}
