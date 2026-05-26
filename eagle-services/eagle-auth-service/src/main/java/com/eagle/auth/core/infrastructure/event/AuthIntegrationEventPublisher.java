package com.eagle.auth.core.infrastructure.event;

import com.eagle.auth.core.domain.event.AccountDeletedEvent;
import com.eagle.auth.core.domain.event.AccountRegisteredEvent;
import com.eagle.auth.core.infrastructure.event.integration.AccountDeletedIntegrationEvent;
import com.eagle.auth.core.infrastructure.event.integration.AccountRegisteredIntegrationEvent;
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
 * RocketMQ topic {@code eagle_auth_events}（tag 按事件类型区分），供 system-service /
 * 其他下游服务消费。
 *
 * <p><strong>Topic 命名</strong>:RocketMQ 5.x gRPC 客户端强制 topic 匹配
 * {@code ^[%a-zA-Z0-9_-]+$}（禁止点号），因此用下划线分隔而非点号。
 *
 * <p>当前发布的跨服务集成事件:
 * <ul>
 *   <li>{@code account.registered} — system-service 据此创建 User 镜像</li>
 *   <li>{@code account.deleted}    — system-service 据此清理 User 镜像</li>
 * </ul>
 *
 * <p>其余 auth 内部领域事件（{@link com.eagle.auth.core.domain.event.AccountFrozenEvent}、
 * {@link com.eagle.auth.core.domain.event.AccountUnfrozenEvent}、
 * {@link com.eagle.auth.core.domain.event.BlacklistAddedEvent}、
 * {@link com.eagle.auth.core.domain.event.BlacklistRemovedEvent}）<strong>不</strong>出域,
 * 由 {@link AccountSecurityEventHandler} / {@link BlacklistCacheSyncHandler} 处理强制下线、
 * 缓存同步等本地副作用。若未来确有下游服务需要订阅,再在本类追加 publish 方法。
 *
 * <p>领域事件内部 handler 与本桥接器并行不冲突。
 *
 * <p>所有发布在 {@code AFTER_COMMIT} 阶段触发并 {@code @Async}，主事务无任何同步阻塞。
 *
 * <p><strong>Topic 命名约定</strong>:本 topic 故意<em>不</em>拼 {@code eagle.rocketmq.topic-env-prefix},
 * 走字面 {@code eagle_auth_events}(环境通过独立 RocketMQ 集群 / Nacos namespace 隔离,
 * topic 名本身不带 env 前缀,见 rules/15-messaging.md)。
 * 消费侧 {@code AccountRegisteredConsumer} / {@code AccountDeletedConsumer} 必须严格一致,
 * 不要在 {@code getTopic()} 里拼 prefix。
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthIntegrationEventPublisher {

    public static final String TOPIC = "eagle_auth_events";

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
}
