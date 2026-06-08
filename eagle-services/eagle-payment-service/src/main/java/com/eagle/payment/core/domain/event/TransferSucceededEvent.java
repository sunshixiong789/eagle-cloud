package com.eagle.payment.core.domain.event;

import com.eagle.payment.core.domain.model.enums.PaymentChannel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提现成功领域事件。
 *
 * @author sunshixiong
 */
public record TransferSucceededEvent(
        Long transferId,
        String tenantId,
        String bizTransferNo,
        PaymentChannel channel,
        BigDecimal amount,
        String recipientAccount,
        String channelTransferNo,
        LocalDateTime succeededAt
) {
}
