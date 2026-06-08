package com.eagle.payment.core.domain.port;

import com.eagle.payment.core.domain.model.enums.PaymentStatus;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 渠道侧订单查询返回 (用于回调补单 / 对账)。
 *
 * @author sunshixiong
 */
public record GatewayQueryResult(
        @Nullable String channelTradeNo,
        PaymentStatus status,
        @Nullable BigDecimal amount,
        @Nullable LocalDateTime paidAt,
        @Nullable String failReason
) {
}
