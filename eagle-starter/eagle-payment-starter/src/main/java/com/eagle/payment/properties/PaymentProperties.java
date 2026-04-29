package com.eagle.payment.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * 支付统一配置属性。
 *
 * <p>通过 {@code eagle.payment.*} 外部化配置支付宝和微信支付的接入参数。
 * 敏感字段（私钥、密钥）建议通过环境变量或 Nacos 加密配置注入，不得硬编码在代码库中。
 *
 * @author eagle
 */
@Data
@ConfigurationProperties(prefix = "eagle.payment")
public class PaymentProperties {

    /** 支付宝配置 */
    @NestedConfigurationProperty
    private Alipay alipay = new Alipay();

    /** 微信支付配置 */
    @NestedConfigurationProperty
    private Wechat wechat = new Wechat();

    /**
     * 支付宝配置属性。
     */
    @Data
    public static class Alipay {

        /** 支付宝应用 ID */
        private String appId;

        /** 商户私钥（PKCS8 格式） */
        private String privateKey;

        /** 支付宝公钥（用于验签） */
        private String alipayPublicKey;

        /** 网关地址，默认正式环境 */
        private String serverUrl = "https://openapi.alipay.com/gateway.do";

        /** 签名类型 */
        private String signType = "RSA2";

        /** 字符集 */
        private String charset = "UTF-8";

        /** 异步通知地址 */
        private String notifyUrl;

        /** 同步跳转地址 */
        private String returnUrl;
    }

    /**
     * 微信支付配置属性。
     */
    @Data
    public static class Wechat {

        /** 商户号 */
        private String mchId;

        /** 商户 API 证书序列号 */
        private String mchSerialNo;

        /** 商户 API 私钥（PKCS8） */
        private String privateKey;

        /** APIv3 密钥（32 字节） */
        private String apiV3Key;

        /** 小程序/公众号 AppId */
        private String appId;

        /** 异步通知地址 */
        private String notifyUrl;
    }
}
