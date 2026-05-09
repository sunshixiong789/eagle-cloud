package com.eagle.audit.event;

import com.eagle.audit.model.AuditLogEntry;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.io.Serial;

/**
 * 审计日志 Spring 应用事件，由切面发布，由处理器异步消费。
 *
 * @author eagle
 */
@Getter
public class AuditLogEvent extends ApplicationEvent {
    @Serial
    private static final long serialVersionUID = 1L;

    private final AuditLogEntry entry;

    public AuditLogEvent(Object source, AuditLogEntry entry) {
        super(source);
        this.entry = entry;
    }
}
