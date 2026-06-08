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
 * <p>实际方法将在 P0-2 / P1 扩展 refund / transfer / queryRefund / queryTransfer。
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
     * 解析渠道异步通知 (回调),含验签。
     *
     * @param headers HTTP 头 (微信用到 Wechatpay-Serial / Signature 等)
     * @param rawBody POST 原始 body (微信 JSON / 支付宝 form 拼接后)
     * @param formParams 表单形式参数 (支付宝走 application/x-www-form-urlencoded)
     * @return 解析结果 (含 signatureValid 标记)
     */
    GatewayNotifyResult parseNotify(Map<String, String> headers, String rawBody,
                                    Map<String, String> formParams);
}
