package com.eagle.auth.core.application.service;

import com.eagle.common.exception.AppException;
import com.eagle.common.exception.ConflictException;
import com.eagle.common.exception.NotFoundException;
import com.eagle.auth.core.application.mapper.OAuthClientMapper;
import com.eagle.auth.core.domain.AuthErrorCode;
import com.eagle.auth.core.domain.model.OAuthClient;
import com.eagle.auth.core.domain.repository.OAuthClientRepository;
import com.eagle.auth.core.interfaces.dto.request.CreateOAuthClientRequest;
import com.eagle.auth.core.interfaces.dto.request.UpdateOAuthClientRequest;
import com.eagle.auth.core.interfaces.dto.response.OAuthClientResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthClientApplicationServiceTest {

    private static final Long ID = 10L;
    private static final String CLIENT_ID = "eagleWeb";

    @Mock
    OAuthClientRepository oAuthClientRepository;
    @Mock
    OAuthClientMapper oAuthClientMapper;
    @Mock
    PasswordEncoder passwordEncoder;
    @InjectMocks
    OAuthClientApplicationService service;

    private OAuthClient sampleClient() {
        return OAuthClient.create(CLIENT_ID, "{bcrypt}secret", "Eagle Web", "none",
                "authorization_code,refresh_token", "http://localhost/cb", "openid,profile");
    }

    private CreateOAuthClientRequest createReq() {
        CreateOAuthClientRequest r = new CreateOAuthClientRequest();
        r.setClientId(CLIENT_ID);
        r.setClientName("Eagle Web");
        r.setClientSecret("plain-secret");
        r.setClientAuthenticationMethods(Set.of("client_secret_basic"));
        r.setAuthorizationGrantTypes(Set.of("authorization_code", "refresh_token"));
        r.setRedirectUris(Set.of("http://localhost/cb"));
        r.setScopes(Set.of("openid", "profile"));
        r.setAccessTokenTtlSeconds(3600L);
        r.setRefreshTokenTtlSeconds(86400L);
        r.setRequireProofKey(true);
        r.setRequireAuthorizationConsent(false);
        return r;
    }

    @Nested
    @DisplayName("createClient")
    class Create {
        @Test
        @DisplayName("should encode secret and save when clientId is free")
        void shouldCreate() {
            when(oAuthClientRepository.existsByClientId(CLIENT_ID)).thenReturn(false);
            when(passwordEncoder.encode("plain-secret")).thenReturn("{bcrypt}secret");
            when(oAuthClientRepository.save(any(OAuthClient.class))).thenAnswer(inv -> inv.getArgument(0));
            when(oAuthClientMapper.toResponse(any(OAuthClient.class))).thenReturn(new OAuthClientResponse());

            service.createClient(createReq());

            ArgumentCaptor<OAuthClient> captor = ArgumentCaptor.forClass(OAuthClient.class);
            verify(oAuthClientRepository).save(captor.capture());
            OAuthClient saved = captor.getValue();
            assertEquals(CLIENT_ID, saved.getClientId());
            assertEquals("{bcrypt}secret", saved.getClientSecret());
        }

        @Test
        @DisplayName("should throw conflict when clientId already exists")
        void shouldThrowWhenIdExists() {
            when(oAuthClientRepository.existsByClientId(CLIENT_ID)).thenReturn(true);
            AppException ex = assertThrows(ConflictException.class,
                    () -> service.createClient(createReq()));
            assertEquals(AuthErrorCode.CLIENT_ID_EXISTS, ex.getErrorCode());
            verify(oAuthClientRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateClient")
    class Update {
        @Test
        @DisplayName("should update existing client")
        void shouldUpdate() {
            OAuthClient existing = sampleClient();
            when(oAuthClientRepository.findById(ID)).thenReturn(Optional.of(existing));
            when(oAuthClientRepository.save(existing)).thenReturn(existing);
            when(oAuthClientMapper.toResponse(existing)).thenReturn(new OAuthClientResponse());

            UpdateOAuthClientRequest req = new UpdateOAuthClientRequest();
            req.setClientName("New Name");
            req.setAccessTokenTtlSeconds(7200L);
            req.setRefreshTokenTtlSeconds(172800L);

            service.updateClient(ID, req);

            assertEquals("New Name", existing.getClientName());
            assertEquals(7200L, existing.getAccessTokenTtlSeconds());
        }

        @Test
        @DisplayName("should throw NotFound when client missing")
        void shouldThrowWhenMissing() {
            when(oAuthClientRepository.findById(ID)).thenReturn(Optional.empty());
            UpdateOAuthClientRequest req = new UpdateOAuthClientRequest();
            AppException ex = assertThrows(NotFoundException.class,
                    () -> service.updateClient(ID, req));
            assertEquals(AuthErrorCode.CLIENT_NOT_FOUND, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("deleteClient")
    class Delete {
        @Test
        @DisplayName("should delete when exists")
        void shouldDelete() {
            when(oAuthClientRepository.existsById(ID)).thenReturn(true);
            service.deleteClient(ID);
            verify(oAuthClientRepository).deleteById(ID);
        }

        @Test
        @DisplayName("should throw NotFound when client missing")
        void shouldThrowWhenMissing() {
            when(oAuthClientRepository.existsById(ID)).thenReturn(false);
            assertThrows(NotFoundException.class, () -> service.deleteClient(ID));
            verify(oAuthClientRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("enable / disable")
    class EnableDisable {
        @BeforeEach
        void setUp() {
            // shared by all tests in this nested block
        }

        @Test
        @DisplayName("should re-enable a disabled client")
        void shouldEnable() {
            OAuthClient client = sampleClient();
            client.disable();
            when(oAuthClientRepository.findById(ID)).thenReturn(Optional.of(client));
            service.enableClient(ID);
            assertTrue(client.getEnabled());
            verify(oAuthClientRepository).save(client);
        }

        @Test
        @DisplayName("should disable an active client")
        void shouldDisable() {
            OAuthClient client = sampleClient();
            when(oAuthClientRepository.findById(ID)).thenReturn(Optional.of(client));
            service.disableClient(ID);
            assertFalse(client.getEnabled());
        }
    }

    @Nested
    @DisplayName("queryClients")
    class Query {
        @Test
        @DisplayName("should map page entries via mapper")
        void shouldMapPage() {
            OAuthClient c = sampleClient();
            Page<OAuthClient> page = new PageImpl<>(List.of(c));
            when(oAuthClientRepository.findAll(any(PageRequest.class))).thenReturn(page);
            when(oAuthClientMapper.toResponse(c)).thenReturn(new OAuthClientResponse());
            Page<OAuthClientResponse> result = service.queryClients(PageRequest.of(0, 10));
            assertEquals(1, result.getTotalElements());
        }
    }
}
