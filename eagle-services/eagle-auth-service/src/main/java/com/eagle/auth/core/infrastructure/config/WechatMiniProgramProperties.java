package com.eagle.auth.core.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信小程序配置属性
 *
 * <p>对应 application.yml 中的 {@code eagle.wechat.mini-program} 前缀配置。</p>
 *
 * @author sunshixiong
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "eagle.wechat.mini-program")
public class WechatMiniProgramProperties {

    /**
     * 小程序 AppID
     */
    private String appId = "";

    /**
     * 小程序 AppSecret
     */
    private String appSecret = "";
}
