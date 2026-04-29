package com.eagle.payment.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 统一支付异步通知解析结果。
 *
 * <p>网关验签成功且交易状态为成功后，由
 * {@link com.eagle.payment.gateway.PaymentGateway#parseNotify} 返回此对象。
 * 业务方监听 {@link com.eagle.payment.event.PaymentNotifyEvent} 使用此结果更新订单状态。
 *
 * @author eagle
 */
@Data
@Builder
public class NotifyResult {

    /** 支付是否成功 */
    private boolean success;

    /** 商户订单号 */
    private String outTradeNo;

    /** 第三方交易号 */
    private String tradeNo;

    /** 实际支付金额（元） */
    private BigDecimal amount;

    /** 买家 ID（支付宝为买家 uid，微信为 openId） */
    private String buyerId;

    /** 附加参数（下单时传入的 passbackParams） */
    private String passbackParams;
}
