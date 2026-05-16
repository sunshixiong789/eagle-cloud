package com.eagle.system.base.interfaces.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

/**
 * 登录日志统计响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginLogStatsResponse {

    /**
     * 今日登录总次数
     */
    private long todayTotal;

    /**
     * 今日登录失败次数
     */
    private long todayFail;

    /**
     * 今日登录独立用户数
     */
    private long todayUniqueUsers;

    /**
     * 分页日志列表
     */
    private Page<LogResponse> page;
}
