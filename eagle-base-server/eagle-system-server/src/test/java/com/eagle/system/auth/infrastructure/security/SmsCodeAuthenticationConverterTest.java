package com.eagle.auth.infrastructure.security;

import com.eagle.common.exception.DomainException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("短信验证码登录请求转换器测试")
class SmsCodeAuthenticationConverterTest {

    private SmsCodeAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        converter = new SmsCodeAuthenticationConverter();
        // 模拟客户端认证上下文
        OAuth2ClientAuthenticationToken clientAuth = mock(OAuth2ClientAuthenticationToken.class);
        when(clientAuth.isAuthenticated()).thenReturn(true);
        when(clientAuth.getRegisteredClient()).thenReturn(mock(RegisteredClient.class));
        SecurityContextHolder.getContext().setAuthentication(clientAuth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("非短信 grant_type 应返回 null")
    void shouldReturnNullWhenGrantTypeNotMatch() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("grant_type", "authorization_code");

        Authentication result = converter.convert(request);

        assertNull(result);
    }

    @Test
    @DisplayName("缺少手机号应抛出异常")
    void shouldThrowExceptionWhenPhoneIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("grant_type", "sms_code");
        request.setParameter("code", "123456");

        assertThrows(DomainException.class, () -> converter.convert(request));
    }

    @Test
    @DisplayName("缺少验证码应抛出异常")
    void shouldThrowExceptionWhenCodeIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("grant_type", "sms_code");
        request.setParameter("phone", "13800138000");

        assertThrows(DomainException.class, () -> converter.convert(request));
    }

    @Test
    @DisplayName("手机号为空白应抛出异常")
    void shouldThrowExceptionWhenPhoneIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("grant_type", "sms_code");
        request.setParameter("phone", "  ");
        request.setParameter("code", "123456");

        assertThrows(DomainException.class, () -> converter.convert(request));
    }

    @Test
    @DisplayName("验证码为空白应抛出异常")
    void shouldThrowExceptionWhenCodeIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("grant_type", "sms_code");
        request.setParameter("phone", "13800138000");
        request.setParameter("code", "  ");

        assertThrows(DomainException.class, () -> converter.convert(request));
    }

    @Test
    @DisplayName("参数完整应返回 SmsCodeAuthenticationToken")
    void shouldReturnTokenWhenParametersAreValid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("grant_type", "sms_code");
        request.setParameter("phone", "13800138000");
        request.setParameter("code", "654321");

        Authentication result = converter.convert(request);

        assertNotNull(result);
        assertInstanceOf(SmsCodeAuthenticationToken.class, result);
        SmsCodeAuthenticationToken token = (SmsCodeAuthenticationToken) result;
        assertEquals("13800138000", token.getPhone());
        assertEquals("654321", token.getCode());
    }
}
