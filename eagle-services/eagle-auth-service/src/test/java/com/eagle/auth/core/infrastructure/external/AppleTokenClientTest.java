package com.eagle.auth.core.infrastructure.external;

import com.eagle.auth.core.infrastructure.config.AppleAuthenticationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AppleTokenClientTest {

    @Test
    void exchangesAuthorizationCodeForServerTokens() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AppleAuthenticationProperties properties = properties();
        AppleClientSecretGenerator generator = mock(AppleClientSecretGenerator.class);
        when(generator.generate()).thenReturn("client-secret");
        AppleTokenClient client = new AppleTokenClient(
                properties, generator, builder.build());
        server.expect(requestTo("https://appleid.apple.com/auth/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(
                        "client_id=com.shengxinfast.app&client_secret=client-secret"
                                + "&grant_type=authorization_code&code=apple-auth-code"))
                .andRespond(withSuccess(
                        "{\"id_token\":\"server-jwt\",\"refresh_token\":\"refresh-token\"}",
                        MediaType.APPLICATION_JSON));

        AppleTokenClient.AppleTokenSet result = client.exchangeAuthorizationCode(
                "apple-auth-code");

        assertEquals("server-jwt", result.identityToken());
        assertEquals("refresh-token", result.refreshToken());
        server.verify();
    }

    @Test
    void revokesRefreshToken() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AppleClientSecretGenerator generator = mock(AppleClientSecretGenerator.class);
        when(generator.generate()).thenReturn("client-secret");
        AppleTokenClient client = new AppleTokenClient(
                properties(), generator, builder.build());
        server.expect(requestTo("https://appleid.apple.com/auth/revoke"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(
                        "client_id=com.shengxinfast.app&client_secret=client-secret"
                                + "&token=refresh-token&token_type_hint=refresh_token"))
                .andRespond(withSuccess());

        client.revoke("refresh-token");

        server.verify();
    }

    private AppleAuthenticationProperties properties() {
        AppleAuthenticationProperties properties = new AppleAuthenticationProperties();
        properties.setClientId("com.shengxinfast.app");
        return properties;
    }
}
