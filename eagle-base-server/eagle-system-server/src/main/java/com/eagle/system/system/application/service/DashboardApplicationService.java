package com.eagle.system.system.application.service;

import com.eagle.system.domain.model.enums.LogType;
import com.eagle.system.domain.model.enums.RoleStatus;
import com.eagle.system.domain.repository.LogRepository;
import com.eagle.system.domain.repository.LoginTrendProjection;
import com.eagle.system.domain.repository.RoleRepository;
import com.eagle.system.domain.repository.UserRepository;
import com.eagle.system.web.dto.response.DashboardStatsResponse;
import com.eagle.system.web.dto.response.LogSummaryItem;
import com.eagle.system.web.dto.response.LoginTrendItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 仪表盘应用服务
 *
 * @author sunshixiong
 */
@Service
@RequiredArgsConstructor
public class DashboardApplicationService {

    private static final int DEFAULT_TREND_DAYS = 30;
    private static final int MAX_TREND_DAYS = 90;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final LogRepository logRepository;

    /**
     * 获取仪表盘统计卡片数据
     *
     * @return 统计数据
     */
    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrowStart = todayStart.plusDays(1);
        LocalDateTime yesterdayStart = todayStart.minusDays(1);
        LocalDateTime sevenDaysAgo = todayStart.minusDays(7);

        long todayLogins = logRepository.countByLogTypeAndPeriod(LogType.LOGIN, todayStart, tomorrowStart);
        long yesterdayLogins = logRepository.countByLogTypeAndPeriod(LogType.LOGIN, yesterdayStart, todayStart);
        // 昨日为0时，视为增长100%；否则计算百分比变化，保留一位小数
        double vsYesterday = yesterdayLogins == 0 ? 100.0
            : Math.round((todayLogins - yesterdayLogins) * 1000.0 / yesterdayLogins) / 10.0;

        return DashboardStatsResponse.builder()
            .userCount(userRepository.count())
            .userCountLast7Days(userRepository.countByCreateTimeSince(sevenDaysAgo))
            .roleCount(roleRepository.count())
            .roleEnabledCount(roleRepository.countByStatus(RoleStatus.NORMAL))
            .todayLoginCount(todayLogins)
            .todayLoginVsYesterday(vsYesterday)
            .todayLogCount(logRepository.countByPeriod(todayStart, tomorrowStart))
            .todayExceptionCount(logRepository.countByLogTypeAndPeriod(LogType.EXCEPTION, todayStart, tomorrowStart))
            .build();
    }

    /**
     * 获取近 N 天登录趋势数据
     *
     * @param days 天数，最大 90 天
     * @return 每日登录趋势列表（按日期升序）
     */
    @Transactional(readOnly = true)
    public List<LoginTrendItem> getLoginTrend(int days) {
        int safeDays = Math.min(Math.max(days, 1), MAX_TREND_DAYS);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(safeDays - 1);

        List<LoginTrendProjection> projections = logRepository.findLoginTrendByPeriod(
            LogType.LOGIN,
            startDate.atStartOfDay(),
            endDate.plusDays(1).atStartOfDay());

        // Build a complete date sequence, filling zeros for days with no data
        Map<LocalDate, Long> byDate = projections.stream()
            .collect(Collectors.toMap(
                LoginTrendProjection::getDate,
                LoginTrendProjection::getCount));

        List<LoginTrendItem> trend = new ArrayList<>(safeDays);
        for (int i = 0; i < safeDays; i++) {
            LocalDate date = startDate.plusDays(i);
            trend.add(new LoginTrendItem(date.toString(), byDate.getOrDefault(date, 0L)));
        }
        return trend;
    }

    /**
     * 获取今日日志类型分布
     *
     * @return 日志类型统计列表
     */
    @Transactional(readOnly = true)
    public List<LogSummaryItem> getLogSummary() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        return logRepository.findLogSummaryByPeriod(todayStart, todayStart.plusDays(1))
            .stream()
            .map(p -> new LogSummaryItem(p.getLogType().name(), p.getCount()))
            .toList();
    }
}
