package com.eagle.websocket.interceptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link WebSocketAuthHandshakeInterceptor}.
 */
@ExtendWith(MockitoExtension.class)
class WebSocketAuthHandshakeInterceptorTest {

    @Mock
    private ServerHttpRequest request;

    @Mock
    private ServerHttpResponse response;

    private WebSocketHandler wsHandler;
    private WebSocketAuthHandshakeInterceptor interceptor;
    private Map<String, Object> attributes;

    @BeforeEach
    void setUp() {
        wsHandler = mock(WebSocketHandler.class);
        interceptor = new WebSocketAuthHandshakeInterceptor();
        attributes = new HashMap<>();
    }

    @Nested
    @DisplayName("beforeHandshake — token from query string")
    class TokenFromQueryString {

        @Test
        @DisplayName("should extract token from URL query parameter and store in attributes")
        void beforeHandshake_shouldExtractTokenFromQueryString() throws Exception {
            when(request.getURI()).thenReturn(new URI("ws://localhost/ws?token=abc123"));
            // getHeaders() is never called when token is found in query string

            boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

            assertTrue(result);
            assertEquals("abc123", attributes.get("token"));
        }

        @Test
        @DisplayName("should extract token when there are multiple query parameters")
        void beforeHandshake_shouldExtractTokenAmongMultipleParams() throws Exception {
            when(request.getURI()).thenReturn(new URI("ws://localhost/ws?userId=42&token=mytoken&lang=zh"));
            // getHeaders() is never called when token is found in query string

            boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

            assertTrue(result);
            assertEquals("mytoken", attributes.get("token"));
        }
    }

    @Nested
    @DisplayName("beforeHandshake — token from Authorization header")
    class TokenFromAuthHeader {

        @Test
        @DisplayName("should extract token from Authorization Bearer header when no query param")
        void beforeHandshake_shouldExtractTokenFromAuthHeader() throws Exception {
            when(request.getURI()).thenReturn(new URI("ws://localhost/ws"));
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer xyz-jwt-token");
            when(request.getHeaders()).thenReturn(headers);

            boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

            assertTrue(result);
            assertEquals("xyz-jwt-token", attributes.get("token"));
        }

        @Test
        @DisplayName("should not extract token when Authorization header lacks Bearer prefix")
        void beforeHandshake_shouldIgnoreNonBearerAuthHeader() throws Exception {
            when(request.getURI()).thenReturn(new URI("ws://localhost/ws"));
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic dXNlcjpwYXNz");
            when(request.getHeaders()).thenReturn(headers);

            boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

            assertTrue(result);
            assertFalse(attributes.containsKey("token"));
        }
    }

    @Nested
    @DisplayName("beforeHandshake — anonymous connection")
    class AnonymousConnection {

        @Test
        @DisplayName("should return true and not set token when no token is provided")
        void beforeHandshake_shouldAllowAnonymous() throws Exception {
            when(request.getURI()).thenReturn(new URI("ws://localhost/ws"));
            when(request.getHeaders()).thenReturn(new HttpHeaders());

            boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

            assertTrue(result);
            assertFalse(attributes.containsKey("token"),
                    "attributes should not contain 'token' for anonymous connections");
        }
    }

    @Nested
    @DisplayName("beforeHandshake — query param takes priority over header")
    class QueryParamPriority {

        @Test
        @DisplayName("should prefer query param token over Authorization header")
        void beforeHandshake_shouldPreferQueryParamOverHeader() throws Exception {
            when(request.getURI()).thenReturn(new URI("ws://localhost/ws?token=query-token"));
            // getHeaders() is never reached when query param token is found (short-circuit)

            boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

            assertTrue(result);
            assertEquals("query-token", attributes.get("token"));
        }
    }
}
