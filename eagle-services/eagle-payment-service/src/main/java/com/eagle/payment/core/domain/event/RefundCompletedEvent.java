package com.eagle.payment.core.domain.event;

import com.eagle.payment.core.domain.model.enums.PaymentChannel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款完成领域事件 (REFUNDED 终态)。
 *
 * <p>触发: 渠道回调成功后由 {@code Refund#markRefunded} 注册。
 * 下游处理: 应用层在事件 Handler 内调用 {@code Payment#accumulateRefund} 累加已退款金额;
 * 集成事件桥接器发布 {@code refund.refunded} 到 MQ。
 *
 * @author sunshixiong
 */
public record RefundCompletedEvent(
        Long refundId,
        Long paymentId,
        String bizRefundNo,
        PaymentChannel channel,
        BigDecimal amount,
        String channelRefundNo,
        LocalDateTime refundedAt
) {
}
