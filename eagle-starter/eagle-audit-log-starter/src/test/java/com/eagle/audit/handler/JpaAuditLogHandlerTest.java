package com.eagle.audit.handler;

import com.eagle.audit.model.AuditLogEntry;
import com.eagle.audit.model.AuditLogRecord;
import com.eagle.audit.repository.AuditLogRepository;
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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaAuditLogHandlerTest {

    @Mock
    AuditLogRepository repository;

    JpaAuditLogHandler handler;

    @Nested
    @DisplayName("handle")
    class Handle {

        @Test
        @DisplayName("persists AuditLogRecord mapped from entry with serviceId injected")
        void shouldPersistMappedRecord() {
            handler = new JpaAuditLogHandler(repository, "auth");
            AuditLogEntry entry = AuditLogEntry.builder()
                    .operatorId("1001").operatorName("alice")
                    .module("账号管理").action("冻结账号")
                    .clientIp("10.0.0.1").userAgent("Chrome")
                    .costMs(42).success(true)
                    .occurredAt(LocalDateTime.of(2026, 5, 26, 10, 0))
                    .build();

            handler.handle(entry);

            ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
            verify(repository).save(captor.capture());
            AuditLogRecord saved = captor.getValue();
            assertThat(saved.getServiceId()).isEqualTo("auth");
            assertThat(saved.getOperatorId()).isEqualTo("1001");
            assertThat(saved.getModule()).isEqualTo("账号管理");
            assertThat(saved.getAction()).isEqualTo("冻结账号");
            assertThat(saved.isSuccess()).isTrue();
            assertThat(saved.getOccurredAt()).isEqualTo(LocalDateTime.of(2026, 5, 26, 10, 0));
        }

        @Test
        @DisplayName("swallows persistence failure to avoid breaking the main business flow")
        void shouldSwallowPersistFailure() {
            handler = new JpaAuditLogHandler(repository, "system");
            AuditLogEntry entry = AuditLogEntry.builder()
                    .module("test").action("test")
                    .occurredAt(LocalDateTime.now()).build();
            when(repository.save(any(AuditLogRecord.class)))
                    .thenThrow(new RuntimeException("DB down"));

            assertThatCode(() -> handler.handle(entry)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("defaults occurredAt to now when entry has none")
        void shouldDefaultOccurredAtToNow() {
            handler = new JpaAuditLogHandler(repository, "system");
            AuditLogEntry entry = AuditLogEntry.builder()
                    .module("test").action("test").build();
            LocalDateTime before = LocalDateTime.now();

            handler.handle(entry);

            ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getOccurredAt())
                    .isAfterOrEqualTo(before);
        }
    }
}
