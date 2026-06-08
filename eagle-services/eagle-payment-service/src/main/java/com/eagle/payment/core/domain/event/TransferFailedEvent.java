package com.eagle.payment.core.domain.event;

import com.eagle.payment.core.domain.model.enums.PaymentChannel;

import java.math.BigDecimal;

/**
 * 提现失败领域事件。
 *
 * @author sunshixiong
 */
public record TransferFailedEvent(
        Long transferId,
        String tenantId,
        String bizTransferNo,
        PaymentChannel channel,
        BigDecimal amount,
        String recipientAccount,
        String reason
) {
}
