package com.eagle.payment.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 统一支付请求。
 *
 * <p>屏蔽支付宝和微信支付 SDK 的差异，业务方只需构建此请求对象，
 * 由 {@link com.eagle.payment.gateway.PaymentGateway} 实现负责转换为第三方 SDK 调用。
 *
 * @author eagle
 */
@Data
@Builder
public class PayRequest {

    /**
     * 商户订单号（全局唯一，不超过 64 字符）
     */
    private String outTradeNo;

    /**
     * 订单标题
     */
    private String subject;

    /**
     * 支付金额（元，精确到分）
     */
    private BigDecimal amount;

    /**
     * 订单描述
     */
    private String description;

    /**
     * 过期时间（分钟，默认 30 分钟）
     */
    @Builder.Default
    private int expireMinutes = 30;

    /**
     * 扩展参数（透传给第三方，回调时原样返回）
     */
    private String passbackParams;

    /**
     * 买家 openId（微信小程序/公众号支付必填）
     */
    private String openId;
}
