package com.eagle.payment.core.infrastructure.messaging;

import com.eagle.common.event.BaseEvent;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 支付过期跨服务集成事件。topic {@code payment_payment_events}, tag {@code expired}。
 *
 * @author sunshixiong
 */
@Getter
@NoArgsConstructor
public class PaymentExpiredIntegrationEvent extends BaseEvent {

    private Long paymentId;
    private String tenantId;
    private String bizOrderNo;
    private PaymentChannel channel;

    public PaymentExpiredIntegrationEvent(Long paymentId, String tenantId,
                                          String bizOrderNo, PaymentChannel channel) {
        this.paymentId = paymentId;
        this.tenantId = tenantId;
        this.bizOrderNo = bizOrderNo;
        this.channel = channel;
    }
}
