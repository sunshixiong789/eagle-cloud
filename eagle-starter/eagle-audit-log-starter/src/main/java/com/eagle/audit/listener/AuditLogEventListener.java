package com.eagle.audit.listener;

import com.eagle.audit.event.AuditLogEvent;
import com.eagle.audit.handler.AuditLogHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

/**
 * 审计日志事件监听器，异步调用 {@link AuditLogHandler} 处理日志条目。
 *
 * <p>异步发布确保不影响主业务线程性能；若 {@code @Async} 失败则直接丢弃日志（审计不干扰业务）。
 *
 * @author eagle
 */
@Slf4j
@RequiredArgsConstructor
public class AuditLogEventListener {

    private final AuditLogHandler auditLogHandler;

    @Async
    @EventListener
    public void onAuditLogEvent(AuditLogEvent event) {
        try {
            auditLogHandler.handle(event.getEntry());
        } catch (Exception e) {
            log.warn("AuditLogHandler failed silently", e);
        }
    }
}
