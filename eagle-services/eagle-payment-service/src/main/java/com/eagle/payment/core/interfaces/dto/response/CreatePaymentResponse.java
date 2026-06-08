package com.eagle.payment.core.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 创建支付订单响应。
 *
 * <p>{@link #payload} 与 {@link #payloadType} 含义随 scene 不同:
 * <ul>
 *   <li>支付宝 PC_WEB / MOBILE_WEB - payload = HTML form (html-form),前端直接 echo</li>
 *   <li>支付宝 APP                  - payload = orderInfo (order-info),客户端拉起 SDK</li>
 *   <li>支付宝 NATIVE_QR            - payload = qrCode URL (qr-code),前端渲染二维码</li>
 *   <li>微信 NATIVE_QR              - payload = codeUrl (code-url),前端渲染二维码</li>
 *   <li>微信 JSAPI/MINI_PROGRAM/APP - payload = prepayId (prepay-id),客户端二次签名拉起</li>
 *   <li>微信 MOBILE_WEB             - payload = mwebUrl (h5-url),前端 location.href 跳转</li>
 * </ul>
 *
 * @author sunshixiong
 */
@Schema(description = "创建支付订单响应")
public record CreatePaymentResponse(
        @Schema(description = "支付订单 ID", example = "1024") Long id,
        @Schema(description = "支付服务侧交易号", example = "PAY16812340000001") String outTradeNo,
        @Schema(description = "渠道支付载荷,含义取决于 payloadType") String payload,
        @Schema(description = "载荷类型,客户端按此识别 payload 含义",
                example = "html-form") String payloadType
) {
}
