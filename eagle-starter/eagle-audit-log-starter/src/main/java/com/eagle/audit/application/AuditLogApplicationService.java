package com.eagle.audit.application;

import com.eagle.audit.interfaces.dto.AuditLogQueryRequest;
import com.eagle.audit.interfaces.dto.AuditLogResponse;
import com.eagle.audit.model.AuditLogRecord;
import com.eagle.audit.repository.AuditLogRepository;
import com.eagle.audit.repository.AuditLogSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 审计日志应用服务:分页查询 + 详情。
 *
 * @author eagle
 */
@RequiredArgsConstructor
public class AuditLogApplicationService {

    private final AuditLogRepository repository;

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> query(AuditLogQueryRequest request, Pageable pageable) {
        Specification<AuditLogRecord> spec = Specification
                .where(AuditLogSpecification.moduleEquals(request.getModule()))
                .and(AuditLogSpecification.actionLike(request.getAction()))
                .and(AuditLogSpecification.operatorIdEquals(request.getOperatorId()))
                .and(AuditLogSpecification.operatorNameLike(request.getOperatorName()))
                .and(AuditLogSpecification.tenantIdEquals(request.getTenantId()))
                .and(AuditLogSpecification.successEquals(request.getSuccess()))
                .and(AuditLogSpecification.occurredBetween(request.getStartTime(), request.getEndTime()));
        return repository.findAll(spec, pageable).map(AuditLogResponse::from);
    }

    @Transactional(readOnly = true)
    public Optional<AuditLogResponse> getById(Long id) {
        return repository.findById(id).map(AuditLogResponse::from);
    }
}
