package com.eagle.example.integration.websocket;

import com.eagle.websocket.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

/**
 * WebSocket Starter 验证控制器。
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "eagle.websocket", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SampleWebSocketHandler {

    private final WebSocketSessionManager sessionManager;

    @MessageMapping("/sample/echo")
    @SendToUser("/topic/sample/echo")
    public String echo(@Payload String message) {
        log.info("WebSocket echo received: {}", message);
        return "Echo: " + message;
    }
}
