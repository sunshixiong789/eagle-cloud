package com.eagle.system.base.application.service;

import com.eagle.common.exception.AppException;
import com.eagle.common.exception.DomainException;
import com.eagle.common.exception.codes.OperationErrorCode;
import com.eagle.system.auth.domain.port.OnlineUserInfo;
import com.eagle.system.auth.domain.port.OnlineUserPort;
import com.eagle.system.base.domain.repository.LogRepository;
import com.eagle.system.base.interfaces.dto.response.OnlineUserListResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitorApplicationServiceTest {

    @Mock
    OnlineUserPort onlineUserPort;
    @Mock
    LogApplicationService logApplicationService;
    @Mock
    LogRepository logRepository;
    @InjectMocks
    MonitorApplicationService service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("listOnlineUsers")
    class ListOnline {
        @Test
        @DisplayName("should map port infos to responses")
        void shouldMap() {
            OnlineUserInfo info = new OnlineUserInfo(
                    "jti-1", 100L, "alice", "127.0.0.1",
                    LocalDateTime.now(), LocalDateTime.now(),
                    "Chrome", "macOS", 3600L);
            when(onlineUserPort.listOnlineUsers()).thenReturn(List.of(info));

            OnlineUserListResponse resp = service.listOnlineUsers();

            assertEquals(1, resp.getTotalCount());
            assertEquals("jti-1", resp.getUsers().get(0).getTokenId());
        }
    }

    @Nested
    @DisplayName("forceLogout")
    class ForceLogout {

        @Test
        @DisplayName("should reject kicking out the current user")
        void shouldRejectSelfKick() {
            Jwt jwt = Jwt.withTokenValue("token").header("alg", "none")
                    .claim("jti", "self-jti")
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                    .build();
            // 用 Jwt 自身作为 credentials；服务从 auth.getCredentials() 抽 JTI
            TestingAuthenticationToken auth =
                    new TestingAuthenticationToken("alice", jwt);
            auth.setAuthenticated(true);
            SecurityContextHolder.getContext().setAuthentication(auth);

            AppException ex = assertThrows(DomainException.class,
                    () -> service.forceLogout("self-jti"));
            assertEquals(OperationErrorCode.OPERATION_NOT_ALLOWED, ex.getErrorCode());
            verify(onlineUserPort, never()).forceLogout("self-jti");
        }

        @Test
        @DisplayName("should delegate to port for non-self target")
        void shouldDelegate() {
            Jwt jwt = Jwt.withTokenValue("token").header("alg", "none")
                    .claim("jti", "self-jti")
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                    .build();
            TestingAuthenticationToken auth =
                    new TestingAuthenticationToken("alice", jwt);
            auth.setAuthenticated(true);
            SecurityContextHolder.getContext().setAuthentication(auth);

            service.forceLogout("other-jti");
            verify(onlineUserPort).forceLogout("other-jti");
        }

        @Test
        @DisplayName("should allow forceLogout when no current auth context")
        void shouldAllowWithoutAuth() {
            service.forceLogout("any");
            verify(onlineUserPort).forceLogout("any");
        }
    }
}
