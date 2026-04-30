package com.eagle.system.base.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 日志查询请求
 *
 * @author sunshixiong
 */
@Data
public class LogQueryRequest {

    /** 日志类型 */
    private String logType;

    /** 日志状态 */
    private String status;

    /** 用户名 */
    private String username;

    /** 请求URI */
    private String requestUri;

    /** 请求IP（模糊匹配） */
    @Schema(description = "请求IP（模糊匹配）")
    private String remoteAddr;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;
}
