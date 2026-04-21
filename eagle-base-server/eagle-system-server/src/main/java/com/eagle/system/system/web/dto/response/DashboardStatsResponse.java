package com.eagle.system.system.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 仪表盘统计数据响应
 *
 * @author sunshixiong
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "仪表盘统计数据")
public class DashboardStatsResponse {

    /** 用户总数 */
    @Schema(description = "用户总数")
    private long userCount;

    /** 近7天新增用户数 */
    @Schema(description = "近7天新增用户数")
    private long userCountLast7Days;

    /** 角色总数 */
    @Schema(description = "角色总数")
    private long roleCount;

    /** 启用角色数 */
    @Schema(description = "启用角色数")
    private long roleEnabledCount;

    /** 今日登录次数 */
    @Schema(description = "今日登录次数")
    private long todayLoginCount;

    /** 今日登录与昨日相比的变化百分比 */
    @Schema(description = "今日登录与昨日相比的变化百分比")
    private double todayLoginVsYesterday;

    /** 今日日志总数 */
    @Schema(description = "今日日志总数")
    private long todayLogCount;

    /** 今日异常日志数 */
    @Schema(description = "今日异常日志数")
    private long todayExceptionCount;
}
