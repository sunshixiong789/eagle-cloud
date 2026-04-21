package com.eagle.system.application.service;

import com.eagle.auth.domain.port.OnlineUserInfo;
import com.eagle.auth.domain.port.OnlineUserPort;
import com.eagle.common.exception.DomainException;
import com.eagle.system.domain.model.enums.LogStatus;
import com.eagle.system.domain.model.enums.LogType;
import com.eagle.system.domain.repository.LogRepository;
import com.eagle.system.web.dto.request.LoginLogQueryRequest;
import com.eagle.system.web.dto.response.LoginLogStatsResponse;
import com.eagle.system.web.dto.response.OnlineUserListResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitorApplicationServiceTest {

    private static final String TOKEN_ID = "jti-abc123";
    private static final Long USER_ID = 1L;
    private static final String USERNAME = "admin";

    @Mock
    private OnlineUserPort onlineUserPort;
    @Mock
    private LogApplicationService logApplicationService;
    @Mock
    private LogRepository logRepository;
    @InjectMocks
    private MonitorApplicationService service;

    @Nested
    @DisplayName("listOnlineUsers")
    class ListOnlineUsers {

        @Test
        @DisplayName("should return mapped online user list")
        void shouldReturnMappedOnlineUserList() {
            OnlineUserInfo info = new OnlineUserInfo(
                    TOKEN_ID, USER_ID, USERNAME, "127.0.0.1",
                    LocalDateTime.now(), LocalDateTime.now(), "Chrome", "macOS", 3600L
            );
            when(onlineUserPort.listOnlineUsers()).thenReturn(List.of(info));

            OnlineUserListResponse result = service.listOnlineUsers();

            assertThat(result.getTotalCount()).isEqualTo(1);
            assertThat(result.getUsers()).hasSize(1);
            assertThat(result.getUsers().get(0).getUsername()).isEqualTo(USERNAME);
            assertThat(result.getUsers().get(0).getTokenId()).isEqualTo(TOKEN_ID);
        }

        @Test
        @DisplayName("should return empty list when no online users")
        void shouldReturnEmptyWhenNoOnlineUsers() {
            when(onlineUserPort.listOnlineUsers()).thenReturn(List.of());

            OnlineUserListResponse result = service.listOnlineUsers();

            assertThat(result.getTotalCount()).isZero();
            assertThat(result.getUsers()).isEmpty();
        }
    }

    @Nested
    @DisplayName("forceLogout")
    class ForceLogout {

        @Test
        @DisplayName("should delegate to port when token is not current user's")
        void shouldDelegateToPortForOtherToken() {
            SecurityContextHolder.clearContext();

            service.forceLogout("other-token-id");

            verify(onlineUserPort).forceLogout("other-token-id");
        }

        @Test
        @DisplayName("should throw DomainException when token is current user's own")
        void shouldThrowWhenForcingOwnLogout() {
            Jwt jwt = mock(Jwt.class);
            when(jwt.getId()).thenReturn(TOKEN_ID);

            Authentication auth = mock(Authentication.class);
            when(auth.getCredentials()).thenReturn(jwt);

            SecurityContext ctx = mock(SecurityContext.class);
            when(ctx.getAuthentication()).thenReturn(auth);
            SecurityContextHolder.setContext(ctx);

            try {
                assertThatThrownBy(() -> service.forceLogout(TOKEN_ID))
                        .isInstanceOf(DomainException.class);
            } finally {
                SecurityContextHolder.clearContext();
            }
        }
    }

    @Nested
    @DisplayName("queryLoginLogs")
    class QueryLoginLogs {

        @Test
        @DisplayName("should return login stats with today counts")
        void shouldReturnLoginStats() {
            when(logRepository.countByLogTypeAndPeriod(eq(LogType.LOGIN), any(), any()))
                    .thenReturn(50L);
            when(logRepository.countByLogTypeAndStatusAndPeriod(
                    eq(LogType.LOGIN), eq(LogStatus.FAILURE), any(), any()))
                    .thenReturn(5L);
            when(logRepository.countDistinctUsernameByLogTypeAndPeriod(
                    eq(LogType.LOGIN), any(), any()))
                    .thenReturn(30L);
            when(logApplicationService.queryLogs(any(), any())).thenReturn(Page.empty());

            LoginLogStatsResponse result = service.queryLoginLogs(
                    new LoginLogQueryRequest(), Pageable.unpaged());

            assertThat(result.getTodayTotal()).isEqualTo(50L);
            assertThat(result.getTodayFail()).isEqualTo(5L);
            assertThat(result.getTodayUniqueUsers()).isEqualTo(30L);
            assertThat(result.getPage()).isEmpty();
        }

        @Test
        @DisplayName("should pass username and ip filter to log query")
        void shouldPassFiltersToLogQuery() {
            when(logRepository.countByLogTypeAndPeriod(any(), any(), any())).thenReturn(0L);
            when(logRepository.countByLogTypeAndStatusAndPeriod(any(), any(), any(), any()))
                    .thenReturn(0L);
            when(logRepository.countDistinctUsernameByLogTypeAndPeriod(any(), any(), any()))
                    .thenReturn(0L);
            when(logApplicationService.queryLogs(any(), any())).thenReturn(Page.empty());

            LoginLogQueryRequest request = new LoginLogQueryRequest();
            request.setUsername("testuser");
            request.setIp("192.168.1.1");

            service.queryLoginLogs(request, Pageable.unpaged());

            verify(logApplicationService).queryLogs(any(), any());
        }
    }
}
