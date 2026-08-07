package com.eagle.system.base.infrastructure.messaging;

import com.eagle.amqp.properties.AmqpProperties;
import com.eagle.system.base.application.service.AccountEventApplicationService;
import com.eagle.system.base.application.event.AccountDeletedMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountDeletedConsumer")
class AccountDeletedConsumerTest {

    @Mock
    private AccountEventApplicationService accountEventService;

    private AccountDeletedConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new AccountDeletedConsumer(new AmqpProperties(), accountEventService);
    }

    @Test
    @DisplayName("topic/tag/consumerGroup 与常量对齐")
    void wiringMatchesConstants() {
        assertThat(consumer.getTopic()).isEqualTo("eagle_auth_events");
        assertThat(consumer.getRoutingKey()).isEqualTo("account.deleted");
        assertThat(consumer.getConsumerGroup()).isEqualTo("system_account_deleted");
        assertThat(consumer.getEventClass()).isEqualTo(AccountDeletedMessage.class);
    }

    @Test
    @DisplayName("handle 应转发到 AccountEventApplicationService.onAccountDeleted")
    void delegatesToApplicationService() {
        AccountDeletedMessage event = new AccountDeletedMessage();
        event.setAccountId(200L);

        consumer.handle(event);

        verify(accountEventService).onAccountDeleted(event);
    }
}
