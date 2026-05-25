package com.eagle.payment.gateway;

import com.eagle.payment.model.NotifyResult;
import com.eagle.payment.model.PayRequest;
import com.eagle.payment.model.PayResult;
import com.eagle.payment.model.RefundRequest;
import com.eagle.payment.model.RefundResult;
import com.eagle.payment.model.TransferRequest;
import com.eagle.payment.model.TransferResult;

import java.util.Map;

/**
 * 统一支付网关接口。
 *
 * <p>屏蔽支付宝、微信支付等第三方 SDK 的差异，业务代码依赖此接口而非具体实现。
 * 多个实现同时存在时（同时接入支付宝和微信），业务方通过 Bean 名称或 {@code @Qualifier} 区分：
 * <ul>
 *   <li>{@code alipayPaymentGateway} — 支付宝网关</li>
 *   <li>{@code wechatPaymentGateway} — 微信支付网关</li>
 * </ul>
 *
 * @author eagle
 */
public interface PaymentGateway {

    /**
     * 发起支付。
     *
     * <p>返回的 {@code PayResult.getPayInfo()} 因支付方式而异：
     * APP 支付返回订单字符串，PC 扫码返回二维码链接，小程序支付返回 prepayId。
     *
     * @param request 支付请求
     * @return 支付结果
     */
    PayResult pay(PayRequest request);

    /**
     * 发起退款。
     *
     * <p>退款为异步操作，受理成功仅表示第三方已接受退款申请，
     * 实际到账需监听退款异步通知。
     *
     * @param request 退款请求
     * @return 退款结果
     */
    RefundResult refund(RefundRequest request);

    /**
     * 解析并验证支付异步通知。
     *
     * <p>实现应完成签名验证，验签失败时返回 {@code success = false}，
     * 不得抛出异常（避免第三方重复推送）。
     *
     * @param params HTTP 表单参数（支付宝回调）或空 Map（微信回调从 body 解析）
     * @param body   原始请求体（微信回调 JSON，支付宝可传 null）
     * @return 通知解析结果
     */
    NotifyResult parseNotify(Map<String, String> params, String body);

    /**
     * 主动查询订单状态。
     *
     * <p>用于支付超时补偿、对账等场景，避免依赖异步通知。
     *
     * @param outTradeNo 商户订单号
     * @return 查询结果
     */
    PayResult queryOrder(String outTradeNo);

    /**
     * 企业付款/转账给用户（提现、红包等场景）。
     *
     * <p>默认实现抛出 {@link UnsupportedOperationException}，支持转账的渠道（如支付宝）
     * 需覆盖此方法。调用前请通过文档确认当前渠道是否开通企业付款权限。
     *
     * @param request 转账请求
     * @return 转账结果
     * @throws UnsupportedOperationException 若当前渠道不支持转账
     */
    default TransferResult transfer(TransferRequest request) {
        throw new UnsupportedOperationException("Current payment channel does not support transfer");
    }

    /**
     * 查询退款状态。
     *
     * <p>默认实现抛出 {@link UnsupportedOperationException}，需要查询退款的渠道需覆盖此方法。
     * 退款为异步到账，此接口用于对账或超时补偿。
     *
     * @param refundNo 商户退款单号
     * @return 退款查询结果
     * @throws UnsupportedOperationException 若当前渠道不支持退款查询
     */
    default RefundResult queryRefund(String refundNo) {
        throw new UnsupportedOperationException("Current payment channel does not support refund query");
    }
}
