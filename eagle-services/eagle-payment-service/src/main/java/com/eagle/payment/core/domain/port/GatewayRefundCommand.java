package com.eagle.payment.core.domain.port;

import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

/**
 * 提交退款到渠道的入参 (渠道无关)。
 *
 * <p>{@link #paymentOutTradeNo} 是原 Payment 在渠道侧的商户订单号 (即 Payment.outTradeNo);
 * {@link #refundNo} 是支付服务自身的退款单号 (作为渠道的 out_refund_no / out_request_no)。
 *
 * @author sunshixiong
 */
public record GatewayRefundCommand(
        PaymentChannel channel,
        String paymentOutTradeNo,
        String refundNo,
        BigDecimal refundAmount,
        BigDecimal originalAmount,
        String currency,
        @Nullable String reason
) {
}
