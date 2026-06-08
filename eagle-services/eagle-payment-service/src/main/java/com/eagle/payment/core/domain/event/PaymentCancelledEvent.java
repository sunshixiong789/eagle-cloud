package com.eagle.payment.core.domain.event;

import com.eagle.payment.core.domain.model.enums.PaymentChannel;

/**
 * 支付订单主动取消领域事件。
 *
 * @author sunshixiong
 */
public record PaymentCancelledEvent(
        Long paymentId,
        String tenantId,
        String bizOrderNo,
        PaymentChannel channel,
        String reason
) {
}
