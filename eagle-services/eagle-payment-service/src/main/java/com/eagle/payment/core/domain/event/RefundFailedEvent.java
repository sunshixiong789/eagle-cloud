package com.eagle.payment.core.domain.event;

import com.eagle.payment.core.domain.model.enums.PaymentChannel;

import java.math.BigDecimal;

/**
 * 退款失败领域事件 (FAILED 终态)。
 *
 * @author sunshixiong
 */
public record RefundFailedEvent(
        Long refundId,
        Long paymentId,
        String bizRefundNo,
        PaymentChannel channel,
        BigDecimal amount,
        String reason
) {
}
