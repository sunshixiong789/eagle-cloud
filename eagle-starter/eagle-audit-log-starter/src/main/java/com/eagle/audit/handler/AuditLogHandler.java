package com.eagle.audit.handler;

import com.eagle.audit.model.AuditLogEntry;

/**
 * 审计日志持久化接口。
 *
 * <p>业务方实现此接口，将 {@link AuditLogEntry} 写入数据库、消息队列或外部系统。
 * 默认实现为 {@link LoggingAuditLogHandler}（打印到日志），可通过注册自定义 Bean 替换。
 *
 * @author eagle
 */
public interface AuditLogHandler {

    /**
     * 处理审计日志条目。
     *
     * @param entry 审计日志条目
     */
    void handle(AuditLogEntry entry);
}
