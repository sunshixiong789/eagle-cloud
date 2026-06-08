package com.eagle.payment.core.infrastructure.messaging;

import com.eagle.common.event.BaseEvent;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提现审核拒绝跨服务集成事件。topic {@code payment_transfer_events}, tag {@code rejected}。
 *
 * @author sunshixiong
 */
@Getter
@NoArgsConstructor
public class TransferRejectedIntegrationEvent extends BaseEvent {

    private Long transferId;
    private String bizTransferNo;
    private PaymentChannel channel;
    private BigDecimal amount;
    private String recipientAccount;
    private String approverId;
    private String rejectReason;
    private LocalDateTime rejectedAt;

    public TransferRejectedIntegrationEvent(Long transferId, String bizTransferNo,
                                            PaymentChannel channel, BigDecimal amount,
                                            String recipientAccount, String approverId,
                                            String rejectReason, LocalDateTime rejectedAt) {
        this.transferId = transferId;
        this.bizTransferNo = bizTransferNo;
        this.channel = channel;
        this.amount = amount;
        this.recipientAccount = recipientAccount;
        this.approverId = approverId;
        this.rejectReason = rejectReason;
        this.rejectedAt = rejectedAt;
    }
}
