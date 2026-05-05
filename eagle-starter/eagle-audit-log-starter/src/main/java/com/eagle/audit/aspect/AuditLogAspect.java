package com.eagle.audit.aspect;

import com.eagle.audit.annotation.AuditLog;
import com.eagle.audit.context.AuditLogUserProvider;
import com.eagle.audit.event.AuditLogEvent;
import com.eagle.audit.model.AuditLogEntry;
import com.eagle.audit.properties.AuditLogProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 审计日志切面，拦截 {@link AuditLog} 注解方法并发布 {@link AuditLogEvent}。
 *
 * @author eagle
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogProperties properties;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final AuditLogUserProvider userProvider;

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        long start = System.currentTimeMillis();
        AuditLogEntry.AuditLogEntryBuilder builder = AuditLogEntry.builder()
                .module(auditLog.module())
                .action(auditLog.action())
                .occurredAt(LocalDateTime.now());

        fillUserContext(builder);
        fillWebContext(builder);

        if (auditLog.logArgs()) {
            builder.requestArgs(serializeArgs(joinPoint.getArgs()));
        }

        Object result = null;
        boolean success = true;
        String errorMessage = null;
        try {
            result = joinPoint.proceed();
            if (auditLog.logResult() && result != null) {
                builder.responseData(truncate(serialize(result), properties.getMaxResultLength()));
            }
            return result;
        } catch (Throwable ex) {
            success = false;
            errorMessage = ex.getMessage();
            throw ex;
        } finally {
            AuditLogEntry entry = builder
                    .costMs(System.currentTimeMillis() - start)
                    .success(success)
                    .errorMessage(errorMessage)
                    .build();
            publishQuietly(entry);
        }
    }

    private void fillUserContext(AuditLogEntry.AuditLogEntryBuilder builder) {
        try {
            builder.operatorId(userProvider.getCurrentUserId())
                    .operatorName(userProvider.getCurrentUserName())
                    .tenantId(userProvider.getCurrentTenantId());
        } catch (Exception e) {
            log.debug("Failed to fill audit user context", e);
        }
    }

    private void fillWebContext(AuditLogEntry.AuditLogEntryBuilder builder) {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                builder.clientIp(getClientIp(request))
                        .userAgent(request.getHeader("User-Agent"));
            }
        } catch (Exception e) {
            log.debug("Failed to fill audit web context", e);
        }
    }

    private String serializeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        try {
            return truncate(objectMapper.writeValueAsString(args), properties.getMaxArgsLength());
        } catch (JsonProcessingException e) {
            return "[serialization failed]";
        }
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "[serialization failed]";
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...[truncated]";
    }

    private static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null && ip.contains(",") ? ip.split(",")[0].strip() : ip;
    }

    private void publishQuietly(AuditLogEntry entry) {
        try {
            eventPublisher.publishEvent(new AuditLogEvent(this, entry));
        } catch (Exception e) {
            log.warn("Failed to publish audit log event", e);
        }
    }
}
