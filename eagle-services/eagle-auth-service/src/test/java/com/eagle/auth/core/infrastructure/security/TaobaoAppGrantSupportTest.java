package com.eagle.auth.core.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaobaoAppGrantSupportTest {

    private static final RegisteredClient APP_CLIENT = RegisteredClient.withId("c-app")
            .clientId("eagleApp")
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .authorizationGrantType(TaobaoAppAuthenticationToken.TAOBAO_APP)
            .scope("openid")
            .build();

    private static final RegisteredClientRepository APP_CLIENT_REPOSITORY = new RegisteredClientRepository() {
        @Override
        public void save(RegisteredClient registeredClient) {
        }

        @Override
        public RegisteredClient findById(String id) {
            return APP_CLIENT.getId().equals(id) ? APP_CLIENT : null;
        }

        @Override
        public RegisteredClient findByClientId(String clientId) {
            return APP_CLIENT.getClientId().equals(clientId) ? APP_CLIENT : null;
        }
    };

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

    @Test
    @DisplayName("公共客户端 provider 应认证 taobao_app 授权")
    void publicClientProviderAuthenticatesTaobaoGrant() {
        var provider = new CustomGrantClientAuthenticationProvider(APP_CLIENT_REPOSITORY);
        OAuth2ClientAuthenticationToken request = new OAuth2ClientAuthenticationToken(
                "eagleApp", ClientAuthenticationMethod.NONE, null,
                Map.of(OAuth2ParameterNames.GRANT_TYPE, TaobaoAppAuthenticationToken.TAOBAO_APP.getValue()));

        OAuth2ClientAuthenticationToken result =
                assertInstanceOf(OAuth2ClientAuthenticationToken.class, provider.authenticate(request));

        assertNotNull(result);
        assertTrue(result.isAuthenticated());
        assertEquals(APP_CLIENT, result.getRegisteredClient());
    }
}
