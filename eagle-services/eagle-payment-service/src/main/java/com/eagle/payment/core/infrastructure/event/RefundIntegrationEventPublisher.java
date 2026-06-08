package com.eagle.payment.core.infrastructure.event;

import com.eagle.payment.core.domain.event.RefundCompletedEvent;
import com.eagle.payment.core.domain.event.RefundFailedEvent;
import com.eagle.payment.core.infrastructure.messaging.RefundCompletedIntegrationEvent;
import com.eagle.payment.core.infrastructure.messaging.RefundFailedIntegrationEvent;
import com.eagle.rocketmq.publisher.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Refund 域跨服务集成事件桥接器。
 *
 * <p>topic {@code payment_refund_events} (按 rules/15-messaging.md
 * {@code {service}_{domain}_events} 约定, refund 是独立聚合 → 独立 topic);
 * tag: {@code refunded} / {@code failed}。
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefundIntegrationEventPublisher {

    public static final String TOPIC = "payment_refund_events";

    private final DomainEventPublisher publisher;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRefundCompleted(RefundCompletedEvent event) {
        publisher.publish(TOPIC, "refunded", new RefundCompletedIntegrationEvent(
                event.refundId(), event.paymentId(), event.bizRefundNo(),
                event.channel(), event.amount(), event.channelRefundNo(), event.refundedAt()));
        log.debug("published refund.refunded, refundId={}", event.refundId());
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRefundFailed(RefundFailedEvent event) {
        publisher.publish(TOPIC, "failed", new RefundFailedIntegrationEvent(
                event.refundId(), event.paymentId(), event.bizRefundNo(),
                event.channel(), event.amount(), event.reason()));
        log.debug("published refund.failed, refundId={}", event.refundId());
    }
}
