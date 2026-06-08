package com.eagle.payment.core.domain.event;

import com.eagle.payment.core.domain.model.enums.PaymentChannel;

/**
 * 支付订单过期领域事件。
 *
 * @author sunshixiong
 */
public record PaymentExpiredEvent(
        Long paymentId,
        String tenantId,
        String bizOrderNo,
        PaymentChannel channel
) {
}
