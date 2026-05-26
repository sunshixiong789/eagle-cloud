package com.eagle.audit.repository;

import com.eagle.audit.model.AuditLogRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 审计日志 Repository。
 *
 * @author eagle
 */
@Repository
public interface AuditLogRepository
        extends JpaRepository<AuditLogRecord, Long>, JpaSpecificationExecutor<AuditLogRecord> {
}
