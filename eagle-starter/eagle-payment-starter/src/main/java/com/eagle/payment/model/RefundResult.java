package com.eagle.payment.model;

import lombok.Builder;
import lombok.Data;

/**
 * 统一退款结果。
 *
 * @author eagle
 */
@Data
@Builder
public class RefundResult {

    /** 是否退款成功（第三方受理成功，实际到账需等待异步通知） */
    private boolean success;

    /** 退款流水号 */
    private String refundNo;

    /** 原商户订单号 */
    private String outTradeNo;

    /** 错误信息（success = false 时有值） */
    private String errorMessage;
}
