package com.eagle.monolith.integration;

import com.eagle.auth.core.domain.event.AccountDeletedEvent;
import com.eagle.auth.core.domain.event.AccountRegisteredEvent;
import com.eagle.system.base.application.service.AccountEventApplicationService;
import com.eagle.system.base.infrastructure.messaging.event.AccountDeletedMessage;
import com.eagle.system.base.infrastructure.messaging.event.AccountRegisteredMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MonolithAccountEventBridgeTest {

    private static final Long ACCOUNT_ID = 1001L;

    @Mock
    private AccountEventApplicationService accountEventApplicationService;

    @InjectMocks
    private MonolithAccountEventBridge bridge;

    @Nested
    @DisplayName("onAccountRegistered")
    class OnAccountRegistered {

        @Test
        @DisplayName("应映射认证账号事件到System账号消息")
        void shouldMapAuthAccountEventToSystemAccountMessage() {
            AccountRegisteredEvent event = new AccountRegisteredEvent(
                    ACCOUNT_ID,
                    "admin",
                    "13800138000",
                    "Admin",
                    "https://example.com/avatar.png",
                    "admin@example.com");

            bridge.onAccountRegistered(event);

            ArgumentCaptor<AccountRegisteredMessage> captor =
                    ArgumentCaptor.forClass(AccountRegisteredMessage.class);
            verify(accountEventApplicationService).onAccountRegistered(captor.capture());

            AccountRegisteredMessage message = captor.getValue();
            assertEquals("monolith-local-v1", message.getEventVersion());
            assertEquals(ACCOUNT_ID, message.getAccountId());
            assertEquals("admin", message.getUsername());
            assertEquals("13800138000", message.getPhone());
            assertEquals("Admin", message.getNickname());
            assertEquals("https://example.com/avatar.png", message.getAvatar());
            assertEquals("admin@example.com", message.getEmail());
        }
    }

    @Nested
    @DisplayName("onAccountDeleted")
    class OnAccountDeleted {

        @Test
        @DisplayName("应映射认证删除事件到System删除消息")
        void shouldMapAuthDeleteEventToSystemDeleteMessage() {
            bridge.onAccountDeleted(new AccountDeletedEvent(ACCOUNT_ID));

            ArgumentCaptor<AccountDeletedMessage> captor =
                    ArgumentCaptor.forClass(AccountDeletedMessage.class);
            verify(accountEventApplicationService).onAccountDeleted(captor.capture());

            AccountDeletedMessage message = captor.getValue();
            assertEquals("monolith-local-v1", message.getEventVersion());
            assertEquals(ACCOUNT_ID, message.getAccountId());
        }
    }
}
