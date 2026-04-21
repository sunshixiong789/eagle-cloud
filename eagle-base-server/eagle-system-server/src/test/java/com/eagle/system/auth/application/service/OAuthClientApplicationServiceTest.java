package com.eagle.auth.application.service;

import com.eagle.auth.application.mapper.OAuthClientMapper;
import com.eagle.auth.domain.model.OAuthClient;
import com.eagle.auth.domain.repository.OAuthClientRepository;
import com.eagle.auth.web.dto.request.CreateOAuthClientRequest;
import com.eagle.auth.web.dto.request.UpdateOAuthClientRequest;
import com.eagle.auth.web.dto.response.OAuthClientResponse;
import com.eagle.common.exception.ConflictException;
import com.eagle.common.exception.DomainException;
import com.eagle.common.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * OAuthClientApplicationService 单元测试
 *
 * @author sunshixiong
 */
@DisplayName("OAuth2 客户端应用服务")
@ExtendWith(MockitoExtension.class)
class OAuthClientApplicationServiceTest {

    @Mock
    private OAuthClientRepository oAuthClientRepository;

    @Mock
    private OAuthClientMapper oAuthClientMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private OAuthClientApplicationService oAuthClientApplicationService;

    @Nested
    @DisplayName("createClient")
    class CreateClient {

        @Test
        @DisplayName("should create client successfully")
        void shouldCreateClientSuccessfully() {
            // Given
            CreateOAuthClientRequest request = new CreateOAuthClientRequest();
            request.setClientId("test-client");
            request.setClientSecret("secret123");
            request.setClientName("Test Client");
            request.setClientAuthenticationMethods(Set.of("client_secret_basic"));
            request.setAuthorizationGrantTypes(Set.of("authorization_code", "refresh_token"));
            request.setRedirectUris(Set.of("http://localhost:8080/callback"));
            request.setScopes(Set.of("openid", "profile"));
            request.setAccessTokenTtlSeconds(3600L);
            request.setRefreshTokenTtlSeconds(2592000L);
            request.setRequireProofKey(false);
            request.setRequireAuthorizationConsent(false);

            OAuthClientResponse expectedResponse = new OAuthClientResponse();

            when(oAuthClientRepository.existsByClientId("test-client")).thenReturn(false);
            when(passwordEncoder.encode("secret123")).thenReturn("encoded_secret");
            when(oAuthClientRepository.save(any(OAuthClient.class))).thenAnswer(inv -> inv.getArgument(0));
            when(oAuthClientMapper.toResponse(any(OAuthClient.class))).thenReturn(expectedResponse);

            // When
            OAuthClientResponse result = oAuthClientApplicationService.createClient(request);

            // Then
            assertNotNull(result);
            verify(oAuthClientRepository).existsByClientId("test-client");
            verify(passwordEncoder).encode("secret123");
            verify(oAuthClientRepository).save(any(OAuthClient.class));
        }

        @Test
        @DisplayName("should create client without secret")
        void shouldCreateClientWithoutSecret() {
            // Given
            CreateOAuthClientRequest request = new CreateOAuthClientRequest();
            request.setClientId("public-client");
            request.setClientSecret(null);
            request.setClientName("Public Client");
            request.setClientAuthenticationMethods(Set.of("none"));
            request.setAuthorizationGrantTypes(Set.of("authorization_code"));
            request.setAccessTokenTtlSeconds(3600L);
            request.setRefreshTokenTtlSeconds(2592000L);
            request.setRequireProofKey(true);
            request.setRequireAuthorizationConsent(false);

            OAuthClientResponse expectedResponse = new OAuthClientResponse();

            when(oAuthClientRepository.existsByClientId("public-client")).thenReturn(false);
            when(oAuthClientRepository.save(any(OAuthClient.class))).thenAnswer(inv -> inv.getArgument(0));
            when(oAuthClientMapper.toResponse(any(OAuthClient.class))).thenReturn(expectedResponse);

            // When
            OAuthClientResponse result = oAuthClientApplicationService.createClient(request);

            // Then
            assertNotNull(result);
            verify(passwordEncoder, never()).encode(any());
        }

        @Test
        @DisplayName("should throw ConflictException when clientId already exists")
        void shouldThrowWhenClientIdExists() {
            // Given
            CreateOAuthClientRequest request = new CreateOAuthClientRequest();
            request.setClientId("existing-client");

            when(oAuthClientRepository.existsByClientId("existing-client")).thenReturn(true);

            // When & Then
            assertThrows(ConflictException.class, () ->
                oAuthClientApplicationService.createClient(request));
            verify(oAuthClientRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateClient")
    class UpdateClient {

        @Test
        @DisplayName("should update client successfully")
        void shouldUpdateClientSuccessfully() {
            // Given
            Long id = 1L;
            UpdateOAuthClientRequest request = new UpdateOAuthClientRequest();
            request.setClientName("Updated Client");
            request.setClientSecret("new_secret");
            request.setClientAuthenticationMethods(Set.of("client_secret_basic"));
            request.setAuthorizationGrantTypes(Set.of("authorization_code"));
            request.setRedirectUris(Set.of("http://localhost:9090/callback"));
            request.setScopes(Set.of("openid"));
            request.setAccessTokenTtlSeconds(7200L);
            request.setRefreshTokenTtlSeconds(86400L);
            request.setRequireProofKey(true);
            request.setRequireAuthorizationConsent(true);

            OAuthClient existingClient = OAuthClient.create(
                "test-client", "encoded_old", "Test Client",
                "client_secret_basic", "authorization_code",
                "http://localhost:8080/callback", "openid"
            );
            OAuthClientResponse expectedResponse = new OAuthClientResponse();

            when(oAuthClientRepository.findById(id)).thenReturn(Optional.of(existingClient));
            when(passwordEncoder.encode("new_secret")).thenReturn("encoded_new_secret");
            when(oAuthClientRepository.save(any(OAuthClient.class))).thenAnswer(inv -> inv.getArgument(0));
            when(oAuthClientMapper.toResponse(any(OAuthClient.class))).thenReturn(expectedResponse);

            // When
            OAuthClientResponse result = oAuthClientApplicationService.updateClient(id, request);

            // Then
            assertNotNull(result);
            verify(passwordEncoder).encode("new_secret");
            verify(oAuthClientRepository).save(existingClient);
        }

        @Test
        @DisplayName("should update client without changing secret")
        void shouldUpdateClientWithoutChangingSecret() {
            // Given
            Long id = 1L;
            UpdateOAuthClientRequest request = new UpdateOAuthClientRequest();
            request.setClientName("Updated Client");
            request.setClientSecret(null);
            request.setAccessTokenTtlSeconds(3600L);
            request.setRefreshTokenTtlSeconds(2592000L);
            request.setRequireProofKey(false);
            request.setRequireAuthorizationConsent(false);

            OAuthClient existingClient = OAuthClient.create(
                "test-client", "encoded_old", "Test Client",
                "client_secret_basic", "authorization_code",
                "http://localhost:8080/callback", "openid"
            );
            OAuthClientResponse expectedResponse = new OAuthClientResponse();

            when(oAuthClientRepository.findById(id)).thenReturn(Optional.of(existingClient));
            when(oAuthClientRepository.save(any(OAuthClient.class))).thenAnswer(inv -> inv.getArgument(0));
            when(oAuthClientMapper.toResponse(any(OAuthClient.class))).thenReturn(expectedResponse);

            // When
            OAuthClientResponse result = oAuthClientApplicationService.updateClient(id, request);

            // Then
            assertNotNull(result);
            verify(passwordEncoder, never()).encode(any());
        }

        @Test
        @DisplayName("should throw NotFoundException when client not found")
        void shouldThrowWhenClientNotFound() {
            // Given
            Long id = 999L;
            UpdateOAuthClientRequest request = new UpdateOAuthClientRequest();

            when(oAuthClientRepository.findById(id)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () ->
                oAuthClientApplicationService.updateClient(id, request));
        }
    }

    @Nested
    @DisplayName("deleteClient")
    class DeleteClient {

        @Test
        @DisplayName("should delete client successfully")
        void shouldDeleteClientSuccessfully() {
            // Given
            Long id = 1L;
            when(oAuthClientRepository.existsById(id)).thenReturn(true);

            // When
            oAuthClientApplicationService.deleteClient(id);

            // Then
            verify(oAuthClientRepository).deleteById(id);
        }

        @Test
        @DisplayName("should throw NotFoundException when client not found")
        void shouldThrowWhenClientNotFound() {
            // Given
            Long id = 999L;
            when(oAuthClientRepository.existsById(id)).thenReturn(false);

            // When & Then
            assertThrows(NotFoundException.class, () ->
                oAuthClientApplicationService.deleteClient(id));
            verify(oAuthClientRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("getClientById")
    class GetClientById {

        @Test
        @DisplayName("should return client response when found")
        void shouldReturnClientResponse() {
            // Given
            Long id = 1L;
            OAuthClient client = OAuthClient.create(
                "test-client", "secret", "Test Client",
                "client_secret_basic", "authorization_code",
                "http://localhost/callback", "openid"
            );
            OAuthClientResponse expectedResponse = new OAuthClientResponse();

            when(oAuthClientRepository.findById(id)).thenReturn(Optional.of(client));
            when(oAuthClientMapper.toResponse(client)).thenReturn(expectedResponse);

            // When
            OAuthClientResponse result = oAuthClientApplicationService.getClientById(id);

            // Then
            assertNotNull(result);
            verify(oAuthClientMapper).toResponse(client);
        }

        @Test
        @DisplayName("should throw NotFoundException when client not found")
        void shouldThrowWhenClientNotFound() {
            // Given
            Long id = 999L;
            when(oAuthClientRepository.findById(id)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () ->
                oAuthClientApplicationService.getClientById(id));
        }
    }

    @Nested
    @DisplayName("queryClients")
    class QueryClients {

        @Test
        @DisplayName("should return paginated clients")
        void shouldReturnPaginatedClients() {
            // Given
            Pageable pageable = Pageable.ofSize(10);
            OAuthClient client = OAuthClient.create(
                "test-client", "secret", "Test Client",
                "client_secret_basic", "authorization_code",
                "http://localhost/callback", "openid"
            );
            Page<OAuthClient> clientPage = new PageImpl<>(List.of(client));
            OAuthClientResponse response = new OAuthClientResponse();

            when(oAuthClientRepository.findAll(pageable)).thenReturn(clientPage);
            when(oAuthClientMapper.toResponse(client)).thenReturn(response);

            // When
            Page<OAuthClientResponse> result = oAuthClientApplicationService.queryClients(pageable);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
        }
    }

    @Nested
    @DisplayName("enableClient")
    class EnableClient {

        @Test
        @DisplayName("should enable client successfully")
        void shouldEnableClientSuccessfully() {
            // Given
            Long id = 1L;
            OAuthClient client = OAuthClient.create(
                "test-client", "secret", "Test Client",
                "client_secret_basic", "authorization_code",
                "http://localhost/callback", "openid"
            );
            client.disable();

            when(oAuthClientRepository.findById(id)).thenReturn(Optional.of(client));

            // When
            oAuthClientApplicationService.enableClient(id);

            // Then
            assertTrue(client.getEnabled());
            verify(oAuthClientRepository).save(client);
        }

        @Test
        @DisplayName("should throw NotFoundException when client not found")
        void shouldThrowWhenClientNotFound() {
            // Given
            Long id = 999L;
            when(oAuthClientRepository.findById(id)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () ->
                oAuthClientApplicationService.enableClient(id));
        }
    }

    @Nested
    @DisplayName("disableClient")
    class DisableClient {

        @Test
        @DisplayName("should disable client successfully")
        void shouldDisableClientSuccessfully() {
            // Given
            Long id = 1L;
            OAuthClient client = OAuthClient.create(
                "test-client", "secret", "Test Client",
                "client_secret_basic", "authorization_code",
                "http://localhost/callback", "openid"
            );

            when(oAuthClientRepository.findById(id)).thenReturn(Optional.of(client));

            // When
            oAuthClientApplicationService.disableClient(id);

            // Then
            assertFalse(client.getEnabled());
            verify(oAuthClientRepository).save(client);
        }

        @Test
        @DisplayName("should throw NotFoundException when client not found")
        void shouldThrowWhenClientNotFound() {
            // Given
            Long id = 999L;
            when(oAuthClientRepository.findById(id)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () ->
                oAuthClientApplicationService.disableClient(id));
        }
    }
}
