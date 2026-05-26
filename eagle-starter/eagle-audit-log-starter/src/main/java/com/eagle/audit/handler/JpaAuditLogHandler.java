package com.eagle.audit.handler;

import com.eagle.audit.model.AuditLogEntry;
import com.eagle.audit.model.AuditLogRecord;
import com.eagle.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JPA 持久化 {@link AuditLogHandler} 实现。
 *
 * <p>消费方引入 {@code spring-boot-starter-data-jpa} 后自动启用,
 * 覆盖默认的 {@link LoggingAuditLogHandler}。
 *
 * <p>写库失败仅 log.warn 不抛出,避免审计故障影响主业务。
 *
 * @author eagle
 */
@Slf4j
@RequiredArgsConstructor
public class JpaAuditLogHandler implements AuditLogHandler {

    private final AuditLogRepository repository;
    private final String serviceId;

    @Override
    public void handle(AuditLogEntry entry) {
        try {
            repository.save(AuditLogRecord.from(entry, serviceId));
        } catch (Exception e) {
            log.warn("persist audit log failed, module={}, action={}",
                    entry.getModule(), entry.getAction(), e);
        }
    }
}
