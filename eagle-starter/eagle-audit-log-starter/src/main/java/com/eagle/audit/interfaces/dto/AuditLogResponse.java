package com.eagle.audit.interfaces.dto;

import com.eagle.audit.model.AuditLogRecord;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 审计日志响应 DTO。
 *
 * @author eagle
 */
@Schema(description = "审计日志条目")
public record AuditLogResponse(
        @Schema(description = "记录 ID") Long id,
        @Schema(description = "来源服务") String serviceId,
        @Schema(description = "操作者 ID") String operatorId,
        @Schema(description = "操作者名称") String operatorName,
        @Schema(description = "所属模块") String module,
        @Schema(description = "操作描述") String action,
        @Schema(description = "请求参数(JSON,可能截断)") String requestArgs,
        @Schema(description = "返回结果(JSON,可能截断)") String responseData,
        @Schema(description = "客户端 IP") String clientIp,
        @Schema(description = "User-Agent") String userAgent,
        @Schema(description = "执行耗时(毫秒)") long costMs,
        @Schema(description = "是否成功") boolean success,
        @Schema(description = "异常信息") String errorMessage,
        @Schema(description = "操作发生时间") LocalDateTime occurredAt
) {

    public static AuditLogResponse from(AuditLogRecord record) {
        return new AuditLogResponse(
                record.getId(),
                record.getServiceId(),
                record.getOperatorId(),
                record.getOperatorName(),
                record.getModule(),
                record.getAction(),
                record.getRequestArgs(),
                record.getResponseData(),
                record.getClientIp(),
                record.getUserAgent(),
                record.getCostMs(),
                record.isSuccess(),
                record.getErrorMessage(),
                record.getOccurredAt()
        );
    }
}
