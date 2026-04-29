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
         * 阿里云 AccessKey ID。
         */
        private String accessKeyId = "";

        /**
         * 阿里云 AccessKey Secret。
         */
        private String accessKeySecret = "";

        /**
         * 短信签名。
         */
        private String signName = "";

        /**
         * 阿里云 SMS Endpoint。
         */
        private String endpoint = "dysmsapi.aliyuncs.com";
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
