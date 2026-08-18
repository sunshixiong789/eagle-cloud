package com.eagle.auth.core.infrastructure.event;

import com.eagle.auth.core.domain.event.AccountDeletedEvent;
import com.eagle.auth.core.domain.event.AccountPhoneChangedEvent;
import com.eagle.auth.core.domain.event.AccountRegisteredEvent;
import com.eagle.auth.core.infrastructure.event.integration.AccountDeletedIntegrationEvent;
import com.eagle.auth.core.infrastructure.event.integration.AccountPhoneChangedIntegrationEvent;
import com.eagle.auth.core.infrastructure.event.integration.AccountRegisteredIntegrationEvent;
import com.eagle.auth.core.infrastructure.remote.SystemUserSyncClient;
import com.eagle.amqp.publisher.DomainEventPublisher;
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
 * AMQP exchange {@code eagle_auth_events}（routing key 按事件类型区分），供 system-service /
 * 其他下游服务消费。
 *
 * <p>当前发布的跨服务集成事件:
 * <ul>
 *   <li>{@code account.registered} — system-service 据此创建 User 镜像</li>
 *   <li>{@code account.deleted}    — system-service 据此清理 User 镜像</li>
 *   <li>{@code account.phone-changed} — 手机号变更广播</li>
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
 * <p><strong>Topic 命名约定</strong>:本 topic 故意<em>不</em>拼 {@code eagle.amqp.exchange-prefix},
 * 走字面 {@code eagle_auth_events}(环境通过独立 broker 集群 / vhost 隔离,
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
    private final SystemUserSyncClient systemUserSyncClient;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccountRegistered(AccountRegisteredEvent event) {
        AccountRegisteredIntegrationEvent integration = new AccountRegisteredIntegrationEvent(
                event.accountId(), event.username(), event.phone(),
                event.nickname(), event.avatar(), event.email());
        try {
            publisher.publish(TOPIC, "account.registered", integration);
            log.debug("published account.registered, accountId={}", event.accountId());
        } catch (RuntimeException mqEx) {
            // MQ 投递失败(broker 不可达 / gRPC 连接关闭 / 超时等)→ 同步 HTTP 兜底,
            // 保证 base_user 镜像不因 broker 抖动而永久缺失。下游已通过 existsByAccountId
            // + DB 唯一索引兜住幂等,即使后续 MQ 恢复重投递也不会重复创建 user。
            log.warn("AMQP publish failed, falling back to HTTP sync, accountId={}",
                    event.accountId(), mqEx);
            try {
                systemUserSyncClient.syncFromAccount(integration);
                log.info("HTTP fallback sync succeeded, accountId={}", event.accountId());
            } catch (RuntimeException httpEx) {
                // 两条通道都失败 — 此时 user 镜像确实丢了,只能告警让运维介入。
                // 后续重新登录会触发新的 grant 路径但不会再发 register 事件(已存在 Account),
                // 需要靠手工 reconcile / 定时任务补救。
                log.error("Both MQ and HTTP sync failed for accountId={} — base_user mirror lost, "
                        + "manual reconciliation required", event.accountId(), httpEx);
            }
        }
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccountDeleted(AccountDeletedEvent event) {
        try {
            publisher.publish(TOPIC, "account.deleted",
                    new AccountDeletedIntegrationEvent(event.accountId()));
            log.debug("published account.deleted, accountId={}", event.accountId());
        } catch (RuntimeException mqEx) {
            log.warn("AMQP publish failed, falling back to HTTP delete, accountId={}",
                    event.accountId(), mqEx);
            try {
                systemUserSyncClient.deleteByAccountId(event.accountId());
                log.info("HTTP fallback delete succeeded, accountId={}", event.accountId());
            } catch (RuntimeException httpEx) {
                log.error("Both MQ and HTTP delete failed for accountId={} — orphan User may remain",
                        event.accountId(), httpEx);
            }
        }
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccountPhoneChanged(AccountPhoneChangedEvent event) {
        try {
            publisher.publish(TOPIC, "account.phone-changed",
                    new AccountPhoneChangedIntegrationEvent(event.accountId(), event.phone()));
            log.debug("published account.phone-changed, accountId={}", event.accountId());
        } catch (RuntimeException mqEx) {
            // 本仓库 User 不存手机号；下游副本服务未绑定 queue 时会 unroutable。
            log.error("AMQP publish failed for account.phone-changed, accountId={} — "
                    + "downstream phone replica was not notified", event.accountId(), mqEx);
        }
    }
}
