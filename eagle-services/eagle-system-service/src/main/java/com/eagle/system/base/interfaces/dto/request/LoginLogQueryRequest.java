package com.eagle.system.base.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 登录日志查询请求
 */
@Schema(description = "登录日志查询请求")
public record LoginLogQueryRequest(

        @Schema(description = "用户名（模糊匹配）")
        String username,

        @Schema(description = "IP 地址（模糊匹配）")
        String ip,

        @Schema(description = "状态（SUCCESS/FAILURE）")
        String status,

        @Schema(description = "开始时间")
        LocalDateTime startTime,

        @Schema(description = "结束时间")
        LocalDateTime endTime
) {
}
