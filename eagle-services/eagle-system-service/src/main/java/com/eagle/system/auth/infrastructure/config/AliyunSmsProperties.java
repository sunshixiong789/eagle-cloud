package com.eagle.system.auth.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云短信服务配置属性
 *
 * <p>对应 application.yml 中的 {@code eagle.sms.aliyun} 前缀配置。</p>
 *
 * @author sunshixiong
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "eagle.sms.aliyun")
public class AliyunSmsProperties {

    /** AccessKey ID */
    private String accessKeyId = "";

    /** AccessKey Secret */
    private String accessKeySecret = "";

    /** 短信签名 */
    private String signName = "";

    /** 短信模板 Code */
    private String templateCode = "";
}
