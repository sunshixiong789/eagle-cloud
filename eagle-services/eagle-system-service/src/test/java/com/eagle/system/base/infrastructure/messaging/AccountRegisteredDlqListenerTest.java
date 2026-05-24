package com.eagle.system.base.infrastructure.messaging;

import com.eagle.common.alert.AlertEvent;
import com.eagle.common.alert.AlertService;
import com.eagle.common.alert.AlertSeverity;
import com.eagle.rocketmq.properties.RocketMqProperties;
import com.eagle.system.base.domain.model.DeadLetterRecord;
import com.eagle.system.base.domain.repository.DeadLetterRecordRepository;
import com.eagle.system.base.infrastructure.messaging.event.AccountRegisteredMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountRegisteredDlqListener")
class AccountRegisteredDlqListenerTest {

    @Mock
    private AlertService alertService;
    @Mock
    private DeadLetterRecordRepository deadLetterRepository;

    private AccountRegisteredDlqListener listener;

    @BeforeEach
    void setUp() {
        listener = new AccountRegisteredDlqListener(
                new RocketMqProperties(), alertService, deadLetterRepository);
    }

    @Test
    @DisplayName("originalConsumerGroup 应指向原 Consumer 组")
    void originalConsumerGroupMatches() {
        assertThat(listener.getOriginalConsumerGroup())
                .isEqualTo(AccountRegisteredConsumer.CONSUMER_GROUP);
    }

    @Test
    @DisplayName("handleDeadLetter 应发出含 eventId/accountId/attempts 的 ERROR 告警")
    void sendsAlertWithFullContext() {
        AccountRegisteredMessage event = new AccountRegisteredMessage();
        event.setAccountId(100L);
        event.setUsername("alice");

        listener.handleDeadLetter(event, 16);

        ArgumentCaptor<AlertEvent> captor = ArgumentCaptor.forClass(AlertEvent.class);
        verify(alertService).send(captor.capture());
        AlertEvent sent = captor.getValue();

        assertThat(sent.severity()).isEqualTo(AlertSeverity.ERROR);
        assertThat(sent.category()).isEqualTo("mq-dlq");
        assertThat(sent.source()).isEqualTo("eagle-system-service");
        assertThat(sent.title()).contains("AccountRegistered");
        assertThat(sent.contexts())
                .containsEntry("accountId", "100")
                .containsEntry("username", "alice")
                .containsEntry("totalAttempts", "16")
                .containsKey("eventId");
    }

    @Test
    @DisplayName("handleDeadLetter 同时持久化 DeadLetterRecord(payload 含 accountId/username)")
    void persistsDeadLetterRecord() {
        AccountRegisteredMessage event = new AccountRegisteredMessage();
        event.setAccountId(100L);
        event.setUsername("alice");

        listener.handleDeadLetter(event, 16);

        ArgumentCaptor<DeadLetterRecord> captor = ArgumentCaptor.forClass(DeadLetterRecord.class);
        verify(deadLetterRepository).save(captor.capture());
        DeadLetterRecord saved = captor.getValue();

        assertThat(saved.getTopic()).isEqualTo("eagle.auth.events");
        assertThat(saved.getTag()).isEqualTo("account.registered");
        assertThat(saved.getConsumerGroup()).isEqualTo("system_account_registered");
        assertThat(saved.getTotalAttempts()).isEqualTo(16);
        assertThat(saved.getStatus()).isEqualTo("PENDING");
        assertThat(saved.getPayload())
                .contains("\"accountId\":100")
                .contains("\"username\":\"alice\"");
        assertThat(saved.getEventId()).isNotBlank();
    }
}
