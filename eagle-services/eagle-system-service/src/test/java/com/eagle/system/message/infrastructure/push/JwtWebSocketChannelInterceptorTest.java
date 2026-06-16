package com.eagle.system.message.infrastructure.push;

import com.eagle.resource.server.config.EagleJwtAuthenticationConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtWebSocketChannelInterceptor")
class JwtWebSocketChannelInterceptorTest {

    @Mock
    JwtDecoder jwtDecoder;

    @Mock
    EagleJwtAuthenticationConverter converter;

    @Mock
    MessageChannel channel;

    @Nested
    @DisplayName("CONNECT")
    class Connect {

        @Test
        @DisplayName("JWT 过期时应抛出明确过期异常并跳过认证转换")
        void shouldThrowExpiredMessageWhenJwtExpired() {
            JwtWebSocketChannelInterceptor interceptor =
                    new JwtWebSocketChannelInterceptor(jwtDecoder, converter);
            when(jwtDecoder.decode("expired-token"))
                    .thenThrow(new JwtValidationException(
                            "Jwt expired",
                            List.of(new OAuth2Error(
                                    "invalid_token",
                                    "Jwt expired at 2026-06-15T10:32:30Z",
                                    null))));

            BadCredentialsException ex = assertThrows(
                    BadCredentialsException.class,
                    () -> interceptor.preSend(connectMessage("Bearer expired-token"), channel));

            assertEquals("STOMP CONNECT JWT 已过期", ex.getMessage());
            verify(converter, never()).convert(any());
        }
    }

    private static Message<byte[]> connectMessage(String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionId("session-1");
        accessor.addNativeHeader("Authorization", authorization);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
