package com.eagle.payment.core.infrastructure.messaging;

import com.eagle.common.event.BaseEvent;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付成功跨服务集成事件 (生产方载荷)。
 *
 * <p>消费方在自己模块声明 {@code PaymentPaidMessage},不 import 本类
 * (按 {@code rules/15-messaging.md} 双侧独立声明原则)。
 *
 * <p>topic {@code payment_payment_events}, tag {@code paid}。
 *
 * @author sunshixiong
 */
@Getter
@NoArgsConstructor
public class PaymentPaidIntegrationEvent extends BaseEvent {

    private Long paymentId;
    private String tenantId;
    private String bizOrderNo;
    private PaymentChannel channel;
    private BigDecimal amount;
    private String currency;
    private String outTradeNo;
    private LocalDateTime paidAt;

    public PaymentPaidIntegrationEvent(Long paymentId, String tenantId, String bizOrderNo,
                                       PaymentChannel channel, BigDecimal amount, String currency,
                                       String outTradeNo, LocalDateTime paidAt) {
        this.paymentId = paymentId;
        this.tenantId = tenantId;
        this.bizOrderNo = bizOrderNo;
        this.channel = channel;
        this.amount = amount;
        this.currency = currency;
        this.outTradeNo = outTradeNo;
        this.paidAt = paidAt;
    }
}
