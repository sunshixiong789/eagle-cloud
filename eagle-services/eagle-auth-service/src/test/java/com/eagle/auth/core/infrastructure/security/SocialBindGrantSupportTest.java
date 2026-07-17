package com.eagle.auth.core.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocialBindGrantSupportTest {

    private static final RegisteredClient APP_CLIENT = RegisteredClient.withId("social-bind")
            .clientId("eagleApp")
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .authorizationGrantType(SocialBindAuthenticationToken.SOCIAL_BIND)
            .scope("openid")
            .build();

    private static final RegisteredClientRepository CLIENTS = new RegisteredClientRepository() {
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
    void publicClientSupportsSocialBindGrant() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(OAuth2ParameterNames.GRANT_TYPE, "social_bind");
        request.setParameter(OAuth2ParameterNames.CLIENT_ID, "eagleApp");
        request.setParameter("bind_ticket", "ticket-123");
        request.setParameter("phone", "13800138000");
        request.setParameter("code", "123456");

        OAuth2ClientAuthenticationToken converted = assertInstanceOf(
                OAuth2ClientAuthenticationToken.class,
                new CustomGrantPublicClientAuthenticationConverter("/oauth2/revoke")
                        .convert(request));
        assertEquals("eagleApp", converted.getPrincipal());

        OAuth2ClientAuthenticationToken authenticated = assertInstanceOf(
                OAuth2ClientAuthenticationToken.class,
                new CustomGrantClientAuthenticationProvider(CLIENTS).authenticate(
                        new OAuth2ClientAuthenticationToken(
                                "eagleApp", ClientAuthenticationMethod.NONE, null,
                                Map.of(OAuth2ParameterNames.GRANT_TYPE, "social_bind"))));
        assertTrue(authenticated.isAuthenticated());
    }
}
