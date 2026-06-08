package com.eagle.payment.core.domain.port;

import com.eagle.payment.core.domain.model.enums.PaymentChannel;

import java.util.Map;

/**
 * 支付渠道出站端口 (Driven Port)。
 *
 * <p>每个支付渠道 (支付宝 / 微信 / UnionPay / Stripe 等) 在 infrastructure/gateway/{channel}
 * 实现此接口。领域层与应用层只面向接口编程,渠道升级 / 新增不影响业务代码。
 *
 * <p>方法语义按"幂等的、可重试的"原则设计:
 * <ul>
 *   <li>{@link #createPayment} 由聚合根 CREATED → PAYING 时调用,失败抛 ServiceException;</li>
 *   <li>{@link #queryPayment} 用于回调补单 / 对账,可重试;</li>
 *   <li>{@link #parseNotify} 完成验签 + 提取交易号 / 金额 / 状态,签名失败返回 invalid 结果;</li>
 * </ul>
 *
 * @author sunshixiong
 */
public interface PaymentGatewayPort {

    /**
     * 标识本适配器对应的渠道。
     */
    PaymentChannel getChannel();

    /**
     * 提交支付订单到渠道,返回支付参数 (form / qrCode / orderInfo / jsapi 参数等)。
     *
     * @param command 渠道无关的支付参数
     * @return 渠道结果 (含 outTradeNo + scene 相关支付参数)
     */
    GatewayPayResult createPayment(GatewayPayCommand command);

    /**
     * 主动查询渠道侧的订单状态 (回调补单 / 对账场景)。
     */
    GatewayQueryResult queryPayment(PaymentChannel channel, String outTradeNo);

    /**
     * 提交退款到渠道。
     *
     * @param command 退款入参 (原订单号 + 退款单号 + 金额 + 原始金额 + 原因)
     * @return 退款结果 (含渠道退款单号 + 状态 + 失败原因)
     */
    GatewayRefundResult refund(GatewayRefundCommand command);

    /**
     * 主动查询退款状态 (异步通知补单 / 对账)。
     */
    GatewayRefundResult queryRefund(PaymentChannel channel, String refundNo);

    /**
     * 解析支付完成异步通知 (回调),含验签。
     *
     * @param headers HTTP 头 (微信用到 Wechatpay-Serial / Signature 等)
     * @param rawBody POST 原始 body (微信 JSON / 支付宝 form 拼接后)
     * @param formParams 表单形式参数 (支付宝走 application/x-www-form-urlencoded)
     * @return 解析结果 (含 signatureValid 标记)
     */
    GatewayNotifyResult parseNotify(Map<String, String> headers, String rawBody,
                                    Map<String, String> formParams);

    /**
     * 解析退款异步通知,含验签。
     *
     * <p>支付宝退款回调与支付回调走同一 notify_url,通过 trade_status / refund_fee 等字段区分;
     * 适配器内部可以选择: (a) 在此方法中识别并解析退款回调,(b) 在 {@link #parseNotify}
     * 中识别后 dispatcher 转发,本接口由调用方明确指定。
     *
     * <p>微信 V3 退款回调与支付回调走不同 URL,可直接区分。
     */
    GatewayRefundNotifyResult parseRefundNotify(Map<String, String> headers, String rawBody,
                                                Map<String, String> formParams);

    /**
     * 提交 B2C 转账 / 提现到渠道。
     *
     * <p>需要在渠道开放平台开通对应权限 (支付宝"转账到支付宝账户"、
     * 微信"商家转账到零钱"),否则会被拒绝。
     */
    GatewayTransferResult transfer(GatewayTransferCommand command);

    /**
     * 主动查询转账状态。
     */
    GatewayTransferResult queryTransfer(PaymentChannel channel, String transferNo);
}
