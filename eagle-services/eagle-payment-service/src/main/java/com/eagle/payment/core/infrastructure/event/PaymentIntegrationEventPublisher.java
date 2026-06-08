package com.eagle.payment.core.infrastructure.event;

import com.eagle.payment.core.domain.event.PaymentCancelledEvent;
import com.eagle.payment.core.domain.event.PaymentExpiredEvent;
import com.eagle.payment.core.domain.event.PaymentFailedEvent;
import com.eagle.payment.core.domain.event.PaymentPaidEvent;
import com.eagle.payment.core.infrastructure.messaging.PaymentCancelledIntegrationEvent;
import com.eagle.payment.core.infrastructure.messaging.PaymentExpiredIntegrationEvent;
import com.eagle.payment.core.infrastructure.messaging.PaymentFailedIntegrationEvent;
import com.eagle.payment.core.infrastructure.messaging.PaymentPaidIntegrationEvent;
import com.eagle.rocketmq.publisher.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Payment 域跨服务集成事件桥接器。
 *
 * <p>把 {@code com.eagle.payment.core.domain.event} 下的领域事件 (record) 转为
 * {@code extends BaseEvent} 集成事件,发布到 RocketMQ topic {@code payment_payment_events}
 * (tag 区分子事件)。上游业务方 (order-service / ledger-service / fulfillment-service /
 * notification-service) 各自订阅自己关心的 tag。
 *
 * <p>topic 命名遵循 {@code rules/15-messaging.md} 的 {@code {service}_{domain}_events}
 * 约定;同一聚合根的生命周期事件复用同一 topic,以 tag 区分。环境隔离由独立 RocketMQ
 * 集群 / Nacos namespace 实现,topic 本身不带 env 前缀。
 *
 * <p>所有发布在 {@code AFTER_COMMIT} 阶段触发并 {@code @Async},主事务无同步阻塞。
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentIntegrationEventPublisher {

    public static final String TOPIC = "payment_payment_events";

    private final DomainEventPublisher publisher;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentPaid(PaymentPaidEvent event) {
        publisher.publish(TOPIC, "paid", new PaymentPaidIntegrationEvent(
                event.paymentId(), event.bizOrderNo(), event.channel(),
                event.amount(), event.currency(), event.outTradeNo(), event.paidAt()));
        log.debug("published payment.paid, paymentId={}", event.paymentId());
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentFailed(PaymentFailedEvent event) {
        publisher.publish(TOPIC, "failed", new PaymentFailedIntegrationEvent(
                event.paymentId(), event.bizOrderNo(),
                event.channel(), event.reason()));
        log.debug("published payment.failed, paymentId={}", event.paymentId());
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCancelled(PaymentCancelledEvent event) {
        publisher.publish(TOPIC, "cancelled", new PaymentCancelledIntegrationEvent(
                event.paymentId(), event.bizOrderNo(),
                event.channel(), event.reason()));
        log.debug("published payment.cancelled, paymentId={}", event.paymentId());
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentExpired(PaymentExpiredEvent event) {
        publisher.publish(TOPIC, "expired", new PaymentExpiredIntegrationEvent(
                event.paymentId(), event.bizOrderNo(), event.channel()));
        log.debug("published payment.expired, paymentId={}", event.paymentId());
    }
}
