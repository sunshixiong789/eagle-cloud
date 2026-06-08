package com.eagle.payment.core.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * 支付服务全量配置 (绑定 {@code eagle.payment.*})。
 *
 * <p>v1 单商户:credentials 直接从 yml / 环境变量读;v2 多商户时此配置类降级为全局默认,
 * 实际凭证由 {@code MerchantResolverPort} 按租户解析。
 *
 * @author sunshixiong
 */
@Data
@Validated
@ConfigurationProperties(prefix = "eagle.payment")
public class PaymentProperties {

    private Alipay alipay = new Alipay();
    private Wechat wechat = new Wechat();
    private Timeout timeout = new Timeout();
    private Refund refund = new Refund();
    private Transfer transfer = new Transfer();
    private Reconcile reconcile = new Reconcile();

    /** 默认支付订单过期时间 (分钟)。 */
    private int expireMinutes = 30;

    @Data
    public static class Alipay {
        private boolean enabled = false;
        private String appId = "";
        private String gatewayUrl = "https://openapi.alipay.com/gateway.do";
        private String signType = "RSA2";
        private String charset = "UTF-8";
        private String format = "json";
        /** 商户应用私钥 (PKCS8 base64);ENC() 加密 */
        private String privateKey = "";
        /** 支付宝公钥 */
        private String alipayPublicKey = "";
        /** notify_url 拼接基底,例 https://api.example.com */
        private String notifyBaseUrl = "";
    }

    @Data
    public static class Wechat {
        private boolean enabled = false;
        private String appId = "";
        private String mchId = "";
        /** 商户 API V3 密钥;ENC() 加密 */
        private String apiV3Key = "";
        /** 商户证书私钥 (PEM 字符串);ENC() 加密 */
        private String privateKey = "";
        /** 私钥证书序列号 */
        private String privateKeySerialNo = "";
        private String notifyBaseUrl = "";
    }

    @Data
    public static class Timeout {
        private int connectMs = 3000;
        private int readMs = 10000;
    }

    @Data
    public static class Refund {
        private boolean allowPartial = true;
    }

    @Data
    public static class Transfer {
        private boolean enabled = false;
        private long singleAmountLimit = 5000L;
        private long dailyAmountLimit = 50000L;
        private int dailyCountLimit = 20;
    }

    @Data
    public static class Reconcile {
        private boolean enabled = false;
        private String cron = "0 0 2 * * ?";
    }

    /** 默认下单过期时间。 */
    public Duration expireDuration() {
        return Duration.ofMinutes(expireMinutes);
    }
}
