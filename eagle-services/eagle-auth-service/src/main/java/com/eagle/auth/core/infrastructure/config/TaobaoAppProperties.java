package com.eagle.auth.core.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 淘宝登录配置（TOP 解析 openUid 用）。
 *
 * <p>对应 application.yml 中的 {@code eagle.taobao.app} 前缀。与 user-service 的
 * {@code eagle.taobao.top}（联盟绑定）相互独立，各服务自管。
 *
 * @author sunshixiong
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "eagle.taobao.app")
public class TaobaoAppProperties {

    /**
     * 是否启用淘宝登录（false 时 TaobaoServiceImpl 直接报 upstream，不发起 TOP 调用）
     */
    private boolean enabled = false;

    /**
     * TOP 网关
     */
    private String serverUrl = "https://eco.taobao.com/router/rest";

    /**
     * 应用 App Key
     */
    private String appKey = "";

    /**
     * 应用 App Secret
     */
    private String appSecret = "";

    /**
     * 签名算法：md5 / hmac-sha256
     */
    private String signMethod = "md5";

    /**
     * 连接超时（毫秒）
     */
    private int connectTimeoutMs = 3000;

    /**
     * 读超时（毫秒）
     */
    private int readTimeoutMs = 10000;
}
