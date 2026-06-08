package com.eagle.payment.core.domain.port;

import com.eagle.payment.core.domain.model.enums.RefundStatus;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 渠道退款异步通知解析结果。
 *
 * <p>{@link #signatureValid} = false 时调用方必须拒绝处理。
 *
 * @author sunshixiong
 */
public record GatewayRefundNotifyResult(
        boolean signatureValid,
        @Nullable String refundNo,
        @Nullable String channelRefundNo,
        @Nullable RefundStatus status,
        @Nullable BigDecimal refundAmount,
        @Nullable LocalDateTime refundedAt,
        @Nullable String failReason,
        @Nullable String rawBody
) {
    public static GatewayRefundNotifyResult invalid(String rawBody) {
        return new GatewayRefundNotifyResult(false, null, null, null, null, null, null, rawBody);
    }
}
