package com.eagle.payment.core.infrastructure.messaging;

import com.eagle.common.event.BaseEvent;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提现审核通过跨服务集成事件。topic {@code payment_transfer_events}, tag {@code approved}。
 *
 * @author sunshixiong
 */
@Getter
@NoArgsConstructor
public class TransferApprovedIntegrationEvent extends BaseEvent {

    private Long transferId;
    private String bizTransferNo;
    private PaymentChannel channel;
    private BigDecimal amount;
    private String recipientAccount;
    private String approverId;
    private LocalDateTime approvedAt;

    public TransferApprovedIntegrationEvent(Long transferId, String bizTransferNo,
                                            PaymentChannel channel, BigDecimal amount,
                                            String recipientAccount, String approverId,
                                            LocalDateTime approvedAt) {
        this.transferId = transferId;
        this.bizTransferNo = bizTransferNo;
        this.channel = channel;
        this.amount = amount;
        this.recipientAccount = recipientAccount;
        this.approverId = approverId;
        this.approvedAt = approvedAt;
    }
}
