package com.eagle.audit.repository;

import com.eagle.audit.model.AuditLogRecord;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/**
 * 审计日志查询规格(Specification Pattern)。
 *
 * <p>null 条件自动忽略,支持组合 AND。
 *
 * @author eagle
 */
public final class AuditLogSpecification {

    private AuditLogSpecification() {
    }

    public static Specification<AuditLogRecord> moduleEquals(String module) {
        return (root, query, cb) -> (module == null || module.isBlank())
                ? null : cb.equal(root.get("module"), module);
    }

    public static Specification<AuditLogRecord> actionLike(String action) {
        return (root, query, cb) -> (action == null || action.isBlank())
                ? null : cb.like(root.get("action"), "%" + action + "%");
    }

    public static Specification<AuditLogRecord> operatorIdEquals(String operatorId) {
        return (root, query, cb) -> (operatorId == null || operatorId.isBlank())
                ? null : cb.equal(root.get("operatorId"), operatorId);
    }

    public static Specification<AuditLogRecord> operatorNameLike(String operatorName) {
        return (root, query, cb) -> (operatorName == null || operatorName.isBlank())
                ? null : cb.like(root.get("operatorName"), "%" + operatorName + "%");
    }

    public static Specification<AuditLogRecord> tenantIdEquals(String tenantId) {
        return (root, query, cb) -> (tenantId == null || tenantId.isBlank())
                ? null : cb.equal(root.get("tenantId"), tenantId);
    }

    public static Specification<AuditLogRecord> successEquals(Boolean success) {
        return (root, query, cb) -> success == null
                ? null : cb.equal(root.get("success"), success);
    }

    public static Specification<AuditLogRecord> occurredBetween(LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> {
            if (start == null && end == null) {
                return null;
            }
            if (start != null && end != null) {
                return cb.between(root.get("occurredAt"), start, end);
            }
            if (start != null) {
                return cb.greaterThanOrEqualTo(root.get("occurredAt"), start);
            }
            return cb.lessThanOrEqualTo(root.get("occurredAt"), end);
        };
    }
}
