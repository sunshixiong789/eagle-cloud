package com.eagle.payment.model;

import lombok.Builder;
import lombok.Data;

/**
 * 统一支付结果。
 *
 * <p>各支付方式返回的 {@code payInfo} 含义不同：
 * <ul>
 *   <li>支付宝 APP 支付：返回拉起 SDK 所需的订单字符串</li>
 *   <li>支付宝 PC 扫码：返回二维码链接</li>
 *   <li>微信小程序支付：返回 prepayId（前端调起 wx.requestPayment）</li>
 * </ul>
 *
 * @author eagle
 */
@Data
@Builder
public class PayResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 第三方交易号
     */
    private String tradeNo;

    /**
     * 商户订单号
     */
    private String outTradeNo;

    /**
     * 支付凭证/prepayId/二维码链接等（各支付方式含义不同）
     */
    private String payInfo;

    /**
     * 错误信息（success = false 时有值）
     */
    private String errorMessage;
}
