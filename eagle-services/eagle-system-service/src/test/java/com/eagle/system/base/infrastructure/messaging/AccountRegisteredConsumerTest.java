package com.eagle.system.base.infrastructure.messaging;

import com.eagle.rocketmq.properties.RocketMqProperties;
import com.eagle.system.base.application.service.AccountEventApplicationService;
import com.eagle.system.base.infrastructure.messaging.event.AccountRegisteredMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountRegisteredConsumer")
class AccountRegisteredConsumerTest {

    @Mock
    private AccountEventApplicationService accountEventService;

    private AccountRegisteredConsumer consumer;

    @BeforeEach
    void setUp() {
        // Consumer 基类构造器必须调 super(props),不能用 @InjectMocks(Mockito 不调 super);
        // 也不能让 Consumer 子类用 @RequiredArgsConstructor(详见 CLAUDE.md 高频陷阱)。
        consumer = new AccountRegisteredConsumer(new RocketMqProperties(), accountEventService);
    }

    @Test
    @DisplayName("topic/tag/consumerGroup 与常量对齐")
    void wiringMatchesConstants() {
        assertThat(consumer.getTopic()).isEqualTo("eagle_auth_events");
        assertThat(consumer.getTagExpression()).isEqualTo("account.registered");
        assertThat(consumer.getConsumerGroup()).isEqualTo("system_account_registered");
        assertThat(consumer.getEventClass()).isEqualTo(AccountRegisteredMessage.class);
    }

    @Test
    @DisplayName("handle 应转发到 AccountEventApplicationService.onAccountRegistered")
    void delegatesToApplicationService() {
        AccountRegisteredMessage event = new AccountRegisteredMessage();
        event.setAccountId(100L);
        event.setUsername("alice");
        event.setPhone("13900000000");

        consumer.handle(event);

        verify(accountEventService).onAccountRegistered(event);
    }
}
