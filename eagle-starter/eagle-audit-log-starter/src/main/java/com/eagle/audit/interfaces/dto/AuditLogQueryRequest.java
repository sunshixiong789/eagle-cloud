package com.eagle.audit.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计日志查询请求 DTO。
 *
 * @author eagle
 */
@Data
@Schema(description = "审计日志查询条件")
public class AuditLogQueryRequest {

    @Schema(description = "所属模块", example = "用户管理")
    private String module;

    @Schema(description = "操作描述(模糊)", example = "删除")
    private String action;

    @Schema(description = "操作者 ID", example = "1024")
    private String operatorId;

    @Schema(description = "操作者名称(模糊)", example = "alice")
    private String operatorName;

    @Schema(description = "租户 ID", example = "t-default")
    private String tenantId;

    @Schema(description = "是否成功", example = "true")
    private Boolean success;

    @Schema(description = "开始时间(含)")
    private LocalDateTime startTime;

    @Schema(description = "结束时间(不含)")
    private LocalDateTime endTime;
}
