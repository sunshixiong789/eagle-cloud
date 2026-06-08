package com.eagle.payment.core.infrastructure.event;

import com.eagle.payment.core.domain.event.TransferFailedEvent;
import com.eagle.payment.core.domain.event.TransferReturnedEvent;
import com.eagle.payment.core.domain.event.TransferSucceededEvent;
import com.eagle.payment.core.infrastructure.messaging.TransferFailedIntegrationEvent;
import com.eagle.payment.core.infrastructure.messaging.TransferReturnedIntegrationEvent;
import com.eagle.payment.core.infrastructure.messaging.TransferSucceededIntegrationEvent;
import com.eagle.rocketmq.publisher.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Transfer 域跨服务集成事件桥接器。
 *
 * <p>topic {@code payment_transfer_events}, tag: {@code success} / {@code failed} /
 * {@code returned}。Transfer 是独立聚合,按 rules/15-messaging.md 走独立 topic。
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransferIntegrationEventPublisher {

    public static final String TOPIC = "payment_transfer_events";

    private final DomainEventPublisher publisher;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransferSucceeded(TransferSucceededEvent event) {
        publisher.publish(TOPIC, "success", new TransferSucceededIntegrationEvent(
                event.transferId(), event.bizTransferNo(), event.channel(),
                event.amount(), event.recipientAccount(), event.channelTransferNo(),
                event.succeededAt()));
        log.debug("published transfer.success, transferId={}", event.transferId());
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransferFailed(TransferFailedEvent event) {
        publisher.publish(TOPIC, "failed", new TransferFailedIntegrationEvent(
                event.transferId(), event.bizTransferNo(), event.channel(),
                event.amount(), event.recipientAccount(), event.reason()));
        log.debug("published transfer.failed, transferId={}", event.transferId());
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransferReturned(TransferReturnedEvent event) {
        publisher.publish(TOPIC, "returned", new TransferReturnedIntegrationEvent(
                event.transferId(), event.bizTransferNo(), event.channel(),
                event.amount(), event.recipientAccount(), event.reason()));
        log.debug("published transfer.returned, transferId={}", event.transferId());
    }
}
