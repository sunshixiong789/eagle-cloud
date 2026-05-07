package com.eagle.zhetaoke.jd.config;

import com.eagle.http.client.support.EagleHttpServiceClientFactory;
import com.eagle.zhetaoke.jd.client.ZhejingkeClient;
import com.eagle.zhetaoke.jd.properties.ZhejingkeProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

/**
 * 折京客 API 自动配置。
 *
 * <p>基于 {@link RestClient} 创建折京客 HTTP Service Interface 代理客户端。
 *
 * @author 孙士雄
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(RestClient.class)
@ConditionalOnProperty(name = "eagle.zhejingke.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ZhejingkeProperties.class)
public class ZhejingkeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ZhejingkeClient zhejingkeClient(ZhejingkeProperties properties,
                                           EagleHttpServiceClientFactory factory) {
        log.info("ZhejingkeClient auto-configured, baseUrl={}", properties.getBaseUrl());
        return factory.createClient(ZhejingkeClient.class, properties.getBaseUrl());
    }
}
