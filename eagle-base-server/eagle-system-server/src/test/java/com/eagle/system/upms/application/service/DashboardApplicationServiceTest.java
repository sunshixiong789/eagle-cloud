package com.eagle.system.application.service;

import com.eagle.system.domain.model.enums.LogType;
import com.eagle.system.domain.model.enums.RoleStatus;
import com.eagle.system.domain.repository.LogRepository;
import com.eagle.system.domain.repository.LogTypeSummaryProjection;
import com.eagle.system.domain.repository.LoginTrendProjection;
import com.eagle.system.domain.repository.RoleRepository;
import com.eagle.system.domain.repository.UserRepository;
import com.eagle.system.web.dto.response.DashboardStatsResponse;
import com.eagle.system.web.dto.response.LoginTrendItem;
import com.eagle.system.web.dto.response.LogSummaryItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardApplicationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private LogRepository logRepository;
    @InjectMocks private DashboardApplicationService service;

    @Nested
    @DisplayName("getStats")
    class GetStats {

        @Test
        @DisplayName("should return aggregated stats")
        void shouldReturnAggregatedStats() {
            when(userRepository.count()).thenReturn(100L);
            when(userRepository.countByCreateTimeSince(any())).thenReturn(5L);
            when(roleRepository.count()).thenReturn(10L);
            when(roleRepository.countByStatus(RoleStatus.NORMAL)).thenReturn(8L);
            when(logRepository.countByLogTypeAndPeriod(eq(LogType.LOGIN), any(), any()))
                .thenReturn(50L, 40L); // today, yesterday
            when(logRepository.countByPeriod(any(), any())).thenReturn(200L);
            when(logRepository.countByLogTypeAndPeriod(eq(LogType.EXCEPTION), any(), any()))
                .thenReturn(3L);

            DashboardStatsResponse stats = service.getStats();

            assertThat(stats.getUserCount()).isEqualTo(100L);
            assertThat(stats.getUserCountLast7Days()).isEqualTo(5L);
            assertThat(stats.getRoleCount()).isEqualTo(10L);
            assertThat(stats.getRoleEnabledCount()).isEqualTo(8L);
            assertThat(stats.getTodayLogCount()).isEqualTo(200L);
            assertThat(stats.getTodayLoginCount()).isEqualTo(50L);
            assertThat(stats.getTodayLoginVsYesterday()).isEqualTo(25.0);  // (50-40)/40 * 100 = 25%
            assertThat(stats.getTodayExceptionCount()).isEqualTo(3L);
        }

        @Test
        @DisplayName("should return 100.0 vsYesterday when yesterday has no logins")
        void shouldReturn100WhenYesterdayIsZero() {
            when(userRepository.count()).thenReturn(0L);
            when(userRepository.countByCreateTimeSince(any())).thenReturn(0L);
            when(roleRepository.count()).thenReturn(0L);
            when(roleRepository.countByStatus(RoleStatus.NORMAL)).thenReturn(0L);
            when(logRepository.countByLogTypeAndPeriod(eq(LogType.LOGIN), any(), any()))
                .thenReturn(10L, 0L); // today=10, yesterday=0
            when(logRepository.countByPeriod(any(), any())).thenReturn(0L);

            DashboardStatsResponse stats = service.getStats();

            assertThat(stats.getTodayLoginVsYesterday()).isEqualTo(100.0);
        }
    }

    @Nested
    @DisplayName("getLoginTrend")
    class GetLoginTrend {

        @Test
        @DisplayName("should return trend for specified days")
        void shouldReturnTrendForSpecifiedDays() {
            when(logRepository.findLoginTrendByPeriod(eq(LogType.LOGIN), any(), any()))
                .thenReturn(List.of());

            List<LoginTrendItem> trend = service.getLoginTrend(7);

            assertThat(trend).hasSize(7);
            assertThat(trend.get(0).getCount()).isEqualTo(0L);
        }

        @Test
        @DisplayName("should cap days at 90")
        void shouldCapDaysAt90() {
            when(logRepository.findLoginTrendByPeriod(any(), any(), any())).thenReturn(List.of());
            List<LoginTrendItem> trend = service.getLoginTrend(200);
            assertThat(trend).hasSize(90);
        }
    }

    @Nested
    @DisplayName("getLogSummary")
    class GetLogSummary {

        @Test
        @DisplayName("should return log type summary for today")
        void shouldReturnLogTypeSummaryForToday() {
            LogTypeSummaryProjection proj = mock(LogTypeSummaryProjection.class);
            when(proj.getLogType()).thenReturn(LogType.LOGIN);
            when(proj.getCount()).thenReturn(100L);
            when(logRepository.findLogSummaryByPeriod(any(), any())).thenReturn(List.of(proj));

            List<LogSummaryItem> summary = service.getLogSummary();

            assertThat(summary).hasSize(1);
            assertThat(summary.get(0).getLogType()).isEqualTo("LOGIN");
            assertThat(summary.get(0).getCount()).isEqualTo(100L);
        }
    }
}
