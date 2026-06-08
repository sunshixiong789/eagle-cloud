package com.eagle.payment.core.infrastructure.messaging;

import com.eagle.common.event.BaseEvent;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 退款失败跨服务集成事件。topic {@code payment_refund_events}, tag {@code failed}。
 *
 * @author sunshixiong
 */
@Getter
@NoArgsConstructor
public class RefundFailedIntegrationEvent extends BaseEvent {

    private Long refundId;
    private Long paymentId;
    private String bizRefundNo;
    private PaymentChannel channel;
    private BigDecimal amount;
    private String reason;

    public RefundFailedIntegrationEvent(Long refundId, Long paymentId,
                                        String bizRefundNo, PaymentChannel channel,
                                        BigDecimal amount, String reason) {
        this.refundId = refundId;
        this.paymentId = paymentId;
        this.bizRefundNo = bizRefundNo;
        this.channel = channel;
        this.amount = amount;
        this.reason = reason;
    }
}
