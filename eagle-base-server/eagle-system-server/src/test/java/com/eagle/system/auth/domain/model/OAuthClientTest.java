package com.eagle.auth.domain.model;

import com.eagle.common.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OAuthClient 聚合根单元测试
 *
 * @author sunshixiong
 */
@DisplayName("OAuthClient 聚合根")
class OAuthClientTest {

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create OAuth client when all required fields are valid")
        void shouldCreateOAuthClient() {
            // When
            OAuthClient client = OAuthClient.create(
                "client_123",
                "secret_123",
                "Test Client",
                "client_secret_basic",
                "authorization_code,refresh_token",
                "http://localhost/callback",
                "openid,profile"
            );

            // Then
            assertNotNull(client);
            assertEquals("client_123", client.getClientId());
            assertEquals("secret_123", client.getClientSecret());
            assertEquals("Test Client", client.getClientName());
            assertEquals("client_secret_basic", client.getClientAuthenticationMethods());
            assertEquals("authorization_code,refresh_token", client.getAuthorizationGrantTypes());
            assertEquals("http://localhost/callback", client.getRedirectUris());
            assertEquals("openid,profile", client.getScopes());
            assertFalse(client.getRequireProofKey());
            assertFalse(client.getRequireAuthorizationConsent());
            assertEquals(3600L, client.getAccessTokenTtlSeconds());
            assertEquals(2592000L, client.getRefreshTokenTtlSeconds());
            assertTrue(client.getEnabled());
            assertNotNull(client.getClientIdIssuedAt());
        }

        @Test
        @DisplayName("should create client with null client secret")
        void shouldCreateClientWithNullSecret() {
            // When
            OAuthClient client = OAuthClient.create(
                "client_123",
                null,
                "Test Client",
                "none",
                "authorization_code",
                "http://localhost/callback",
                "openid"
            );

            // Then
            assertNull(client.getClientSecret());
        }

        @Test
        @DisplayName("should create client with null redirect URIs")
        void shouldCreateClientWithNullRedirectUris() {
            // When
            OAuthClient client = OAuthClient.create(
                "client_123",
                "secret",
                "Test Client",
                "none",
                "authorization_code",
                null,
                "openid"
            );

            // Then
            assertNull(client.getRedirectUris());
        }

        @Test
        @DisplayName("should create client with null scopes")
        void shouldCreateClientWithNullScopes() {
            // When
            OAuthClient client = OAuthClient.create(
                "client_123",
                "secret",
                "Test Client",
                "none",
                "authorization_code",
                "http://localhost/callback",
                null
            );

            // Then
            assertEquals("", client.getScopes());
        }

        @Test
        @DisplayName("should create client with default authentication methods when null")
        void shouldCreateClientWithDefaultAuthMethods() {
            // When
            OAuthClient client = OAuthClient.create(
                "client_123",
                "secret",
                "Test Client",
                null,
                "authorization_code",
                "http://localhost/callback",
                "openid"
            );

            // Then
            assertEquals("none", client.getClientAuthenticationMethods());
        }

        @Test
        @DisplayName("should throw DomainException when clientId is null")
        void shouldThrowWhenClientIdIsNull() {
            assertThrows(DomainException.class, () ->
                OAuthClient.create(null, "secret", "Name", "none", "authorization_code", null, "openid"));
        }

        @Test
        @DisplayName("should throw DomainException when clientId is blank")
        void shouldThrowWhenClientIdIsBlank() {
            assertThrows(DomainException.class, () ->
                OAuthClient.create("  ", "secret", "Name", "none", "authorization_code", null, "openid"));
        }

        @Test
        @DisplayName("should throw DomainException when clientName is null")
        void shouldThrowWhenClientNameIsNull() {
            assertThrows(DomainException.class, () ->
                OAuthClient.create("client_123", "secret", null, "none", "authorization_code", null, "openid"));
        }

        @Test
        @DisplayName("should throw DomainException when clientName is blank")
        void shouldThrowWhenClientNameIsBlank() {
            assertThrows(DomainException.class, () ->
                OAuthClient.create("client_123", "secret", "  ", "none", "authorization_code", null, "openid"));
        }

        @Test
        @DisplayName("should throw DomainException when authorizationGrantTypes is null")
        void shouldThrowWhenGrantTypesIsNull() {
            assertThrows(DomainException.class, () ->
                OAuthClient.create("client_123", "secret", "Name", "none", null, null, "openid"));
        }

        @Test
        @DisplayName("should throw DomainException when authorizationGrantTypes is blank")
        void shouldThrowWhenGrantTypesIsBlank() {
            assertThrows(DomainException.class, () ->
                OAuthClient.create("client_123", "secret", "Name", "none", "  ", null, "openid"));
        }
    }

    @Nested
    @DisplayName("updateInfo")
    class UpdateInfo {

        @Test
        @DisplayName("should update all fields when provided")
        void shouldUpdateAllFields() {
            // Given
            OAuthClient client = OAuthClient.create(
                "client_123", "old_secret", "Old Name", "none",
                "authorization_code", "http://old/callback", "openid");

            // When
            client.updateInfo(
                "New Name", "new_secret", "client_secret_basic",
                "authorization_code,refresh_token", "http://new/callback", "openid,profile");

            // Then
            assertEquals("New Name", client.getClientName());
            assertEquals("new_secret", client.getClientSecret());
            assertEquals("client_secret_basic", client.getClientAuthenticationMethods());
            assertEquals("authorization_code,refresh_token", client.getAuthorizationGrantTypes());
            assertEquals("http://new/callback", client.getRedirectUris());
            assertEquals("openid,profile", client.getScopes());
        }

        @Test
        @DisplayName("should not update null fields")
        void shouldNotUpdateNullFields() {
            // Given
            OAuthClient client = OAuthClient.create(
                "client_123", "secret", "Old Name", "none",
                "authorization_code", "http://old/callback", "openid");

            // When
            client.updateInfo(null, null, null, null, null, null);

            // Then
            assertEquals("Old Name", client.getClientName());
            assertEquals("secret", client.getClientSecret());
            assertEquals("none", client.getClientAuthenticationMethods());
        }

        @Test
        @DisplayName("should update only client name")
        void shouldUpdateOnlyClientName() {
            // Given
            OAuthClient client = OAuthClient.create(
                "client_123", "secret", "Old Name", "none",
                "authorization_code", "http://old/callback", "openid");

            // When
            client.updateInfo("New Name", null, null, null, null, null);

            // Then
            assertEquals("New Name", client.getClientName());
            assertEquals("secret", client.getClientSecret());
        }
    }

    @Nested
    @DisplayName("updateTokenSettings")
    class UpdateTokenSettings {

        @Test
        @DisplayName("should update token TTL settings")
        void shouldUpdateTokenTTL() {
            // Given
            OAuthClient client = OAuthClient.create(
                "client_123", "secret", "Name", "none",
                "authorization_code", null, "openid");

            // When
            client.updateTokenSettings(7200L, 86400L);

            // Then
            assertEquals(7200L, client.getAccessTokenTtlSeconds());
            assertEquals(86400L, client.getRefreshTokenTtlSeconds());
        }

        @Test
        @DisplayName("should not update when TTL is null")
        void shouldNotUpdateWhenTtlIsNull() {
            // Given
            OAuthClient client = OAuthClient.create(
                "client_123", "secret", "Name", "none",
                "authorization_code", null, "openid");

            // When
            client.updateTokenSettings(null, null);

            // Then
            assertEquals(3600L, client.getAccessTokenTtlSeconds());
            assertEquals(2592000L, client.getRefreshTokenTtlSeconds());
        }

        @Test
        @DisplayName("should not update when TTL is zero or negative")
        void shouldNotUpdateWhenTtlIsInvalid() {
            // Given
            OAuthClient client = OAuthClient.create(
                "client_123", "secret", "Name", "none",
                "authorization_code", null, "openid");

            // When
            client.updateTokenSettings(0L, -1L);

            // Then
            assertEquals(3600L, client.getAccessTokenTtlSeconds());
            assertEquals(2592000L, client.getRefreshTokenTtlSeconds());
        }

        @Test
        @DisplayName("should update only access token TTL")
        void shouldUpdateOnlyAccessTokenTtl() {
            // Given
            OAuthClient client = OAuthClient.create(
                "client_123", "secret", "Name", "none",
                "authorization_code", null, "openid");

            // When
            client.updateTokenSettings(7200L, null);

            // Then
            assertEquals(7200L, client.getAccessTokenTtlSeconds());
            assertEquals(2592000L, client.getRefreshTokenTtlSeconds());
        }
    }

    @Nested
    @DisplayName("updateClientSettings")
    class UpdateClientSettings {

        @Test
        @DisplayName("should update PKCE and consent settings")
        void shouldUpdateSettings() {
            // Given
            OAuthClient client = OAuthClient.create(
                "client_123", "secret", "Name", "none",
                "authorization_code", null, "openid");

            // When
            client.updateClientSettings(true, true);

            // Then
            assertTrue(client.getRequireProofKey());
            assertTrue(client.getRequireAuthorizationConsent());
        }

        @Test
        @DisplayName("should not update when settings are null")
        void shouldNotUpdateWhenSettingsAreNull() {
            // Given
            OAuthClient client = OAuthClient.create(
                "client_123", "secret", "Name", "none",
                "authorization_code", null, "openid");

            // When
            client.updateClientSettings(null, null);

            // Then
            assertFalse(client.getRequireProofKey());
            assertFalse(client.getRequireAuthorizationConsent());
        }

        @Test
        @DisplayName("should update only PKCE setting")
        void shouldUpdateOnlyPkceSetting() {
            // Given
            OAuthClient client = OAuthClient.create(
                "client_123", "secret", "Name", "none",
                "authorization_code", null, "openid");

            // When
            client.updateClientSettings(true, null);

            // Then
            assertTrue(client.getRequireProofKey());
            assertFalse(client.getRequireAuthorizationConsent());
        }
    }

    @Nested
    @DisplayName("enable")
    class Enable {

        @Test
        @DisplayName("should enable client")
        void shouldEnableClient() {
            // Given
            OAuthClient client = OAuthClient.create(
                "client_123", "secret", "Name", "none",
                "authorization_code", null, "openid");
            client.disable();

            // When
            client.enable();

            // Then
            assertTrue(client.getEnabled());
        }
    }

    @Nested
    @DisplayName("disable")
    class Disable {

        @Test
        @DisplayName("should disable client when enabled")
        void shouldDisableClient() {
            // Given
            OAuthClient client = OAuthClient.create(
                "client_123", "secret", "Name", "none",
                "authorization_code", null, "openid");

            // When
            client.disable();

            // Then
            assertFalse(client.getEnabled());
        }

        @Test
        @DisplayName("should throw DomainException when already disabled")
        void shouldThrowWhenAlreadyDisabled() {
            // Given
            OAuthClient client = OAuthClient.create(
                "client_123", "secret", "Name", "none",
                "authorization_code", null, "openid");
            client.disable();

            // When & Then
            assertThrows(DomainException.class, client::disable);
        }
    }
}
