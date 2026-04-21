package com.eagle.system.system.web.controller;

import com.eagle.system.application.service.DashboardApplicationService;
import com.eagle.system.web.dto.response.DashboardStatsResponse;
import com.eagle.system.web.dto.response.LogSummaryItem;
import com.eagle.system.web.dto.response.LoginTrendItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 仪表盘统计接口
 *
 * @author sunshixiong
 */
@Validated
@Tag(name = "仪表盘")
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardApplicationService dashboardService;

    /**
     * 获取统计卡片数据
     *
     * @return 仪表盘统计数据
     */
    @Operation(summary = "获取统计卡片数据")
    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public DashboardStatsResponse getStats() {
        return dashboardService.getStats();
    }

    /**
     * 获取登录趋势（近N天）
     *
     * @param days 天数，默认30天，最大90天
     * @return 每日登录趋势列表
     */
    @Operation(summary = "获取登录趋势（近N天）")
    @GetMapping("/login-trend")
    @PreAuthorize("isAuthenticated()")
    public List<LoginTrendItem> getLoginTrend(
            @RequestParam(defaultValue = "30") @Min(1) @Max(90) int days) {
        return dashboardService.getLoginTrend(days);
    }

    /**
     * 获取今日日志类型分布
     *
     * @return 今日各类型日志数量
     */
    @Operation(summary = "获取今日日志类型分布")
    @GetMapping("/log-summary")
    @PreAuthorize("isAuthenticated()")
    public List<LogSummaryItem> getLogSummary() {
        return dashboardService.getLogSummary();
    }
}
