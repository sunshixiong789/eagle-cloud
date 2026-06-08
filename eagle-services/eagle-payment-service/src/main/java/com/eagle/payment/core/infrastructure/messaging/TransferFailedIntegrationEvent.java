package com.eagle.payment.core.infrastructure.messaging;

import com.eagle.common.event.BaseEvent;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 提现失败跨服务集成事件。topic {@code payment_transfer_events}, tag {@code failed}。
 *
 * @author sunshixiong
 */
@Getter
@NoArgsConstructor
public class TransferFailedIntegrationEvent extends BaseEvent {

    private Long transferId;
    private String bizTransferNo;
    private PaymentChannel channel;
    private BigDecimal amount;
    private String recipientAccount;
    private String reason;

    public TransferFailedIntegrationEvent(Long transferId, String bizTransferNo,
                                          PaymentChannel channel, BigDecimal amount,
                                          String recipientAccount, String reason) {
        this.transferId = transferId;
        this.bizTransferNo = bizTransferNo;
        this.channel = channel;
        this.amount = amount;
        this.recipientAccount = recipientAccount;
        this.reason = reason;
    }
}
