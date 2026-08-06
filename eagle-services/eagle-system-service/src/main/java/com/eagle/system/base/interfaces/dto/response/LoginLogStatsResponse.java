package com.eagle.system.base.interfaces.dto.response;

import org.springframework.data.domain.Page;

/**
 * 登录日志统计响应
 *
 * @param todayTotal       今日登录总次数
 * @param todayFail        今日登录失败次数
 * @param todayUniqueUsers 今日登录独立用户数
 * @param page             分页日志列表（前端友好字段）
 */
public record LoginLogStatsResponse(
        long todayTotal,
        long todayFail,
        long todayUniqueUsers,
        Page<LoginLogItemResponse> page
) {
}
