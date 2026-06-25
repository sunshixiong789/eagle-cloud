package com.eagle.auth.core.infrastructure.event;

import com.eagle.auth.core.domain.event.AccountPhoneChangedEvent;
import com.eagle.auth.core.infrastructure.event.integration.AccountPhoneChangedIntegrationEvent;
import com.eagle.auth.core.infrastructure.remote.SystemUserSyncClient;
import com.eagle.rocketmq.publisher.DomainEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthIntegrationEventPublisherTest {

    @Mock
    private DomainEventPublisher publisher;
    @Mock
    private SystemUserSyncClient systemUserSyncClient;
    @InjectMocks
    private AuthIntegrationEventPublisher bridge;

    @Test
    void shouldPublishPhoneChangedWithCorrectTopicTagPayload() {
        bridge.onAccountPhoneChanged(new AccountPhoneChangedEvent(100L, "13900139000"));

        ArgumentCaptor<AccountPhoneChangedIntegrationEvent> captor =
                ArgumentCaptor.forClass(AccountPhoneChangedIntegrationEvent.class);
        verify(publisher).publish(
                org.mockito.ArgumentMatchers.eq("eagle_auth_events"),
                org.mockito.ArgumentMatchers.eq("account.phone-changed"),
                captor.capture());
        assertEquals(100L, captor.getValue().getAccountId());
        assertEquals("13900139000", captor.getValue().getPhone());
        assertEquals("1.0", captor.getValue().getEventVersion());
    }
}
