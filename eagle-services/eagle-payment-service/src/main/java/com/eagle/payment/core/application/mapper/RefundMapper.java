package com.eagle.payment.core.application.mapper;

import com.eagle.payment.core.domain.model.aggregate.Refund;
import com.eagle.payment.core.interfaces.dto.response.RefundResponse;
import org.springframework.stereotype.Component;

/**
 * Refund 聚合 ↔ DTO 映射器。
 *
 * @author sunshixiong
 */
@Component
public class RefundMapper {

    public RefundResponse toResponse(Refund refund) {
        if (refund == null) {
            return null;
        }
        return new RefundResponse(
                refund.getId(),
                refund.getPaymentId(),
                refund.getBizRefundNo(),
                refund.getChannel(),
                refund.getAmount(),
                refund.getReason(),
                refund.getChannelRefundNo(),
                refund.getStatus(),
                refund.getRefundedAt(),
                refund.getFailReason(),
                refund.getCreateTime()
        );
    }
}
