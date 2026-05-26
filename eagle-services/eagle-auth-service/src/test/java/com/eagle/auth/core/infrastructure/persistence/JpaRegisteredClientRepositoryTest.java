package com.eagle.auth.core.infrastructure.persistence;

import com.eagle.auth.core.domain.model.OAuthClient;
import com.eagle.auth.core.domain.repository.OAuthClientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaRegisteredClientRepositoryTest {

    private static final long CLIENT_DATABASE_ID = 1L;
    private static final String CLIENT_ID = "eagleWeb";

    @Mock
    private OAuthClientRepository oAuthClientRepository;

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should find registered client by database id when authorization references registered client id")
        void shouldFindRegisteredClientByDatabaseIdWhenAuthorizationReferencesRegisteredClientId() {
            OAuthClient client = OAuthClient.create(
                    CLIENT_ID,
                    null,
                    "Eagle Web",
                    "none",
                    "authorization_code,refresh_token",
                    "http://127.0.0.1/swagger-ui/oauth2-redirect.html",
                    "openid,profile"
            );
            ReflectionTestUtils.setField(client, "id", CLIENT_DATABASE_ID);
            when(oAuthClientRepository.findById(CLIENT_DATABASE_ID)).thenReturn(Optional.of(client));

            JpaRegisteredClientRepository repository = new JpaRegisteredClientRepository(oAuthClientRepository);

            RegisteredClient registeredClient = repository.findById(String.valueOf(CLIENT_DATABASE_ID));

            assertNotNull(registeredClient);
            assertEquals(String.valueOf(CLIENT_DATABASE_ID), registeredClient.getId());
            assertEquals(CLIENT_ID, registeredClient.getClientId());
            assertTrue(registeredClient.getAuthorizationGrantTypes()
                    .contains(AuthorizationGrantType.AUTHORIZATION_CODE));
            verify(oAuthClientRepository).findById(CLIENT_DATABASE_ID);
        }
    }
}
