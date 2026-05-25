package com.eagle.system.base.infrastructure.messaging;

import com.eagle.common.alert.AlertEvent;
import com.eagle.common.alert.AlertService;
import com.eagle.common.alert.AlertSeverity;
import com.eagle.rocketmq.properties.RocketMqProperties;
import com.eagle.system.base.domain.model.DeadLetterRecord;
import com.eagle.system.base.domain.repository.DeadLetterRecordRepository;
import com.eagle.system.base.infrastructure.messaging.event.AccountDeletedMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountDeletedDlqListener")
class AccountDeletedDlqListenerTest {

    @Mock
    private AlertService alertService;
    @Mock
    private DeadLetterRecordRepository deadLetterRepository;

    private AccountDeletedDlqListener listener;

    @BeforeEach
    void setUp() {
        listener = new AccountDeletedDlqListener(
                new RocketMqProperties(), alertService, deadLetterRepository, new ObjectMapper());
    }

    @Test
    @DisplayName("originalConsumerGroup 应指向原 Consumer 组")
    void originalConsumerGroupMatches() {
        assertThat(listener.getOriginalConsumerGroup())
                .isEqualTo(AccountDeletedConsumer.CONSUMER_GROUP);
    }

    @Test
    @DisplayName("handleDeadLetter 应发出含 eventId/accountId/attempts 的 ERROR 告警")
    void sendsAlertWithFullContext() {
        AccountDeletedMessage event = new AccountDeletedMessage();
        event.setAccountId(200L);

        listener.handleDeadLetter(event, 16);

        ArgumentCaptor<AlertEvent> captor = ArgumentCaptor.forClass(AlertEvent.class);
        verify(alertService).send(captor.capture());
        AlertEvent sent = captor.getValue();

        assertThat(sent.severity()).isEqualTo(AlertSeverity.ERROR);
        assertThat(sent.category()).isEqualTo("mq-dlq");
        assertThat(sent.source()).isEqualTo("eagle-system-service");
        assertThat(sent.title()).contains("AccountDeleted");
        assertThat(sent.contexts())
                .containsEntry("accountId", "200")
                .containsEntry("totalAttempts", "16")
                .containsKey("eventId");
    }

    @Test
    @DisplayName("handleDeadLetter 同时持久化 DeadLetterRecord(payload 含 accountId)")
    void persistsDeadLetterRecord() {
        AccountDeletedMessage event = new AccountDeletedMessage();
        event.setAccountId(200L);

        listener.handleDeadLetter(event, 16);

        ArgumentCaptor<DeadLetterRecord> captor = ArgumentCaptor.forClass(DeadLetterRecord.class);
        verify(deadLetterRepository).save(captor.capture());
        DeadLetterRecord saved = captor.getValue();

        assertThat(saved.getTopic()).isEqualTo("eagle.auth.events");
        assertThat(saved.getTag()).isEqualTo("account.deleted");
        assertThat(saved.getConsumerGroup()).isEqualTo("system_account_deleted");
        assertThat(saved.getTotalAttempts()).isEqualTo(16);
        assertThat(saved.getStatus()).isEqualTo("PENDING");
        assertThat(saved.getPayload()).contains("\"accountId\":200");
    }
}
