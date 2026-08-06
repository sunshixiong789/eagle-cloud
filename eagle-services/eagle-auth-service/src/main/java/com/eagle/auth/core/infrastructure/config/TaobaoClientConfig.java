package com.eagle.auth.core.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 淘宝 TOP 网关 RestClient 装配。
 *
 * <p>不再依赖淘宝官方 TOP SDK（本地 jar，无 Nexus/Maven 坐标）——
 * {@link com.eagle.auth.core.infrastructure.external.TaobaoServiceImpl} 只用到
 * {@code taobao.openuid.get} / {@code taobao.top.auth.token.create} 两个接口，
 * 已改为手写 RestClient 直调 + TOP 签名算法，与
 * {@link com.eagle.auth.core.infrastructure.external.WechatMiniProgramServiceImpl}
 * 处理微信小程序登录同一套思路。
 *
 * @author sunshixiong
 */
@Configuration(proxyBeanMethods = false)
public class TaobaoClientConfig {

    @Bean("taobaoRestClient")
    RestClient taobaoRestClient(TaobaoAppProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(properties.getReadTimeoutMs());
        return RestClient.builder().requestFactory(requestFactory).build();
    }
}
