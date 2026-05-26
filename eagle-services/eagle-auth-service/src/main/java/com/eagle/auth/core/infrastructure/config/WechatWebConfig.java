package com.eagle.auth.core.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * 微信 Web 端 OAuth2 基础设施配置
 * <p>
 * 声明调用微信 API 所需的 {@link RestClient} Bean，
 * 与 {@link WechatWebProperties} 配合使用。
 *
 * @author sunshixiong
 */
@Configuration
public class WechatWebConfig {

    /**
     * 用于调用微信 API 的 RestClient
     * <p>
     * 独立声明为 Bean，便于在测试中通过 Mock 替换，
     * 避免在 {@link com.eagle.auth.core.infrastructure.external.WechatWebServiceImpl} 中内联创建。
     */
    @Bean("wechatRestClient")
    public RestClient wechatRestClient() {
        return RestClient.create();
    }
}
