package com.eagle.audit.handler;

import com.eagle.audit.model.AuditLogEntry;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认审计日志处理器：将条目输出到 SLF4J 日志。
 *
 * <p>适用于开发调试阶段；生产环境应注册自定义 {@link AuditLogHandler} Bean 将条目写入数据库。
 *
 * @author eagle
 */
@Slf4j
public class LoggingAuditLogHandler implements AuditLogHandler {

    @Override
    public void handle(AuditLogEntry entry) {
        log.info("[AUDIT] operator={}, module={}, action={}, success={}, costMs={}, ip={}",
                entry.getOperatorId(), entry.getModule(), entry.getAction(),
                entry.isSuccess(), entry.getCostMs(), entry.getClientIp());
    }
}
