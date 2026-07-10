package com.eagle.system.base.application.service;

import com.eagle.common.exception.AppException;
import com.eagle.common.exception.DomainException;
import com.eagle.common.exception.codes.OperationErrorCode;
import com.eagle.system.base.domain.repository.LogRepository;
import com.eagle.system.base.infrastructure.remote.AuthClientFacade;
import com.eagle.system.base.infrastructure.remote.dto.OnlineUserSnapshot;
import com.eagle.system.base.interfaces.dto.request.LoginLogQueryRequest;
import com.eagle.system.base.interfaces.dto.response.OnlineUserListResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitorApplicationServiceTest {

    @Mock
    AuthClientFacade authClientFacade;
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
    @DisplayName("queryLoginLogs")
    class QueryLoginLogs {

        @Test
        @DisplayName("无显式排序时应按创建时间和ID倒序查询")
        void shouldApplyDefaultSortWhenQueryLoginLogsWithoutSort() {
            when(logRepository.countByLogTypeAndPeriod(any(), any(), any())).thenReturn(0L);
            when(logRepository.countByLogTypeAndStatusAndPeriod(any(), any(), any(), any())).thenReturn(0L);
            when(logRepository.countDistinctUsernameByLogTypeAndPeriod(any(), any(), any())).thenReturn(0L);
            when(logApplicationService.queryLogs(any(), any(Pageable.class))).thenReturn(Page.empty());

            service.queryLoginLogs(new LoginLogQueryRequest(), PageRequest.of(0, 20));

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(logApplicationService).queryLogs(any(), pageableCaptor.capture());

            Sort sort = pageableCaptor.getValue().getSort();
            assertEquals(Sort.Direction.DESC, sort.getOrderFor("createTime").getDirection());
            assertEquals(Sort.Direction.DESC, sort.getOrderFor("id").getDirection());
        }
    }

    @Nested
    @DisplayName("listOnlineUsers")
    class ListOnline {
        @Test
        @DisplayName("应映射")
        void shouldMap() {
            OnlineUserSnapshot info = new OnlineUserSnapshot(
                    "jti-1", 100L, "alice", "127.0.0.1",
                    LocalDateTime.now(), LocalDateTime.now(),
                    "Chrome", "macOS", 3600L);
            when(authClientFacade.listOnlineUsers()).thenReturn(List.of(info));

            OnlineUserListResponse resp = service.listOnlineUsers();

            assertEquals(1, resp.getTotalCount());
            assertEquals("jti-1", resp.getUsers().get(0).getTokenId());
        }

        @Test
        @DisplayName("应Deduplicate通过用户ID")
        void shouldDeduplicateByUserId() {
            LocalDateTime older = LocalDateTime.now().minusHours(1);
            LocalDateTime newer = LocalDateTime.now();
            OnlineUserSnapshot oldSession = new OnlineUserSnapshot(
                    "jti-old", 100L, "alice", "10.0.0.1",
                    older, older, "Chrome", "macOS", 3600L);
            OnlineUserSnapshot newSession = new OnlineUserSnapshot(
                    "jti-new", 100L, "alice", "10.0.0.2",
                    newer, newer, "Firefox", "Windows", 3600L);
            when(authClientFacade.listOnlineUsers()).thenReturn(List.of(oldSession, newSession));

            OnlineUserListResponse resp = service.listOnlineUsers();

            assertEquals(1, resp.getTotalCount());
            assertEquals("jti-new", resp.getUsers().get(0).getTokenId());
            assertEquals("10.0.0.2", resp.getUsers().get(0).getIp());
        }

        @Test
        @DisplayName("应Keep不同Users")
        void shouldKeepDifferentUsers() {
            OnlineUserSnapshot a = new OnlineUserSnapshot(
                    "jti-a", 100L, "alice", "10.0.0.1",
                    LocalDateTime.now(), LocalDateTime.now(),
                    "Chrome", "macOS", 3600L);
            OnlineUserSnapshot b = new OnlineUserSnapshot(
                    "jti-b", 200L, "bob", "10.0.0.2",
                    LocalDateTime.now(), LocalDateTime.now(),
                    "Chrome", "Linux", 3600L);
            when(authClientFacade.listOnlineUsers()).thenReturn(List.of(a, b));

            OnlineUserListResponse resp = service.listOnlineUsers();

            assertEquals(2, resp.getTotalCount());
        }

        @Test
        @DisplayName("无用户ID时应Deduplicate通过用户名")
        void shouldDeduplicateByUsernameWhenNoUserId() {
            LocalDateTime older = LocalDateTime.now().minusHours(1);
            LocalDateTime newer = LocalDateTime.now();
            OnlineUserSnapshot oldSession = new OnlineUserSnapshot(
                    "jti-old", null, "anon", "10.0.0.1",
                    older, older, "Chrome", "macOS", 3600L);
            OnlineUserSnapshot newSession = new OnlineUserSnapshot(
                    "jti-new", null, "anon", "10.0.0.2",
                    newer, newer, "Firefox", "Windows", 3600L);
            when(authClientFacade.listOnlineUsers()).thenReturn(List.of(oldSession, newSession));

            OnlineUserListResponse resp = service.listOnlineUsers();

            assertEquals(1, resp.getTotalCount());
            assertEquals("jti-new", resp.getUsers().get(0).getTokenId());
        }
    }

    @Nested
    @DisplayName("forceLogout")
    class ForceLogout {

        @Test
        @DisplayName("应拒绝SelfKick")
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
            verify(authClientFacade, never()).forceLogout("self-jti");
        }

        @Test
        @DisplayName("应委托")
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
            verify(authClientFacade).forceLogout("other-jti");
        }

        @Test
        @DisplayName("使用out认证时应允许")
        void shouldAllowWithoutAuth() {
            service.forceLogout("any");
            verify(authClientFacade).forceLogout("any");
        }
    }
}
