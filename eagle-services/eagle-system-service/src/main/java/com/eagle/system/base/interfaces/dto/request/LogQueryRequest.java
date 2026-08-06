package com.eagle.system.base.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 日志查询请求
 *
 * @param logType    日志类型
 * @param status     日志状态
 * @param username   用户名
 * @param requestUri 请求 URI
 * @param remoteAddr 请求 IP（模糊匹配）
 * @param startTime  开始时间
 * @param endTime    结束时间
 * @author sunshixiong
 */
public record LogQueryRequest(

        String logType,

        String status,

        String username,

        String requestUri,

        @Schema(description = "请求IP（模糊匹配）")
        String remoteAddr,

        LocalDateTime startTime,

        LocalDateTime endTime
) {
}
