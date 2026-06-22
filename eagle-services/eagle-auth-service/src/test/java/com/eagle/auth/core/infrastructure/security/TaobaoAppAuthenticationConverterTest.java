package com.eagle.auth.core.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

class TaobaoAppAuthenticationConverterTest {

    @Test
    @DisplayName("应解析淘宝授权参数为 Token")
    void convertsTaobaoParams() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(OAuth2ParameterNames.GRANT_TYPE, TaobaoAppAuthenticationToken.TAOBAO_APP.getValue());
        request.setParameter("tb_access_token", "acc-1");
        request.setParameter("tb_auth_code", "code-1");
        request.setParameter("phone", "13800138000");
        request.setParameter("sms_code", "123456");
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("eagleApp", null));

        try {
            var auth = new TaobaoAppAuthenticationConverter().convert(request);
            TaobaoAppAuthenticationToken token = assertInstanceOf(TaobaoAppAuthenticationToken.class, auth);
            assertEquals("acc-1", token.getTbAccessToken());
            assertEquals("code-1", token.getTbAuthCode());
            assertEquals("13800138000", token.getPhone());
            assertEquals("123456", token.getSmsCode());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("非 taobao_app grant 返回 null")
    void ignoresOtherGrant() {
        assertNull(new TaobaoAppAuthenticationConverter().convert(new MockHttpServletRequest()));
    }
}
