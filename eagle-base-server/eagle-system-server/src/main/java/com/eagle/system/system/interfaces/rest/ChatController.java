package com.eagle.system.system.interfaces.rest;

import com.eagle.eagle.system.interfaces.dto.PrivateMessage;
import com.eagle.eagle.system.interfaces.dto.response.ChatMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * 消息通知
 *
 * @author 孙士雄（sunshix@seeyon.com）
 * 2025/12/29-18:00
 */
@Controller("chat")
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 客户端发送到 /app/chat.sendMessage 的消息会进入此方法
     */
    @MessageMapping("broadcast-message")
    public void sendMessage(ChatMessage message) {
        // 广播给所有订阅 /topic/public 的客户端
        messagingTemplate.convertAndSend("/topic/public", message);
    }

    /**
     * 私信：发送给特定用户
     */
    @MessageMapping("message-to-one")
    public void sendPrivateMessage(PrivateMessage message) {
        // 自动拼接为 /user/{to}/queue/private
        messagingTemplate.convertAndSendToUser(
                message.getTo(),
                "/queue/private",
                message
        );
    }
}
