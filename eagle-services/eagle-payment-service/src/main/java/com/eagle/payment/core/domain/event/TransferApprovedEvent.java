package com.eagle.payment.core.domain.event;

import com.eagle.payment.core.domain.model.enums.PaymentChannel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提现审核通过领域事件。
 *
 * @author sunshixiong
 */
public record TransferApprovedEvent(
        Long transferId,
        String bizTransferNo,
        PaymentChannel channel,
        BigDecimal amount,
        String recipientAccount,
        String approverId,
        LocalDateTime approvedAt
) {
}
