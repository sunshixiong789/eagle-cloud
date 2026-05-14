package com.eagle.system.auth.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 腾讯云短信服务配置属性
 *
 * <p>对应 application.yml 中的 {@code eagle.sms.tencent} 前缀配置。</p>
 *
 * @author sunshixiong
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "eagle.sms.tencent")
public class TencentSmsProperties {

    /**
     * SecretId
     */
    private String accessKeyId = "";

    /**
     * SecretKey
     */
    private String accessKeySecret = "";

    /**
     * 短信签名
     */
    private String signName = "";

    /**
     * 短信模板 ID（腾讯云为数字字符串，如 1234567）
     */
    private String templateId = "";

    /**
     * 短信应用 SdkAppId（腾讯云短信控制台分配）
     */
    private String sdkAppId = "";

    /**
     * 地域，如 ap-guangzhou / ap-shanghai
     */
    private String region = "ap-guangzhou";
}
