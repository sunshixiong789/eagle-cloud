package com.eagle.auth.core.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * 短信网关基础设施配置。
 *
 * <p>声明调用短信网关专用的 {@link RestClient}，超时取自 {@link SmsProperties}。
 * 与 {@link WechatWebConfig} 同一取舍：独立声明为 Bean 便于测试替换，
 * 不在发送器内部内联创建。
 *
 * @author sunshixiong
 */
@Configuration
public class SmsClientConfig {

    /**
     * 调用短信网关的 RestClient。
     *
     * <p>短信网关是外部三方，不走服务发现，也不需要 Eagle 的 header 透传拦截器，
     * 因此直接用 {@link RestClient#builder()} 而非注入的 Eagle Builder。
     */
    @Bean("smsRestClient")
    public RestClient smsRestClient(SmsProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        return RestClient.builder().requestFactory(factory).build();
    }
}
