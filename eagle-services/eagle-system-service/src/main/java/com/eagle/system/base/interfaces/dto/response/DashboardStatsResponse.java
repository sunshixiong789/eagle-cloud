package com.eagle.system.base.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仪表盘统计数据响应
 *
 * @author sunshixiong
 */
@Schema(description = "仪表盘统计数据")
public record DashboardStatsResponse(

        @Schema(description = "用户总数")
        long userCount,

        @Schema(description = "近7天新增用户数")
        long userCountLast7Days,

        @Schema(description = "角色总数")
        long roleCount,

        @Schema(description = "启用角色数")
        long roleEnabledCount,

        @Schema(description = "今日登录次数")
        long todayLoginCount,

        @Schema(description = "今日登录与昨日相比的变化百分比")
        double todayLoginVsYesterday,

        @Schema(description = "今日日志总数")
        long todayLogCount,

        @Schema(description = "今日异常日志数")
        long todayExceptionCount,

        @Schema(description = "当前在线用户数（按账号去重）")
        long onlineUserCount
) {
}
