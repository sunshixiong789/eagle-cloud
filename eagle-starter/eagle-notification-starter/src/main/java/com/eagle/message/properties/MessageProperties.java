package com.eagle.message.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * 消息通知配置属性。
 *
 * @author 孙士雄
 */
@Data
@ConfigurationProperties(prefix = "eagle.message")
public class MessageProperties {

    /**
     * 是否启用消息通知。
     */
    private boolean enabled = true;

    /**
     * 短信配置。
     */
    private Sms sms = new Sms();

    /**
     * 邮件配置。
     */
    private Email email = new Email();

    /**
     * 消息模板配置（key 为模板编码）。
     */
    private Map<String, Template> templates = new HashMap<>();

    @Data
    public static class Sms {
        /**
         * 短信服务商：{@code aliyun} / {@code tencent} / {@code hnsls}。
         *
         * <p>注意：此字段默认值仅用于属性绑定，{@code SmsChannelConfiguration} 通过
         * {@code @ConditionalOnProperty(name = "provider")} 读取 Environment 决定是否装配，
         * 字段默认值不会触发条件——消费方需在 {@code application.yml} 显式声明本属性。
         */
        private String provider = "";

        /**
         * 服务商 AccessKey ID / SecretId（阿里云用 AK，腾讯云用 SecretId）。
         */
        private String accessKeyId = "";

        /**
         * 服务商 AccessKey Secret / SecretKey。
         */
        private String accessKeySecret = "";

        /**
         * 短信签名（阿里云、腾讯云均使用）。
         */
        private String signName = "";

        /**
         * 服务商 SMS Endpoint。
         * <p>阿里云默认 {@code dysmsapi.aliyuncs.com}；
         * 腾讯云默认 {@code sms.tencentcloudapi.com}。
         */
        private String endpoint = "dysmsapi.aliyuncs.com";

        /**
         * 腾讯云地域，例如 {@code ap-guangzhou}、{@code ap-shanghai}。仅腾讯云使用。
         */
        private String region = "ap-guangzhou";

        /**
         * 腾讯云短信应用 SdkAppId（在腾讯云短信控制台创建，例如 {@code 1400000000}）。
         * <p>仅腾讯云使用。
         */
        private String sdkAppId = "";

        /**
         * 手拉手网关账号 name。仅手拉手使用。
         */
        private String username = "";

        /**
         * 手拉手网关密码，用于生成 key。仅手拉手使用。
         */
        private String password = "";

        /**
         * 手拉手验证码短信内容模板，使用 {code} 作为占位符。
         * <p>仅手拉手使用。阿里云/腾讯云在服务端完成模板渲染，无需此字段。
         */
        private String contentTemplate = "您的验证码是{code}，5分钟内有效。";

        /**
         * 手拉手下行提交接口地址。
         * <p>仅手拉手使用。
         */
        private String sendUrl = "https://xapi.hnsls.com.cn/eums/sms/utf8/send.do";

        /**
         * 手拉手表单编码，需与接口地址匹配，可选 UTF-8 / GBK。仅手拉手使用。
         */
        private String charset = "UTF-8";

        /**
         * 短信模板 ID（阿里云为 {@code SMS_xxx}，腾讯云为数字字符串）。
         * <p>用于直接发送场景（如验证码），不经过模板引擎。Hnsls 忽略此字段。
         */
        private String templateId = "";

        /**
         * 手拉手连接超时，单位毫秒。仅手拉手使用。
         */
        private int connectTimeoutMs = 8000;

        /**
         * 手拉手读取响应超时，单位毫秒。仅手拉手使用。
         */
        private int readTimeoutMs = 10000;
    }

    @Data
    public static class Email {
        /**
         * 发件人地址。
         */
        private String from = "";
    }

    @Data
    public static class Template {
        /**
         * 主题（用于邮件），支持 ${key} 占位符。
         */
        private String subject = "";

        /**
         * 模板内容，支持 ${key} 占位符。
         */
        private String content = "";

        /**
         * 阿里云 SMS 模板 ID（如 {@code SMS_123456789}）。
         * <p>仅 SMS 渠道使用。阿里云在服务端完成渲染，应用层只需传递参数，
         * 该字段与应用层 templateCode 不同，需与阿里云控制台保持一致。
         */
        private String smsTemplateId = "";
    }
}
