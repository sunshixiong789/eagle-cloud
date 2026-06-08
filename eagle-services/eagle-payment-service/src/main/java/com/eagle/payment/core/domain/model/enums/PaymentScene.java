package com.eagle.payment.core.domain.model.enums;

/**
 * 支付场景枚举(决定渠道 SDK 走哪条接入路径与返回什么类型的支付参数)。
 *
 * <ul>
 *   <li>{@link #PC_WEB}        - 支付宝电脑网站 / 微信扫码;返回 form / qrCode</li>
 *   <li>{@link #MOBILE_WEB}    - 支付宝/微信 H5 支付;返回支付页 URL</li>
 *   <li>{@link #APP}           - 支付宝/微信 APP 支付;返回 orderInfo / prepayId</li>
 *   <li>{@link #MINI_PROGRAM}  - 微信小程序 / 支付宝小程序支付;返回 prepayId + paySign</li>
 *   <li>{@link #JSAPI}         - 微信公众号 / 支付宝服务窗内支付;返回 jsapi 参数</li>
 *   <li>{@link #NATIVE_QR}     - 微信 Native 扫码支付;返回 codeUrl</li>
 * </ul>
 *
 * @author sunshixiong
 */
public enum PaymentScene {
    PC_WEB,
    MOBILE_WEB,
    APP,
    MINI_PROGRAM,
    JSAPI,
    NATIVE_QR
}
