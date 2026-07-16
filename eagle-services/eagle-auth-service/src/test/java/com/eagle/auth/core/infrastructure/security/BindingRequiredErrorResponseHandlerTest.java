package com.eagle.auth.core.infrastructure.security;

import com.eagle.auth.core.domain.model.enums.SocialProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BindingRequiredErrorResponseHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BindingRequiredErrorResponseHandler handler =
            new BindingRequiredErrorResponseHandler(objectMapper);

    @Test
    @DisplayName("binding_required 应输出 bind_ticket 与 provider")
    void shouldWriteBindingRequiredBody() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(request, response,
                new SocialBindingRequiredException("ticket-123", SocialProvider.APPLE));

        assertEquals(400, response.getStatus());
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertEquals("binding_required", body.get("error").asString());
        assertEquals("ticket-123", body.get("bind_ticket").asString());
        assertEquals("APPLE", body.get("provider").asString());
    }

    @Test
    @DisplayName("其他 OAuth2 异常应走 SAS 默认输出，不受影响")
    void shouldDelegateOtherErrors() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(request, response,
                new OAuth2AuthenticationException(new OAuth2Error("invalid_grant")));

        assertEquals(400, response.getStatus());
        assertTrue(response.getContentAsString().contains("invalid_grant"));
    }
}
