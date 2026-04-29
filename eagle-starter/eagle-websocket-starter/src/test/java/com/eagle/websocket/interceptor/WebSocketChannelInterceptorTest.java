package com.eagle.websocket.interceptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link WebSocketChannelInterceptor}.
 */
class WebSocketChannelInterceptorTest {

    private MessageChannel channel;

    @BeforeEach
    void setUp() {
        channel = mock(MessageChannel.class);
    }

    /**
     * Builds a STOMP {@link Message} for a given command.
     */
    private Message<byte[]> buildStompMessage(StompCommand command) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    /**
     * Builds a STOMP CONNECT message with the given Authorization header value.
     */
    private Message<byte[]> buildConnectMessage(String authorizationValue) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authorizationValue != null) {
            accessor.addNativeHeader("Authorization", authorizationValue);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Nested
    @DisplayName("preSend — CONNECT frame")
    class ConnectFrame {

        @Test
        @DisplayName("should invoke onConnect callback with extracted token for CONNECT frame")
        void preSend_shouldCallOnConnectForConnectFrame() {
            AtomicReference<String> capturedToken = new AtomicReference<>();

            WebSocketChannelInterceptor interceptor = new WebSocketChannelInterceptor() {
                @Override
                protected void onConnect(StompHeaderAccessor accessor, String token) {
                    capturedToken.set(token);
                }
            };

            Message<byte[]> message = buildConnectMessage("my-jwt-token");
            Message<?> result = interceptor.preSend(message, channel);

            assertNotNull(result);
            assertEquals("my-jwt-token", capturedToken.get());
        }

        @Test
        @DisplayName("should pass null token to onConnect when Authorization header is absent")
        void preSend_shouldPassNullTokenWhenNoAuthHeader() {
            AtomicReference<String> capturedToken = new AtomicReference<>("sentinel");

            WebSocketChannelInterceptor interceptor = new WebSocketChannelInterceptor() {
                @Override
                protected void onConnect(StompHeaderAccessor accessor, String token) {
                    capturedToken.set(token);
                }
            };

            Message<byte[]> message = buildConnectMessage(null);
            interceptor.preSend(message, channel);

            // token should be null when the header is absent
            assertEquals(null, capturedToken.get());
        }
    }

    @Nested
    @DisplayName("preSend — non-CONNECT frames")
    class NonConnectFrames {

        @Test
        @DisplayName("should return original message unchanged for SUBSCRIBE frame")
        void preSend_shouldReturnMessageUnchangedForSubscribe() {
            WebSocketChannelInterceptor interceptor = new WebSocketChannelInterceptor();
            Message<byte[]> message = buildStompMessage(StompCommand.SUBSCRIBE);

            Message<?> result = interceptor.preSend(message, channel);

            assertSame(message, result);
        }

        @Test
        @DisplayName("should return original message unchanged for SEND frame")
        void preSend_shouldReturnMessageUnchangedForSend() {
            WebSocketChannelInterceptor interceptor = new WebSocketChannelInterceptor();
            Message<byte[]> message = buildStompMessage(StompCommand.SEND);

            Message<?> result = interceptor.preSend(message, channel);

            assertSame(message, result);
        }

        @Test
        @DisplayName("should return original message unchanged for DISCONNECT frame")
        void preSend_shouldReturnMessageUnchangedForDisconnect() {
            WebSocketChannelInterceptor interceptor = new WebSocketChannelInterceptor();
            Message<byte[]> message = buildStompMessage(StompCommand.DISCONNECT);

            Message<?> result = interceptor.preSend(message, channel);

            assertSame(message, result);
        }
    }

    @Nested
    @DisplayName("preSend — null accessor guard")
    class NullAccessorGuard {

        @Test
        @DisplayName("should return original message without throwing when message has no StompHeaderAccessor")
        void preSend_shouldHandleNullAccessor() {
            WebSocketChannelInterceptor interceptor = new WebSocketChannelInterceptor();
            // Build a plain message — no StompHeaderAccessor attached
            Message<byte[]> plainMessage = MessageBuilder.withPayload(new byte[0]).build();

            assertDoesNotThrow(() -> {
                Message<?> result = interceptor.preSend(plainMessage, channel);
                assertSame(plainMessage, result);
            });
        }
    }
}
