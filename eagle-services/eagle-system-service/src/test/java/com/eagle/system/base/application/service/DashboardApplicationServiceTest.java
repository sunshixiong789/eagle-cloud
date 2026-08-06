package com.eagle.system.base.application.service;

import com.eagle.system.base.domain.model.enums.LogType;
import com.eagle.system.base.domain.model.enums.RoleStatus;
import com.eagle.system.base.domain.repository.LogRepository;
import com.eagle.system.base.domain.repository.LoginTrendProjection;
import com.eagle.system.base.domain.repository.RoleRepository;
import com.eagle.system.base.domain.repository.UserRepository;
import com.eagle.system.base.infrastructure.config.AdminProperties;
import com.eagle.system.base.infrastructure.remote.AuthClientFacade;
import com.eagle.system.base.interfaces.dto.response.DashboardStatsResponse;
import com.eagle.system.base.interfaces.dto.response.LoginTrendItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardApplicationServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    RoleRepository roleRepository;
    @Mock
    LogRepository logRepository;
    @Mock
    MonitorApplicationService monitorApplicationService;
    @Mock
    AuthClientFacade authClientFacade;
    @Mock
    AdminProperties adminProperties;
    @InjectMocks
    DashboardApplicationService service;

    // ----- Mockito helpers for shorter call sites -----
    private static <T> T mock(Class<T> type) {
        return org.mockito.Mockito.mock(type);
    }

    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }

    @Nested
    @DisplayName("getStats")
    class GetStats {
        @Test
        @DisplayName("应Aggregate")
        void shouldAggregate() {
            when(adminProperties.getUsername()).thenReturn("admin");
            // userCount 取 auth 权威源(>=0 不走 fallback)
            when(authClientFacade.countAccounts()).thenReturn(50L);
            when(userRepository.countByCreateTimeSinceAndUsernameNot(any(), eq("admin"))).thenReturn(5L);
            when(roleRepository.count()).thenReturn(8L);
            when(roleRepository.countByStatus(RoleStatus.NORMAL)).thenReturn(6L);
            // today=20 vs yesterday=10 → +100.0%
            when(logRepository.countByLogTypeAndPeriod(eq(LogType.LOGIN), any(), any()))
                    .thenReturn(20L, 10L);
            when(logRepository.countByLogTypeAndPeriod(eq(LogType.EXCEPTION), any(), any()))
                    .thenReturn(1L);
            when(logRepository.countByPeriod(any(), any())).thenReturn(200L);
            when(monitorApplicationService.countOnlineUsers()).thenReturn(3L);

            DashboardStatsResponse stats = service.getStats();

            assertEquals(50L, stats.userCount());
            assertEquals(5L, stats.userCountLast7Days());
            assertEquals(8L, stats.roleCount());
            assertEquals(20L, stats.todayLoginCount());
            assertEquals(100.0, stats.todayLoginVsYesterday());
            assertEquals(200L, stats.todayLogCount());
            assertEquals(1L, stats.todayExceptionCount());
            assertEquals(3L, stats.onlineUserCount());
        }

        @Test
        @DisplayName("auth facade 降级返回 -1 时,userCount 回退 base_user 兜底")
        void shouldFallbackToLocalCountWhenAuthDown() {
            when(adminProperties.getUsername()).thenReturn("admin");
            when(authClientFacade.countAccounts()).thenReturn(-1L);
            when(userRepository.countByUsernameNot("admin")).thenReturn(42L);
            when(userRepository.countByCreateTimeSinceAndUsernameNot(any(), eq("admin"))).thenReturn(0L);
            when(roleRepository.count()).thenReturn(0L);
            when(roleRepository.countByStatus(any())).thenReturn(0L);
            when(logRepository.countByLogTypeAndPeriod(eq(LogType.LOGIN), any(), any()))
                    .thenReturn(0L, 0L);
            when(logRepository.countByLogTypeAndPeriod(eq(LogType.EXCEPTION), any(), any()))
                    .thenReturn(0L);
            when(logRepository.countByPeriod(any(), any())).thenReturn(0L);
            when(monitorApplicationService.countOnlineUsers()).thenReturn(0L);

            DashboardStatsResponse stats = service.getStats();
            assertEquals(42L, stats.userCount());
        }

        @Test
        @DisplayName("应Handle零Yesterday")
        void shouldHandleZeroYesterday() {
            when(adminProperties.getUsername()).thenReturn("admin");
            when(authClientFacade.countAccounts()).thenReturn(0L);
            when(userRepository.countByCreateTimeSinceAndUsernameNot(any(), eq("admin"))).thenReturn(0L);
            when(roleRepository.count()).thenReturn(0L);
            when(roleRepository.countByStatus(any())).thenReturn(0L);
            when(logRepository.countByLogTypeAndPeriod(eq(LogType.LOGIN), any(), any()))
                    .thenReturn(5L, 0L);
            when(logRepository.countByLogTypeAndPeriod(eq(LogType.EXCEPTION), any(), any()))
                    .thenReturn(0L);
            when(logRepository.countByPeriod(any(), any())).thenReturn(0L);
            when(monitorApplicationService.countOnlineUsers()).thenReturn(0L);

            DashboardStatsResponse stats = service.getStats();
            assertEquals(100.0, stats.todayLoginVsYesterday());
            assertEquals(0L, stats.onlineUserCount());
        }
    }

    @Nested
    @DisplayName("getLoginTrend")
    class LoginTrend {
        @Test
        @DisplayName("应限制负数Days")
        void shouldClampNegativeDays() {
            when(logRepository.findLoginTrendByPeriod(any(), any(), any())).thenReturn(List.of());
            List<LoginTrendItem> trend = service.getLoginTrend(-5);
            assertEquals(1, trend.size());
        }

        @Test
        @DisplayName("应限制TooMany")
        void shouldClampTooMany() {
            when(logRepository.findLoginTrendByPeriod(any(), any(), any())).thenReturn(List.of());
            List<LoginTrendItem> trend = service.getLoginTrend(500);
            assertEquals(90, trend.size());
        }

        @Test
        @DisplayName("应填充Zeros")
        void shouldFillZeros() {
            when(logRepository.findLoginTrendByPeriod(any(), any(), any())).thenReturn(List.of());
            List<LoginTrendItem> trend = service.getLoginTrend(3);
            assertEquals(3, trend.size());
            assertEquals(0L, trend.get(0).count());
        }

        @Test
        @DisplayName("应保留ProjectionCounts")
        void shouldPreserveProjectionCounts() {
            LocalDate today = LocalDate.now();
            LoginTrendProjection p = mock(LoginTrendProjection.class);
            when(p.getDate()).thenReturn(today);
            when(p.getCount()).thenReturn(7L);
            when(logRepository.findLoginTrendByPeriod(any(), any(), any())).thenReturn(List.of(p));

            List<LoginTrendItem> trend = service.getLoginTrend(3);
            assertEquals(7L, trend.get(2).count());
        }
    }
}
