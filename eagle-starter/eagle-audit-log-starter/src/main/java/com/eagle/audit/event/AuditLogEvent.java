package com.eagle.audit.event;

import com.eagle.audit.model.AuditLogEntry;
import org.springframework.context.ApplicationEvent;

/**
 * 审计日志 Spring 应用事件，由切面发布，由处理器异步消费。
 *
 * @author eagle
 */
public class AuditLogEvent extends ApplicationEvent {

    private final AuditLogEntry entry;

    public AuditLogEvent(Object source, AuditLogEntry entry) {
        super(source);
        this.entry = entry;
    }

    public AuditLogEntry getEntry() {
        return entry;
    }
}
