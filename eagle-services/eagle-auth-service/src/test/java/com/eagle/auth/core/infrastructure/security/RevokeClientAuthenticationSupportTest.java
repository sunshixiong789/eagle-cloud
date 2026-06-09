package com.eagle.auth.core.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * 公共客户端（无 secret）调用 {@code /oauth2/revoke} 退出登录的客户端认证支持。
 *
 * <p>背景：revoke 请求不带 {@code grant_type}，SAS 内置 {@code PublicClientAuthenticationConverter}
 * 又强制要求 PKCE {@code code_verifier}，导致公共客户端 revoke 请求 fallback 到 {@code invalid_client}
 * （中台退出登录报「无效的 client」）。本测试约束 {@link CustomGrantPublicClientAuthenticationConverter}
 * 与 {@link CustomGrantClientAuthenticationProvider} 对 revoke 端点放行无凭据的公共客户端。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("公共客户端 revoke 端点认证支持")
class RevokeClientAuthenticationSupportTest {

    private static final String REVOKE = "/oauth2/revoke";

    @Mock
    RegisteredClientRepository registeredClientRepository;

    private static RegisteredClient publicWebClient() {
        return RegisteredClient.withId("c-web")
                .clientId("eagleWeb")
                .clientName("Web")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .scope("openid")
                .build();
    }

    // ====================== converter ======================

    @Test
    @DisplayName("converter 应放行无凭据的公共客户端 revoke 请求")
    void converterAuthenticatesPublicClientRevoke() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", REVOKE);
        request.setParameter(OAuth2ParameterNames.TOKEN, "the-refresh-token");
        request.setParameter(OAuth2ParameterNames.TOKEN_TYPE_HINT, "refresh_token");
        request.setParameter(OAuth2ParameterNames.CLIENT_ID, "eagleWeb");

        var converter = new CustomGrantPublicClientAuthenticationConverter(REVOKE);
        Authentication authentication = converter.convert(request);

        OAuth2ClientAuthenticationToken token =
                assertInstanceOf(OAuth2ClientAuthenticationToken.class, authentication);
        assertEquals("eagleWeb", token.getPrincipal());
        assertEquals(ClientAuthenticationMethod.NONE, token.getClientAuthenticationMethod());
        assertEquals("the-refresh-token",
                token.getAdditionalParameters().get(OAuth2ParameterNames.TOKEN));
        assertFalse(token.getAdditionalParameters().containsKey(OAuth2ParameterNames.CLIENT_ID));
    }

    @Test
    @DisplayName("converter 应忽略带 client 凭据（Basic）的 revoke 请求，交给内置 converter")
    void converterIgnoresRevokeWithBasicAuth() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", REVOKE);
        request.setParameter(OAuth2ParameterNames.TOKEN, "tk");
        request.setParameter(OAuth2ParameterNames.CLIENT_ID, "eagleWeb");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic ZWFnbGVXZWI6c2VjcmV0");

        var converter = new CustomGrantPublicClientAuthenticationConverter(REVOKE);
        assertNull(converter.convert(request));
    }

    @Test
    @DisplayName("converter 不应在非 revoke 端点触发")
    void converterIgnoresNonRevokeEndpoint() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/oauth2/token");
        request.setParameter(OAuth2ParameterNames.TOKEN, "tk");
        request.setParameter(OAuth2ParameterNames.CLIENT_ID, "eagleWeb");

        var converter = new CustomGrantPublicClientAuthenticationConverter(REVOKE);
        assertNull(converter.convert(request));
    }

    // ====================== provider ======================

    @Test
    @DisplayName("provider 应认证公共客户端 revoke（无 grant_type 带 token）")
    void providerAuthenticatesPublicClientRevoke() {
        RegisteredClient client = publicWebClient();
        when(registeredClientRepository.findByClientId("eagleWeb")).thenReturn(client);

        var provider = new CustomGrantClientAuthenticationProvider(registeredClientRepository);
        OAuth2ClientAuthenticationToken request = new OAuth2ClientAuthenticationToken(
                "eagleWeb", ClientAuthenticationMethod.NONE, null,
                Map.of(OAuth2ParameterNames.TOKEN, "the-token"));

        OAuth2ClientAuthenticationToken result =
                (OAuth2ClientAuthenticationToken) provider.authenticate(request);

        assertNotNull(result);
        assertTrue(result.isAuthenticated());
        assertEquals(client, result.getRegisteredClient());
    }

    @Test
    @DisplayName("provider 对无 grant_type 且无 token 的 NONE 请求放行给内置 provider")
    void providerIgnoresNoneTokenWithoutGrantNorToken() {
        var provider = new CustomGrantClientAuthenticationProvider(registeredClientRepository);
        OAuth2ClientAuthenticationToken request = new OAuth2ClientAuthenticationToken(
                "eagleWeb", ClientAuthenticationMethod.NONE, null, Map.of());

        assertNull(provider.authenticate(request));
    }
}
