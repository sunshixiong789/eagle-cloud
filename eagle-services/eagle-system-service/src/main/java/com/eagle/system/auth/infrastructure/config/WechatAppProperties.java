package com.eagle.system.auth.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信 App 登录配置属性。
 *
 * <p>对应 application.yml 中的 {@code eagle.wechat.app} 前缀配置。</p>
 *
 * @author sunshixiong
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "eagle.wechat.app")
public class WechatAppProperties {

    /**
     * 开放平台移动应用 AppId
     */
    private String appId = "";

    /**
     * 开放平台移动应用 AppSecret
     */
    private String appSecret = "";
}
