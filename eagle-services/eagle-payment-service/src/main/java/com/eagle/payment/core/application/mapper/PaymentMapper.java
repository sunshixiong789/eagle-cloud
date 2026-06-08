package com.eagle.payment.core.application.mapper;

import com.eagle.payment.core.domain.model.aggregate.Payment;
import com.eagle.payment.core.interfaces.dto.response.PaymentResponse;
import org.springframework.stereotype.Component;

/**
 * Payment 聚合 ↔ DTO 映射器 (纯 Java,无 MapStruct)。
 *
 * @author sunshixiong
 */
@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {
        if (payment == null) {
            return null;
        }
        return new PaymentResponse(
                payment.getId(),
                payment.getBizOrderNo(),
                payment.getChannel(),
                payment.getScene(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getSubject(),
                payment.getOutTradeNo(),
                payment.getStatus(),
                payment.getRefundedAmount(),
                payment.getPaidAt(),
                payment.getExpiresAt(),
                payment.getFailReason(),
                payment.getCreateTime()
        );
    }
}
