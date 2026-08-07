package com.eagle.system.base.application.service;

import com.eagle.audit.model.AuditLogEntry;
import com.eagle.system.base.domain.model.SysLog;
import com.eagle.system.base.domain.model.enums.LogStatus;
import com.eagle.system.base.domain.repository.LogRepository;
import com.eagle.system.base.application.event.AuthLoginMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统日志记录器。
 */
@Slf4j
@Service
public class SystemLogApplicationService {

    private static final int TITLE_MAX_LENGTH = 255;
    private static final int USERNAME_MAX_LENGTH = 64;
    private static final int IP_MAX_LENGTH = 50;
    private static final int USER_AGENT_MAX_LENGTH = 500;
    private static final int SERVICE_ID_MAX_LENGTH = 64;

    private static final String AUTH_SERVICE_ID = "eagle-auth-service";
    private static final String EVENT_ID_UNIQUE_CONSTRAINT = "uk_sys_log_event_id";

    private final LogRepository logRepository;
    private final String serviceId;

    public SystemLogApplicationService(LogRepository logRepository, Environment environment) {
        this.logRepository = logRepository;
        this.serviceId = truncate(
                environment.getProperty("spring.application.name", "unknown"),
                SERVICE_ID_MAX_LENGTH);
    }

    /**
     * 将审计切面捕获的业务操作写入系统操作日志。
     *
     * @param entry 审计日志条目
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAudit(AuditLogEntry entry) {
        String title = joinTitle(entry.getModule(), entry.getAction());
        SysLog operationLog = SysLog.createOperationLog(
                truncate(title, TITLE_MAX_LENGTH),
                parseLong(entry.getOperatorId()),
                truncate(entry.getOperatorName(), USERNAME_MAX_LENGTH),
                truncate(entry.getClientIp(), IP_MAX_LENGTH),
                truncate(entry.getUserAgent(), USER_AGENT_MAX_LENGTH),
                entry.getRequestArgs(),
                entry.getResponseData(),
                entry.getCostMs(),
                entry.getErrorMessage(),
                serviceId,
                entry.isSuccess() ? LogStatus.SUCCESS : LogStatus.FAILURE);
        if (entry.getOccurredAt() != null) {
            operationLog.setCreateTime(entry.getOccurredAt());
        }
        logRepository.save(operationLog);
    }

    /**
     * 将 auth-service 登录集成事件写入系统登录日志。
     *
     * <p>幂等保障(Mode A):{@code sys_log.event_id} 唯一约束兜底 RocketMQ 至少一次重投递。
     * 唯一约束冲突时只跳过明确命中 {@code uk_sys_log_event_id} 的重复事件;其他约束冲突上抛,
     * 由 RocketMQ 重试 / DLQ 暴露真实故障。
     *
     * @param event 登录日志事件
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLogin(AuthLoginMessage event) {
        String eventId = event.getEventId();
        if (eventId != null && logRepository.existsByEventId(eventId)) {
            log.info("idempotent skip login log, eventId={}", eventId);
            return;
        }
        SysLog loginLog = SysLog.createLoginLog(
                event.isSuccess() ? "用户登录成功" : "用户登录失败",
                eventId,
                event.getAccountId(),
                truncate(event.getUsername(), USERNAME_MAX_LENGTH),
                truncate(event.getIp(), IP_MAX_LENGTH),
                truncate(event.getUserAgent(), USER_AGENT_MAX_LENGTH),
                event.getFailReason(),
                AUTH_SERVICE_ID,
                event.isSuccess() ? LogStatus.SUCCESS : LogStatus.FAILURE);
        if (event.getOccurredOn() != null) {
            loginLog.setCreateTime(event.getOccurredOn());
        }
        try {
            logRepository.save(loginLog);
        } catch (DataIntegrityViolationException ex) {
            if (eventId != null && isEventIdUniqueConstraintViolation(ex)) {
                log.info("idempotent skip login log on conflict, eventId={}", eventId);
                return;
            }
            throw ex;
        }
    }

    private String joinTitle(String module, String action) {
        if (module == null || module.isBlank()) {
            return action != null ? action : "系统操作";
        }
        if (action == null || action.isBlank()) {
            return module;
        }
        return module + " - " + action;
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            log.debug("audit operatorId not a Long, value={}", value);
            return null;
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private boolean isEventIdUniqueConstraintViolation(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains(EVENT_ID_UNIQUE_CONSTRAINT)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
