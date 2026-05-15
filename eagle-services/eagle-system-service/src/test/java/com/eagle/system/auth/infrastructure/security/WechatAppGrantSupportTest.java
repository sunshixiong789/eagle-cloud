package com.eagle.system.auth.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WechatAppGrantSupportTest {

    @Test
    void customGrantPublicClientConverterSupportsWechatAppGrant() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(OAuth2ParameterNames.GRANT_TYPE, WechatAppAuthenticationToken.WECHAT_APP.getValue());
        request.setParameter(OAuth2ParameterNames.CLIENT_ID, "eagleApp");
        request.setParameter("code", "wechat-app-code");

        var converter = new CustomGrantPublicClientAuthenticationConverter();

        var authentication = converter.convert(request);

        OAuth2ClientAuthenticationToken clientToken =
                assertInstanceOf(OAuth2ClientAuthenticationToken.class, authentication);
        assertEquals("eagleApp", clientToken.getPrincipal());
        assertEquals(ClientAuthenticationMethod.NONE, clientToken.getClientAuthenticationMethod());
        assertEquals(WechatAppAuthenticationToken.WECHAT_APP.getValue(),
                clientToken.getAdditionalParameters().get(OAuth2ParameterNames.GRANT_TYPE));
        assertEquals("wechat-app-code", clientToken.getAdditionalParameters().get("code"));
    }

    @Test
    void wechatAppConverterCreatesWechatAppAuthenticationToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(OAuth2ParameterNames.GRANT_TYPE, WechatAppAuthenticationToken.WECHAT_APP.getValue());
        request.setParameter("code", "wechat-app-code");
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("eagleApp", null));

        var converter = new WechatAppAuthenticationConverter();

        try {
            var authentication = converter.convert(request);

            WechatAppAuthenticationToken token =
                    assertInstanceOf(WechatAppAuthenticationToken.class, authentication);
            assertEquals("wechat-app-code", token.getCode());
            assertNotNull(token.getAdditionalParameters());
            assertEquals("wechat-app-code", token.getAdditionalParameters().get("code"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void wechatAppConverterIgnoresOtherGrantTypes() {
        HttpServletRequest request = new MockHttpServletRequest();

        var converter = new WechatAppAuthenticationConverter();

        var authentication = converter.convert(request);

        assertEquals(null, authentication);
    }
}
