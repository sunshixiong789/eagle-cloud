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

@DisplayName("微信小程序登录请求转换器测试")
class WechatMiniProgramAuthenticationConverterTest {

    private WechatMiniProgramAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        converter = new WechatMiniProgramAuthenticationConverter();
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
    @DisplayName("非微信 grant_type 应返回 null")
    void shouldReturnNullWhenGrantTypeNotMatch() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("grant_type", "authorization_code");

        Authentication result = converter.convert(request);

        assertNull(result);
    }

    @Test
    @DisplayName("grant_type 匹配但缺少 code 应抛出异常")
    void shouldThrowExceptionWhenCodeIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("grant_type", "wechat_mini_program");

        assertThrows(DomainException.class, () -> converter.convert(request));
    }

    @Test
    @DisplayName("grant_type 匹配且 code 为空白应抛出异常")
    void shouldThrowExceptionWhenCodeIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("grant_type", "wechat_mini_program");
        request.setParameter("code", "  ");

        assertThrows(DomainException.class, () -> converter.convert(request));
    }

    @Test
    @DisplayName("参数完整应返回 WechatMiniProgramAuthenticationToken")
    void shouldReturnTokenWhenParametersAreValid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("grant_type", "wechat_mini_program");
        request.setParameter("code", "wx_login_code_123");

        Authentication result = converter.convert(request);

        assertNotNull(result);
        assertInstanceOf(WechatMiniProgramAuthenticationToken.class, result);
        WechatMiniProgramAuthenticationToken token = (WechatMiniProgramAuthenticationToken) result;
        assertEquals("wx_login_code_123", token.getCode());
    }
}
