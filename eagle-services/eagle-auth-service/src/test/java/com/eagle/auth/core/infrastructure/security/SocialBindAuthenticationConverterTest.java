package com.eagle.auth.core.infrastructure.security;

import com.eagle.auth.core.domain.AuthErrorCode;
import com.eagle.common.exception.AppException;
import com.eagle.common.exception.DomainException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SocialBindAuthenticationConverterTest {

    private final SocialBindAuthenticationConverter converter =
            new SocialBindAuthenticationConverter();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(OAuth2ParameterNames.GRANT_TYPE,
                SocialBindAuthenticationToken.SOCIAL_BIND.getValue());
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("eagleApp", null));
        return request;
    }

    @Test
    @DisplayName("应解析 bind_ticket/phone/code 为 Token")
    void convertsParams() {
        MockHttpServletRequest request = request();
        request.setParameter("bind_ticket", "t-1");
        request.setParameter("phone", "13800138000");
        request.setParameter("code", "123456");

        var auth = converter.convert(request);

        SocialBindAuthenticationToken token =
                assertInstanceOf(SocialBindAuthenticationToken.class, auth);
        assertEquals("t-1", token.getBindTicket());
        assertEquals("13800138000", token.getPhone());
        assertEquals("123456", token.getCode());
    }

    @Test
    @DisplayName("非 social_bind grant 返回 null")
    void returnsNullForOtherGrant() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(OAuth2ParameterNames.GRANT_TYPE, "sms_code");
        assertNull(converter.convert(request));
    }

    @Test
    @DisplayName("缺 bind_ticket 应抛 SOCIAL_BIND_TICKET_INVALID")
    void missingTicketThrows() {
        MockHttpServletRequest request = request();
        request.setParameter("phone", "13800138000");
        request.setParameter("code", "123456");

        AppException ex = assertThrows(DomainException.class, () -> converter.convert(request));
        assertEquals(AuthErrorCode.SOCIAL_BIND_TICKET_INVALID, ex.getErrorCode());
    }

    @Test
    @DisplayName("缺手机号应抛 SMS_PHONE_REQUIRED")
    void missingPhoneThrows() {
        MockHttpServletRequest request = request();
        request.setParameter("bind_ticket", "t-1");
        request.setParameter("code", "123456");

        AppException ex = assertThrows(DomainException.class, () -> converter.convert(request));
        assertEquals(AuthErrorCode.SMS_PHONE_REQUIRED, ex.getErrorCode());
    }
}
