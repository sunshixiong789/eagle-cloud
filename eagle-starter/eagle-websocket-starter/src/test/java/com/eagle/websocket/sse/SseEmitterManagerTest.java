package com.eagle.websocket.sse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link SseEmitterManager}.
 *
 * <p>No external dependencies — {@link SseEmitterManager} is instantiated directly.
 */
class SseEmitterManagerTest {

    private SseEmitterManager manager;

    @BeforeEach
    void setUp() {
        manager = new SseEmitterManager();
    }

    @Nested
    @DisplayName("connect")
    class Connect {

        @Test
        @DisplayName("连接：应返回 Emitter")
        void connect_shouldReturnEmitter() {
            SseEmitter emitter = manager.connect("user1");

            assertNotNull(emitter);
        }

        @Test
        @DisplayName("连接：应支持单用户多个连接")
        void connect_shouldSupportMultipleConnectionsPerUser() {
            manager.connect("user1");
            manager.connect("user1");

            assertEquals(2, manager.getConnectionCount("user1"));
        }

        @Test
        @DisplayName("连接：应使用默认超时")
        void connect_shouldUsePDefaultTimeout() {
            SseEmitter emitter = manager.connect("user1");

            // Default timeout overload returns a non-null emitter
            assertNotNull(emitter);
        }

        @Test
        @DisplayName("连接：应使用指定超时")
        void connect_shouldUseProvidedTimeout() {
            SseEmitter emitter = manager.connect("user1", 30_000L);

            assertNotNull(emitter);
            assertEquals(1, manager.getConnectionCount("user1"));
        }
    }

    @Nested
    @DisplayName("disconnectUser")
    class DisconnectUser {

        @Test
        @DisplayName("断开连接：应移除用户的全部 Emitter")
        void disconnect_shouldRemoveAllUserEmitters() {
            manager.connect("user1");
            manager.connect("user1");
            assertEquals(2, manager.getConnectionCount("user1"));

            manager.disconnectUser("user1");

            assertEquals(0, manager.getConnectionCount("user1"));
        }

        @Test
        @DisplayName("断开连接：无连接时不应抛出")
        void disconnect_shouldNotThrowWhenNoConnections() {
            assertDoesNotThrow(() -> manager.disconnectUser("unknown-user"));
        }
    }

    @Nested
    @DisplayName("sendToUser")
    class SendToUser {

        @Test
        @DisplayName("发送到用户：离线用户应不执行操作")
        void sendToUser_shouldDoNothingForOfflineUser() {
            assertDoesNotThrow(() -> manager.sendToUser("offline-user", "order", "payload"));
        }

        @Test
        @DisplayName("发送到用户：应移除失效 Emitter")
        void sendToUser_shouldRemoveDeadEmitter() {
            // Complete the emitter before sending. In a unit test (no servlet handler),
            // complete() sets the flag but does NOT fire onCompletion. The removal
            // happens via the send-error path: send() on a completed emitter throws
            // IllegalStateException, which sendToUser catches and uses to removeEmitter.
            SseEmitter emitter = manager.connect("user1");
            emitter.complete();

            manager.sendToUser("user1", "order", "payload");

            assertEquals(0, manager.getConnectionCount("user1"));
        }
    }

    @Nested
    @DisplayName("broadcast")
    class Broadcast {

        @Test
        @DisplayName("广播：无连接时应不执行操作")
        void broadcast_shouldDoNothingWhenNoConnections() {
            assertDoesNotThrow(() -> manager.broadcast("announcement", "hello everyone"));
        }

        @Test
        @DisplayName("广播：应发送给全部已连接用户")
        void broadcast_shouldSendToAllConnectedUsers() {
            manager.connect("user1");
            manager.connect("user2");

            // Verify broadcast doesn't throw even with multiple users connected
            assertDoesNotThrow(() -> manager.broadcast("news", "some payload"));
        }
    }

    @Nested
    @DisplayName("getConnectionCount")
    class GetConnectionCount {

        @Test
        @DisplayName("获取Connection计数：应返回零针对未知用户")
        void getConnectionCount_shouldReturnZeroForUnknownUser() {
            assertEquals(0, manager.getConnectionCount("nobody"));
        }

        @Test
        @DisplayName("获取Connection计数：应追踪Accurately")
        void getConnectionCount_shouldTrackAccurately() {
            manager.connect("user1");
            assertEquals(1, manager.getConnectionCount("user1"));

            manager.connect("user1");
            assertEquals(2, manager.getConnectionCount("user1"));

            manager.disconnectUser("user1");
            assertEquals(0, manager.getConnectionCount("user1"));
        }
    }
}
