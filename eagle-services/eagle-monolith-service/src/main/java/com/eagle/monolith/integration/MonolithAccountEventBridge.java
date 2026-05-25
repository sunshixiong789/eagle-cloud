package com.eagle.monolith.integration;

import com.eagle.auth.domain.event.AccountDeletedEvent;
import com.eagle.auth.domain.event.AccountRegisteredEvent;
import com.eagle.system.base.application.service.AccountEventApplicationService;
import com.eagle.system.base.infrastructure.messaging.event.AccountDeletedMessage;
import com.eagle.system.base.infrastructure.messaging.event.AccountRegisteredMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges auth account events to the system user mirror without RocketMQ in monolith mode.
 * @author eagle
 */
@Component
@RequiredArgsConstructor
public class MonolithAccountEventBridge {

    private static final String EVENT_VERSION = "monolith-local-v1";

    private final AccountEventApplicationService accountEventApplicationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccountRegistered(AccountRegisteredEvent event) {
        AccountRegisteredMessage message = new AccountRegisteredMessage();
        message.setEventVersion(EVENT_VERSION);
        message.setAccountId(event.accountId());
        message.setUsername(event.username());
        message.setPhone(event.phone());
        message.setNickname(event.nickname());
        message.setAvatar(event.avatar());
        message.setEmail(event.email());
        accountEventApplicationService.onAccountRegistered(message);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccountDeleted(AccountDeletedEvent event) {
        AccountDeletedMessage message = new AccountDeletedMessage();
        message.setEventVersion(EVENT_VERSION);
        message.setAccountId(event.accountId());
        accountEventApplicationService.onAccountDeleted(message);
    }
}
