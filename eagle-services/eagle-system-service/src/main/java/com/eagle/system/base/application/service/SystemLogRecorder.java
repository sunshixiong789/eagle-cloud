package com.eagle.system.base.application.service;

import com.eagle.audit.model.AuditLogEntry;
import com.eagle.system.base.domain.model.SysLog;
import com.eagle.system.base.domain.model.enums.LogStatus;
import com.eagle.system.base.domain.model.enums.LogType;
import com.eagle.system.base.domain.repository.LogRepository;
import com.eagle.system.base.infrastructure.messaging.event.AuthLoginMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统日志记录器。
 */
@Service
@RequiredArgsConstructor
public class SystemLogRecorder {

    private static final int TITLE_MAX_LENGTH = 255;
    private static final int USERNAME_MAX_LENGTH = 64;
    private static final int IP_MAX_LENGTH = 50;
    private static final int USER_AGENT_MAX_LENGTH = 500;
    private static final int URI_MAX_LENGTH = 500;
    private static final int METHOD_MAX_LENGTH = 10;
    private static final int SERVICE_ID_MAX_LENGTH = 64;

    private final LogRepository logRepository;

    @Value("${spring.application.name:unknown}")
    private String serviceId;

    /**
     * 将审计切面捕获的业务操作写入系统操作日志。
     *
     * @param entry 审计日志条目
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAudit(AuditLogEntry entry) {
        String title = joinTitle(entry.getModule(), entry.getAction());
        SysLog log = SysLog.create(
                LogType.OPERATION,
                truncate(title, TITLE_MAX_LENGTH),
                parseLong(entry.getOperatorId()),
                truncate(entry.getOperatorName(), USERNAME_MAX_LENGTH),
                truncate(entry.getClientIp(), IP_MAX_LENGTH),
                truncate(entry.getUserAgent(), USER_AGENT_MAX_LENGTH),
                null,
                null,
                entry.getRequestArgs(),
                entry.getResponseData(),
                entry.getCostMs(),
                entry.getErrorMessage(),
                truncate(serviceId, SERVICE_ID_MAX_LENGTH),
                entry.isSuccess() ? LogStatus.SUCCESS : LogStatus.FAILURE);
        if (entry.getOccurredAt() != null) {
            log.setCreateTime(entry.getOccurredAt());
        }
        logRepository.save(log);
    }

    /**
     * 将 auth-service 登录集成事件写入系统登录日志。
     *
     * @param event 登录日志事件
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLogin(AuthLoginMessage event) {
        SysLog log = SysLog.create(
                LogType.LOGIN,
                event.isSuccess() ? "用户登录成功" : "用户登录失败",
                event.getAccountId(),
                truncate(event.getUsername(), USERNAME_MAX_LENGTH),
                truncate(event.getIp(), IP_MAX_LENGTH),
                truncate(event.getUserAgent(), USER_AGENT_MAX_LENGTH),
                "/login",
                "POST",
                null,
                null,
                null,
                event.getFailReason(),
                "eagle-auth-service",
                event.isSuccess() ? LogStatus.SUCCESS : LogStatus.FAILURE);
        if (event.getOccurredOn() != null) {
            log.setCreateTime(event.getOccurredOn());
        }
        logRepository.save(log);
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
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
