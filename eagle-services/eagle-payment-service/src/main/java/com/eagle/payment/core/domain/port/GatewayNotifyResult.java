package com.eagle.payment.core.domain.port;

import com.eagle.payment.core.domain.model.enums.PaymentStatus;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 渠道异步通知 (回调) 解析结果。
 *
 * <p>{@link #signatureValid} 为 false 时,其他字段可能为 null,业务方 <strong>必须</strong>
 * 直接拒绝处理并返回 fail (不要返回 success,避免渠道误判 ack)。
 *
 * <p>{@link #status} 仅区分 {@link PaymentStatus#PAID} / {@link PaymentStatus#FAILED};
 * 渠道侧未明示成败的(理论上不发回调)由调用方进入对账流程。
 *
 * @author sunshixiong
 */
public record GatewayNotifyResult(
        boolean signatureValid,
        @Nullable String outTradeNo,
        @Nullable String channelTradeNo,
        @Nullable PaymentStatus status,
        @Nullable BigDecimal amount,
        @Nullable LocalDateTime paidAt,
        @Nullable String failReason,
        @Nullable String rawBody,
        @Nullable String eventId
) {
    public static GatewayNotifyResult invalid(String rawBody) {
        return new GatewayNotifyResult(false, null, null, null, null, null, null, rawBody, null);
    }
}
