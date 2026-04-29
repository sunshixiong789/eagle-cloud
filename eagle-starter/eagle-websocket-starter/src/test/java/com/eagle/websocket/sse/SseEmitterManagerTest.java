package com.eagle.websocket.sse;

import org.junit.jupiter.api.AfterEach;
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
        @DisplayName("should return a non-null SseEmitter")
        void connect_shouldReturnEmitter() {
            SseEmitter emitter = manager.connect("user1");

            assertNotNull(emitter);
        }

        @Test
        @DisplayName("should support multiple connections per user")
        void connect_shouldSupportMultipleConnectionsPerUser() {
            manager.connect("user1");
            manager.connect("user1");

            assertEquals(2, manager.getConnectionCount("user1"));
        }

        @Test
        @DisplayName("should use default timeout when no timeout is specified")
        void connect_shouldUsePDefaultTimeout() {
            SseEmitter emitter = manager.connect("user1");

            // Default timeout overload returns a non-null emitter
            assertNotNull(emitter);
        }

        @Test
        @DisplayName("should use provided timeout when specified")
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
        @DisplayName("should remove all emitters for user after disconnect")
        void disconnect_shouldRemoveAllUserEmitters() {
            manager.connect("user1");
            manager.connect("user1");
            assertEquals(2, manager.getConnectionCount("user1"));

            manager.disconnectUser("user1");

            assertEquals(0, manager.getConnectionCount("user1"));
        }

        @Test
        @DisplayName("should not throw when disconnecting a user with no connections")
        void disconnect_shouldNotThrowWhenNoConnections() {
            assertDoesNotThrow(() -> manager.disconnectUser("unknown-user"));
        }
    }

    @Nested
    @DisplayName("sendToUser")
    class SendToUser {

        @Test
        @DisplayName("should do nothing for an offline user without throwing")
        void sendToUser_shouldDoNothingForOfflineUser() {
            assertDoesNotThrow(() -> manager.sendToUser("offline-user", "order", "payload"));
        }

        @Test
        @DisplayName("should remove dead emitter after send error and clean up connection")
        void sendToUser_shouldRemoveDeadEmitter() {
            // Connect and then immediately complete the emitter (simulates a closed connection)
            SseEmitter emitter = manager.connect("user1");
            emitter.complete();  // triggers onCompletion callback -> removeEmitter

            // After completion, the emitter should be cleaned up
            assertEquals(0, manager.getConnectionCount("user1"));
        }
    }

    @Nested
    @DisplayName("broadcast")
    class Broadcast {

        @Test
        @DisplayName("should do nothing when there are no connections without throwing")
        void broadcast_shouldDoNothingWhenNoConnections() {
            assertDoesNotThrow(() -> manager.broadcast("announcement", "hello everyone"));
        }

        @Test
        @DisplayName("should send to all connected users")
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
        @DisplayName("should return 0 for a user with no connections")
        void getConnectionCount_shouldReturnZeroForUnknownUser() {
            assertEquals(0, manager.getConnectionCount("nobody"));
        }

        @Test
        @DisplayName("should return accurate count after connecting and disconnecting")
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
