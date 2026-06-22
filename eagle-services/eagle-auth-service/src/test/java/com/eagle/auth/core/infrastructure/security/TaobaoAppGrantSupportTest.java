package com.eagle.auth.core.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class TaobaoAppGrantSupportTest {

    @Test
    @DisplayName("公共客户端转换器应支持 taobao_app 授权")
    void publicClientConverterSupportsTaobaoGrant() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(OAuth2ParameterNames.GRANT_TYPE, TaobaoAppAuthenticationToken.TAOBAO_APP.getValue());
        request.setParameter(OAuth2ParameterNames.CLIENT_ID, "eagleApp");
        request.setParameter("tb_auth_code", "authcode");

        var converter = new CustomGrantPublicClientAuthenticationConverter("/oauth2/revoke");
        var authentication = converter.convert(request);

        OAuth2ClientAuthenticationToken clientToken =
                assertInstanceOf(OAuth2ClientAuthenticationToken.class, authentication);
        assertEquals("eagleApp", clientToken.getPrincipal());
        assertEquals(ClientAuthenticationMethod.NONE, clientToken.getClientAuthenticationMethod());
    }
}
