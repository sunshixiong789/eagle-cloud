package com.eagle.payment.core.domain.port;

import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

/**
 * 提交 B2C 转账 / 提现到渠道的入参。
 *
 * @author sunshixiong
 */
public record GatewayTransferCommand(
        PaymentChannel channel,
        String transferNo,
        BigDecimal amount,
        String currency,
        String recipientAccount,
        @Nullable String recipientName,
        @Nullable String reason
) {
}
