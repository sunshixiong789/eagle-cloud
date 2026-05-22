package com.eagle.auth.infrastructure.event;

import com.eagle.auth.domain.event.AccountDeletedEvent;
import com.eagle.auth.domain.event.AccountFrozenEvent;
import com.eagle.auth.domain.event.AccountRegisteredEvent;
import com.eagle.auth.domain.event.AccountUnfrozenEvent;
import com.eagle.auth.domain.event.BlacklistAddedEvent;
import com.eagle.auth.domain.event.BlacklistRemovedEvent;
import com.eagle.auth.infrastructure.event.integration.AccountDeletedIntegrationEvent;
import com.eagle.auth.infrastructure.event.integration.AccountFrozenIntegrationEvent;
import com.eagle.auth.infrastructure.event.integration.AccountRegisteredIntegrationEvent;
import com.eagle.auth.infrastructure.event.integration.AccountUnfrozenIntegrationEvent;
import com.eagle.auth.infrastructure.event.integration.BlacklistAddedIntegrationEvent;
import com.eagle.auth.infrastructure.event.integration.BlacklistRemovedIntegrationEvent;
import com.eagle.rocketmq.publisher.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Auth 域跨服务集成事件桥接器。
 *
 * <p>把 auth 内部的领域事件（record）转换为 {@code extends BaseEvent} 的集成事件，发布到
 * RocketMQ topic {@code eagle.auth.events}（tag 按事件类型区分），供 system-service /
 * 其他下游服务消费。
 *
 * <p>领域事件内部 handler（如 {@link AccountSecurityEventHandler}）继续处理 auth 自己
 * 的副作用（强制下线 / 审计日志等），与本桥接器并行不冲突。
 *
 * <p>所有发布在 {@code AFTER_COMMIT} 阶段触发并 {@code @Async}，主事务无任何同步阻塞。
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthIntegrationEventPublisher {

    public static final String TOPIC = "eagle.auth.events";

    private final DomainEventPublisher publisher;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccountRegistered(AccountRegisteredEvent event) {
        AccountRegisteredIntegrationEvent integration = new AccountRegisteredIntegrationEvent(
                event.accountId(), event.username(), event.phone(),
                event.nickname(), event.avatar(), event.email());
        publisher.publish(TOPIC, "account.registered", integration);
        log.debug("published account.registered, accountId={}", event.accountId());
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccountDeleted(AccountDeletedEvent event) {
        publisher.publish(TOPIC, "account.deleted",
                new AccountDeletedIntegrationEvent(event.accountId()));
        log.debug("published account.deleted, accountId={}", event.accountId());
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccountFrozen(AccountFrozenEvent event) {
        AccountFrozenIntegrationEvent integration = new AccountFrozenIntegrationEvent(
                event.accountId(), event.username(),
                event.reason() != null ? event.reason().name() : null,
                event.freezeUntil(), event.operatorId());
        publisher.publish(TOPIC, "account.frozen", integration);
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccountUnfrozen(AccountUnfrozenEvent event) {
        AccountUnfrozenIntegrationEvent integration = new AccountUnfrozenIntegrationEvent(
                event.accountId(), event.username(),
                event.source() != null ? event.source().name() : null,
                event.operatorId());
        publisher.publish(TOPIC, "account.unfrozen", integration);
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBlacklistAdded(BlacklistAddedEvent event) {
        BlacklistAddedIntegrationEvent integration = new BlacklistAddedIntegrationEvent(
                event.id(),
                event.type() != null ? event.type().name() : null,
                event.value(), event.expiresAt());
        publisher.publish(TOPIC, "blacklist.added", integration);
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBlacklistRemoved(BlacklistRemovedEvent event) {
        BlacklistRemovedIntegrationEvent integration = new BlacklistRemovedIntegrationEvent(
                event.id(),
                event.type() != null ? event.type().name() : null,
                event.value());
        publisher.publish(TOPIC, "blacklist.removed", integration);
    }
}
