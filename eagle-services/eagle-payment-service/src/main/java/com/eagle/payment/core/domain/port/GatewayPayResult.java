package com.eagle.payment.core.domain.port;

import org.jspecify.annotations.Nullable;

/**
 * 渠道下单返回 (各场景的支付参数不同,统一以 String payload 承载;调用方按 scene 解读)。
 *
 * <p>典型 payload:
 * <ul>
 *   <li>支付宝 PC_WEB - HTML form 字符串,直接 echo 给浏览器</li>
 *   <li>支付宝 MOBILE_WEB - 同上,移动端 H5 form</li>
 *   <li>支付宝 APP - orderInfo 字符串</li>
 *   <li>支付宝 NATIVE_QR - qrCode 二维码 URL</li>
 *   <li>微信 NATIVE_QR - codeUrl (weixin://wxpay/bizpayurl?pr=xxx)</li>
 *   <li>微信 JSAPI / MINI_PROGRAM - JSON (prepay_id + paySign 等)</li>
 *   <li>微信 APP - JSON (prepay_id + sign 等)</li>
 *   <li>微信 MOBILE_WEB - H5 跳转 URL (mweb_url)</li>
 * </ul>
 *
 * @author sunshixiong
 */
public record GatewayPayResult(
        String channelTradeNo,
        String payload,
        @Nullable String payloadType
) {
}
