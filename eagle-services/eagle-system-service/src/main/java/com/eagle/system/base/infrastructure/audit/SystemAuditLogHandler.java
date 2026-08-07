package com.eagle.system.base.infrastructure.audit;

import com.eagle.audit.handler.AuditLogHandler;
import com.eagle.audit.model.AuditLogEntry;
import com.eagle.system.base.application.service.SystemLogApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 将 starter 审计事件桥接到中台系统日志表。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemAuditLogHandler implements AuditLogHandler {

    private final SystemLogApplicationService recorder;

    @Override
    public void handle(AuditLogEntry entry) {
        try {
            recorder.recordAudit(entry);
        } catch (RuntimeException ex) {
            log.warn("persist system operation log failed, module={}, action={}",
                    entry.getModule(), entry.getAction(), ex);
        }
    }
}
