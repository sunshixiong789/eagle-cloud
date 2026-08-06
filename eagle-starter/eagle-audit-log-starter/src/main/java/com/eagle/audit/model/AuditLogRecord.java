package com.eagle.audit.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 审计日志持久化实体(独立于 {@link AuditLogEntry} 传输 DTO)。
 *
 * <p>表名 {@code eagle_audit_log},每服务自己的数据库各持一份。
 * 字段与 {@link AuditLogEntry} 对齐,额外增加 {@code serviceId} 区分来源服务。
 *
 * <p>审计记录是 immutable 的,无 update 路径,因此不引入 {@code @Version} 乐观锁
 * 和 {@code updateBy/updateTime}。
 *
 * @author eagle
 */
@Entity
@Table(name = "eagle_audit_log", indexes = {
        @Index(name = "idx_audit_log_occurred_at", columnList = "occurred_at"),
        @Index(name = "idx_audit_log_operator", columnList = "operator_id, occurred_at"),
        @Index(name = "idx_audit_log_module", columnList = "module, occurred_at")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(comment = "主键 ID")
    private Long id;

    @Column(name = "service_id", length = 64, comment = "来源服务")
    private String serviceId;

    @Column(name = "operator_id", length = 64, comment = "操作者 ID")
    private String operatorId;

    @Column(name = "operator_name", length = 128, comment = "操作者名称")
    private String operatorName;

    @Column(length = 64, comment = "所属模块")
    private String module;

    @Column(length = 128, comment = "操作描述")
    private String action;

    @Column(name = "request_args", columnDefinition = "TEXT", comment = "请求参数 JSON")
    private String requestArgs;

    @Column(name = "response_data", columnDefinition = "TEXT", comment = "返回结果 JSON")
    private String responseData;

    @Column(name = "client_ip", length = 64, comment = "客户端 IP")
    private String clientIp;

    @Column(name = "user_agent", length = 512, comment = "User-Agent")
    private String userAgent;

    @Column(name = "cost_ms", comment = "执行耗时(毫秒)")
    private long costMs;

    @Column(nullable = false, comment = "是否成功")
    private boolean success;

    @Column(name = "error_message", columnDefinition = "TEXT", comment = "异常信息")
    private String errorMessage;

    @Column(name = "occurred_at", nullable = false, comment = "操作发生时间")
    private LocalDateTime occurredAt;

    /**
     * 从切面捕获的 {@link AuditLogEntry} 转换为持久化记录。
     *
     * @param entry     切面收集的 entry
     * @param serviceId 当前服务名(注入自 {@code spring.application.name})
     * @return 待持久化的实体
     */
    public static AuditLogRecord from(AuditLogEntry entry, String serviceId) {
        return AuditLogRecord.builder()
                .serviceId(serviceId)
                .operatorId(entry.getOperatorId())
                .operatorName(entry.getOperatorName())
                .module(entry.getModule())
                .action(entry.getAction())
                .requestArgs(entry.getRequestArgs())
                .responseData(entry.getResponseData())
                .clientIp(entry.getClientIp())
                .userAgent(entry.getUserAgent())
                .costMs(entry.getCostMs())
                .success(entry.isSuccess())
                .errorMessage(entry.getErrorMessage())
                .occurredAt(entry.getOccurredAt() != null ? entry.getOccurredAt() : LocalDateTime.now())
                .build();
    }
}
