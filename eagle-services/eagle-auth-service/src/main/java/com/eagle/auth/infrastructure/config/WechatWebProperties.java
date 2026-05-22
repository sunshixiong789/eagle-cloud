package com.eagle.auth.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 微信 Web 端登录配置属性
 * <p>
 * 包含两种 Web 场景的凭证：
 * <ul>
 *   <li>{@code pc}：微信开放平台网站应用（PC 扫码登录）</li>
 *   <li>{@code h5}：微信公众号网页授权（H5 网页登录）</li>
 * </ul>
 *
 * @author sunshixiong
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "eagle.wechat.web")
public class WechatWebProperties {

    /**
     * 是否启用微信网页登录（PC 扫码 / H5 授权），默认关闭
     */
    private boolean enabled = false;

    /**
     * PC 扫码登录（微信开放平台网站应用）配置
     */
    private Pc pc = new Pc();

    /**
     * H5 网页授权（微信公众号）配置
     */
    private H5 h5 = new H5();

    /**
     * 微信开放平台网站应用配置（PC 扫码）
     */
    @Getter
    @Setter
    public static class Pc {

        /**
         * 开放平台网站应用 AppId
         */
        private String appId = "";

        /**
         * 开放平台网站应用 AppSecret
         */
        private String appSecret = "";

        /**
         * 回调地址，需在微信开放平台配置白名单
         */
        private String redirectUri = "";
    }

    /**
     * 微信公众号网页授权配置（H5）
     */
    @Getter
    @Setter
    public static class H5 {

        /**
         * 公众号 AppId
         */
        private String appId = "";

        /**
         * 公众号 AppSecret
         */
        private String appSecret = "";

        /**
         * 回调地址，需在公众号后台配置网页授权域名
         */
        private String redirectUri = "";
    }
}
