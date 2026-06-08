package com.eagle.payment.core.domain.event;

import com.eagle.payment.core.domain.model.enums.PaymentChannel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付订单完成支付的领域事件 (本域)。
 *
 * <p>由 {@code Payment#markPaid} 注册;基础设施层事件处理器接收后,
 * 转换为 {@code PaymentPaidIntegrationEvent} 发布到 RocketMQ
 * {@code payment_payment_events#paid} 供上游业务方 (order-service /
 * ledger-service 等) 推进自身状态。
 *
 * @author sunshixiong
 */
public record PaymentPaidEvent(
        Long paymentId,
        String bizOrderNo,
        PaymentChannel channel,
        BigDecimal amount,
        String currency,
        String outTradeNo,
        LocalDateTime paidAt
) {
}
