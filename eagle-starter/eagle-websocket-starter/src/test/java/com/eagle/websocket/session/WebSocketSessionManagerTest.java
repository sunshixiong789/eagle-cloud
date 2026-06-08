package com.eagle.websocket.session;

import com.eagle.websocket.metrics.WebSocketMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for {@link WebSocketSessionManager}.
 */
@ExtendWith(MockitoExtension.class)
class WebSocketSessionManagerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private WebSocketMetrics metrics;

    @Nested
    @DisplayName("sendToUser")
    class SendToUser {

        @Test
        @DisplayName("发送到用户：应委托到模板")
        void sendToUser_shouldDelegateToTemplate() {
            WebSocketSessionManager manager = new WebSocketSessionManager(messagingTemplate);
            String userId = "user1";
            String destination = "/queue/order-status";
            Object payload = "test-payload";

            manager.sendToUser(userId, destination, payload);

            verify(messagingTemplate).convertAndSendToUser(userId, destination, payload);
        }

        @Test
        @DisplayName("发送到用户：异常时不应抛出")
        void sendToUser_shouldNotThrowOnException() {
            WebSocketSessionManager manager = new WebSocketSessionManager(messagingTemplate);
            doThrow(new RuntimeException("connection lost"))
                    .when(messagingTemplate).convertAndSendToUser("user1", "/queue/x", "data");

            assertDoesNotThrow(() -> manager.sendToUser("user1", "/queue/x", "data"));
        }

        @Test
        @DisplayName("发送到用户：成功时应递增指标")
        void sendToUser_shouldIncrementMetricsOnSuccess() {
            WebSocketSessionManager manager = new WebSocketSessionManager(messagingTemplate, metrics);

            manager.sendToUser("user1", "/queue/order-status", "payload");

            verify(metrics).onMessageSent();
        }

        @Test
        @DisplayName("发送到用户：异常时不应调用指标")
        void sendToUser_shouldNotCallMetricsOnException() {
            WebSocketSessionManager manager = new WebSocketSessionManager(messagingTemplate, metrics);
            doThrow(new RuntimeException("send failed"))
                    .when(messagingTemplate).convertAndSendToUser("user1", "/queue/x", "data");

            assertDoesNotThrow(() -> manager.sendToUser("user1", "/queue/x", "data"));

            verifyNoInteractions(metrics);
        }
    }

    @Nested
    @DisplayName("broadcast")
    class Broadcast {

        @Test
        @DisplayName("广播：应委托到模板")
        void broadcast_shouldDelegateToTemplate() {
            WebSocketSessionManager manager = new WebSocketSessionManager(messagingTemplate);
            String destination = "/topic/announcement";
            Object payload = "broadcast-msg";

            manager.broadcast(destination, payload);

            verify(messagingTemplate).convertAndSend(destination, payload);
        }

        @Test
        @DisplayName("广播：异常时不应抛出")
        void broadcast_shouldNotThrowOnException() {
            WebSocketSessionManager manager = new WebSocketSessionManager(messagingTemplate);
            doThrow(new RuntimeException("broker down"))
                    .when(messagingTemplate).convertAndSend("/topic/x", "data");

            assertDoesNotThrow(() -> manager.broadcast("/topic/x", "data"));
        }
    }
}
