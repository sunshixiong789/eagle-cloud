package com.eagle.system.system.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/** 登录日志查询请求 */
@Data
@Schema(description = "登录日志查询请求")
public class LoginLogQueryRequest {

    @Schema(description = "用户名（模糊匹配）")
    private String username;

    @Schema(description = "IP 地址（模糊匹配）")
    private String ip;

    @Schema(description = "状态（SUCCESS/FAILURE）")
    private String status;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;
}
