package com.eagle.payment.core.infrastructure.messaging;

import com.eagle.common.event.BaseEvent;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提现成功跨服务集成事件。topic {@code payment_transfer_events}, tag {@code success}。
 *
 * @author sunshixiong
 */
@Getter
@NoArgsConstructor
public class TransferSucceededIntegrationEvent extends BaseEvent {

    private Long transferId;
    private String tenantId;
    private String bizTransferNo;
    private PaymentChannel channel;
    private BigDecimal amount;
    private String recipientAccount;
    private String channelTransferNo;
    private LocalDateTime succeededAt;

    public TransferSucceededIntegrationEvent(Long transferId, String tenantId, String bizTransferNo,
                                             PaymentChannel channel, BigDecimal amount,
                                             String recipientAccount, String channelTransferNo,
                                             LocalDateTime succeededAt) {
        this.transferId = transferId;
        this.tenantId = tenantId;
        this.bizTransferNo = bizTransferNo;
        this.channel = channel;
        this.amount = amount;
        this.recipientAccount = recipientAccount;
        this.channelTransferNo = channelTransferNo;
        this.succeededAt = succeededAt;
    }
}
