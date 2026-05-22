package com.eagle.auth.domain.model;

import com.eagle.common.exception.AppException;
import com.eagle.common.exception.DomainException;
import com.eagle.auth.domain.AuthErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OAuthClientTest {

    private static final String CLIENT_ID = "eagleWeb";
    private static final String CLIENT_NAME = "Eagle Web";
    private static final String GRANTS = "authorization_code,refresh_token";
    private static final String SCOPES = "openid,profile";

    private OAuthClient newClient() {
        return OAuthClient.create(CLIENT_ID, "secret", CLIENT_NAME, "none", GRANTS,
                "http://localhost/cb", SCOPES);
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should populate all fields and set issuedAt + enabled")
        void shouldCreate() {
            OAuthClient client = newClient();
            assertEquals(CLIENT_ID, client.getClientId());
            assertEquals(CLIENT_NAME, client.getClientName());
            assertEquals(GRANTS, client.getAuthorizationGrantTypes());
            assertEquals(SCOPES, client.getScopes());
            assertNotNull(client.getClientIdIssuedAt());
            assertTrue(client.getEnabled());
        }

        @Test
        @DisplayName("should default authentication methods to 'none' when null")
        void shouldDefaultAuthMethod() {
            OAuthClient client = OAuthClient.create(CLIENT_ID, null, CLIENT_NAME, null, GRANTS, null, null);
            assertEquals("none", client.getClientAuthenticationMethods());
            assertEquals("", client.getScopes());
        }

        @Test
        @DisplayName("should throw when clientId blank")
        void shouldThrowWhenClientIdBlank() {
            AppException ex = assertThrows(DomainException.class,
                    () -> OAuthClient.create("", "s", CLIENT_NAME, "none", GRANTS, null, SCOPES));
            assertEquals(AuthErrorCode.CLIENT_ID_REQUIRED, ex.getErrorCode());
        }

        @Test
        @DisplayName("should throw when clientName blank")
        void shouldThrowWhenClientNameBlank() {
            AppException ex = assertThrows(DomainException.class,
                    () -> OAuthClient.create(CLIENT_ID, "s", " ", "none", GRANTS, null, SCOPES));
            assertEquals(AuthErrorCode.CLIENT_NAME_REQUIRED, ex.getErrorCode());
        }

        @Test
        @DisplayName("should throw when grant types blank")
        void shouldThrowWhenGrantTypesBlank() {
            AppException ex = assertThrows(DomainException.class,
                    () -> OAuthClient.create(CLIENT_ID, "s", CLIENT_NAME, "none", "", null, SCOPES));
            assertEquals(AuthErrorCode.CLIENT_GRANT_TYPE_REQ, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("updateInfo")
    class UpdateInfo {

        @Test
        @DisplayName("should update only non-null fields")
        void shouldUpdateNonNullFields() {
            OAuthClient client = newClient();
            client.updateInfo("New Name", null, null, "client_credentials", null, "openid");
            assertEquals("New Name", client.getClientName());
            assertEquals("secret", client.getClientSecret());
            assertEquals("client_credentials", client.getAuthorizationGrantTypes());
            assertEquals("openid", client.getScopes());
        }
    }

    @Nested
    @DisplayName("updateTokenSettings")
    class UpdateTokenSettings {

        @Test
        @DisplayName("should update positive TTLs")
        void shouldUpdatePositiveTtls() {
            OAuthClient client = newClient();
            client.updateTokenSettings(7200L, 86400L);
            assertEquals(7200L, client.getAccessTokenTtlSeconds());
            assertEquals(86400L, client.getRefreshTokenTtlSeconds());
        }

        @Test
        @DisplayName("should ignore non-positive TTLs")
        void shouldIgnoreNonPositive() {
            OAuthClient client = newClient();
            Long originalAccess = client.getAccessTokenTtlSeconds();
            Long originalRefresh = client.getRefreshTokenTtlSeconds();
            client.updateTokenSettings(0L, -1L);
            assertEquals(originalAccess, client.getAccessTokenTtlSeconds());
            assertEquals(originalRefresh, client.getRefreshTokenTtlSeconds());
        }
    }

    @Nested
    @DisplayName("updateClientSettings")
    class UpdateClientSettings {

        @Test
        @DisplayName("should update PKCE and consent flags")
        void shouldUpdateFlags() {
            OAuthClient client = newClient();
            client.updateClientSettings(true, true);
            assertTrue(client.getRequireProofKey());
            assertTrue(client.getRequireAuthorizationConsent());
        }
    }

    @Nested
    @DisplayName("enable / disable")
    class EnableDisable {

        @Test
        @DisplayName("should disable an enabled client")
        void shouldDisable() {
            OAuthClient client = newClient();
            client.disable();
            assertFalse(client.getEnabled());
        }

        @Test
        @DisplayName("should throw when disabling an already disabled client")
        void shouldThrowOnDoubleDisable() {
            OAuthClient client = newClient();
            client.disable();
            AppException ex = assertThrows(DomainException.class, client::disable);
            assertEquals(AuthErrorCode.CLIENT_DISABLED, ex.getErrorCode());
        }

        @Test
        @DisplayName("should re-enable a disabled client")
        void shouldReEnable() {
            OAuthClient client = newClient();
            client.disable();
            client.enable();
            assertTrue(client.getEnabled());
        }
    }
}
