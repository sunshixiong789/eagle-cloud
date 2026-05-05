package com.eagle.audit.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计日志条目。
 *
 * @author eagle
 */
@Data
@Builder
public class AuditLogEntry {

    /** 操作者 ID。 */
    private String operatorId;

    /** 操作者名称。 */
    private String operatorName;

    /** 租户 ID。 */
    private String tenantId;

    /** 所属模块。 */
    private String module;

    /** 操作描述。 */
    private String action;

    /** 请求参数（JSON）。 */
    private String requestArgs;

    /** 返回结果（JSON）。 */
    private String responseData;

    /** 客户端 IP。 */
    private String clientIp;

    /** User-Agent。 */
    private String userAgent;

    /** 操作耗时（毫秒）。 */
    private long costMs;

    /** 操作是否成功。 */
    private boolean success;

    /** 异常信息（失败时）。 */
    private String errorMessage;

    /** 操作发生时间。 */
    private LocalDateTime occurredAt;
}
