package com.eagle.payment.core.application.mapper;

import com.eagle.payment.core.domain.model.aggregate.Transfer;
import com.eagle.payment.core.interfaces.dto.response.TransferResponse;
import org.springframework.stereotype.Component;

/**
 * Transfer 聚合 ↔ DTO 映射器。
 *
 * @author sunshixiong
 */
@Component
public class TransferMapper {

    public TransferResponse toResponse(Transfer transfer) {
        if (transfer == null) {
            return null;
        }
        return new TransferResponse(
                transfer.getId(),
                transfer.getBizTransferNo(),
                transfer.getChannel(),
                transfer.getRecipientAccount(),
                transfer.getRecipientName(),
                transfer.getAmount(),
                transfer.getReason(),
                transfer.getChannelTransferNo(),
                transfer.getStatus(),
                transfer.getSucceededAt(),
                transfer.getFailReason(),
                transfer.getCreateTime()
        );
    }
}
