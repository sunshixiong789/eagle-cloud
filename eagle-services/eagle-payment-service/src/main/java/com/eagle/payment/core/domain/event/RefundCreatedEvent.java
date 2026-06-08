package com.eagle.payment.core.domain.event;

import com.eagle.payment.core.domain.model.enums.PaymentChannel;

import java.math.BigDecimal;

/**
 * 退款单创建领域事件 (REFUNDING 状态注册)。
 *
 * @author sunshixiong
 */
public record RefundCreatedEvent(
        Long refundId,
        Long paymentId,
        String bizRefundNo,
        PaymentChannel channel,
        BigDecimal amount
) {
}
