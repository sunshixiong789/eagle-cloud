package com.eagle.system.base.application.service;

import com.eagle.audit.model.AuditLogEntry;
import com.eagle.system.base.domain.model.SysLog;
import com.eagle.system.base.domain.model.enums.LogStatus;
import com.eagle.system.base.domain.model.enums.LogType;
import com.eagle.system.base.domain.repository.LogRepository;
import com.eagle.system.base.infrastructure.messaging.event.AuthLoginMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SystemLogRecorderTest {

    @Mock
    private LogRepository logRepository;
    @InjectMocks
    private SystemLogRecorder recorder;

    @Nested
    @DisplayName("recordAudit")
    class RecordAudit {

        @Test
        @DisplayName("应把审计条目保存为操作日志")
        void shouldSaveAuditEntryAsOperationLog() {
            AuditLogEntry entry = AuditLogEntry.builder()
                    .operatorId("100")
                    .operatorName("alice")
                    .module("用户管理")
                    .action("分配角色")
                    .requestArgs("[\"admin\"]")
                    .responseData("{\"ok\":true}")
                    .clientIp("203.0.113.8")
                    .userAgent("Chrome")
                    .costMs(42L)
                    .success(true)
                    .occurredAt(LocalDateTime.now())
                    .build();

            recorder.recordAudit(entry);

            ArgumentCaptor<SysLog> captor = ArgumentCaptor.forClass(SysLog.class);
            verify(logRepository).save(captor.capture());
            SysLog log = captor.getValue();
            assertThat(log.getLogType()).isEqualTo(LogType.OPERATION);
            assertThat(log.getStatus()).isEqualTo(LogStatus.SUCCESS);
            assertThat(log.getUserId()).isEqualTo(100L);
            assertThat(log.getUsername()).isEqualTo("alice");
            assertThat(log.getTitle()).isEqualTo("用户管理 - 分配角色");
            assertThat(log.getRemoteAddr()).isEqualTo("203.0.113.8");
            assertThat(log.getParams()).isEqualTo("[\"admin\"]");
            assertThat(log.getResult()).isEqualTo("{\"ok\":true}");
            assertThat(log.getTime()).isEqualTo(42L);
        }
    }

    @Nested
    @DisplayName("recordLogin")
    class RecordLogin {

        @Test
        @DisplayName("应把认证事件保存为登录日志")
        void shouldSaveAuthLoginMessageAsLoginLog() {
            AuthLoginMessage event = new AuthLoginMessage();
            event.setAccountId(200L);
            event.setUsername("bob");
            event.setIp("198.51.100.2");
            event.setUserAgent("Firefox");
            event.setSuccess(false);
            event.setFailReason("Bad credentials");

            recorder.recordLogin(event);

            ArgumentCaptor<SysLog> captor = ArgumentCaptor.forClass(SysLog.class);
            verify(logRepository).save(captor.capture());
            SysLog log = captor.getValue();
            assertThat(log.getLogType()).isEqualTo(LogType.LOGIN);
            assertThat(log.getStatus()).isEqualTo(LogStatus.FAILURE);
            assertThat(log.getUserId()).isEqualTo(200L);
            assertThat(log.getUsername()).isEqualTo("bob");
            assertThat(log.getRemoteAddr()).isEqualTo("198.51.100.2");
            assertThat(log.getUserAgent()).isEqualTo("Firefox");
            assertThat(log.getRequestUri()).isEqualTo("/login");
            assertThat(log.getMethod()).isEqualTo("POST");
            assertThat(log.getException()).isEqualTo("Bad credentials");
        }
    }
}
