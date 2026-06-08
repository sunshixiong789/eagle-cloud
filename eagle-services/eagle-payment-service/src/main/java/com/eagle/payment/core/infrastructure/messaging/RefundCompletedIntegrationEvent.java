package com.eagle.payment.core.infrastructure.messaging;

import com.eagle.common.event.BaseEvent;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款完成跨服务集成事件。topic {@code payment_refund_events}, tag {@code refunded}。
 *
 * @author sunshixiong
 */
@Getter
@NoArgsConstructor
public class RefundCompletedIntegrationEvent extends BaseEvent {

    private Long refundId;
    private Long paymentId;
    private String tenantId;
    private String bizRefundNo;
    private PaymentChannel channel;
    private BigDecimal amount;
    private String channelRefundNo;
    private LocalDateTime refundedAt;

    public RefundCompletedIntegrationEvent(Long refundId, Long paymentId, String tenantId,
                                           String bizRefundNo, PaymentChannel channel,
                                           BigDecimal amount, String channelRefundNo,
                                           LocalDateTime refundedAt) {
        this.refundId = refundId;
        this.paymentId = paymentId;
        this.tenantId = tenantId;
        this.bizRefundNo = bizRefundNo;
        this.channel = channel;
        this.amount = amount;
        this.channelRefundNo = channelRefundNo;
        this.refundedAt = refundedAt;
    }
}
