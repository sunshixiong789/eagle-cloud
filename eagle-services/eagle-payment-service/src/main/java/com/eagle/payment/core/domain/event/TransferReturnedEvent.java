package com.eagle.payment.core.domain.event;

import com.eagle.payment.core.domain.model.enums.PaymentChannel;

import java.math.BigDecimal;

/**
 * 提现退票领域事件 (收款方拒收 / 实名不符等导致资金原路返回)。
 *
 * @author sunshixiong
 */
public record TransferReturnedEvent(
        Long transferId,
        String tenantId,
        String bizTransferNo,
        PaymentChannel channel,
        BigDecimal amount,
        String recipientAccount,
        String reason
) {
}
