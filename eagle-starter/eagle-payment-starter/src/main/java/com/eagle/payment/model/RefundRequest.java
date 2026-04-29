package com.eagle.payment.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 统一退款请求。
 *
 * <p>退款金额不得超过原订单实际支付金额，部分退款时需保证退款流水号全局唯一。
 *
 * @author eagle
 */
@Data
@Builder
public class RefundRequest {

    /** 原商户订单号 */
    private String outTradeNo;

    /** 退款流水号（全局唯一，用于幂等控制） */
    private String refundNo;

    /** 退款金额（元，精确到分） */
    private BigDecimal refundAmount;

    /** 退款原因（展示给买家） */
    private String reason;
}
