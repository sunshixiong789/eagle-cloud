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
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.env.MockEnvironment;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemLogRecorderTest {

    @Mock
    private LogRepository logRepository;

    private SystemLogRecorder recorder;

    @BeforeEach
    void setUp() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("spring.application.name", "eagle-system-service");
        recorder = new SystemLogRecorder(logRepository, env);
    }

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
            assertThat(log.getServiceId()).isEqualTo("eagle-system-service");
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
            assertThat(log.getEventId()).isEqualTo(event.getEventId());
            assertThat(log.getServiceId()).isEqualTo("eagle-auth-service");
        }

        @Test
        @DisplayName("eventId 已存在时应幂等跳过")
        void shouldSkipWhenEventAlreadyExists() {
            AuthLoginMessage event = new AuthLoginMessage();
            event.setUsername("bob");
            when(logRepository.existsByEventId(event.getEventId())).thenReturn(true);

            recorder.recordLogin(event);

            verify(logRepository, never()).save(any());
        }

        @Test
        @DisplayName("save 触发 eventId 唯一约束冲突时应吞掉异常")
        void shouldSwallowConflictWhenEventIdUniqueConstraintViolated() {
            AuthLoginMessage event = new AuthLoginMessage();
            event.setUsername("bob");
            when(logRepository.existsByEventId(event.getEventId())).thenReturn(false);
            when(logRepository.save(any(SysLog.class)))
                    .thenThrow(new DataIntegrityViolationException(
                            "ERROR: duplicate key value violates unique constraint \"uk_sys_log_event_id\""));

            recorder.recordLogin(event);

            verify(logRepository, times(1)).existsByEventId(event.getEventId());
            verify(logRepository).save(any());
        }

        @Test
        @DisplayName("save 触发非 eventId 约束冲突时应上抛且不再查库")
        void shouldRethrowConflictWhenNotEventIdConflict() {
            AuthLoginMessage event = new AuthLoginMessage();
            event.setUsername("bob");
            when(logRepository.existsByEventId(event.getEventId())).thenReturn(false);
            when(logRepository.save(any(SysLog.class)))
                    .thenThrow(new DataIntegrityViolationException(
                            "ERROR: duplicate key value violates unique constraint \"sys_log_pkey\""));

            assertThatThrownBy(() -> recorder.recordLogin(event))
                    .isInstanceOf(DataIntegrityViolationException.class);
            verify(logRepository, times(1)).existsByEventId(event.getEventId());
        }
    }
}
