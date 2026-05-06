package com.eagle.zhetaoke.config;

import com.eagle.http.client.support.EagleHttpServiceClientFactory;
import com.eagle.zhetaoke.client.ZhetaokeClient;
import com.eagle.zhetaoke.properties.ZhetaokeProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

/**
 * 折淘客 API 自动配置。
 *
 * <p>基于 {@link RestClient} 创建折淘客 HTTP Service Interface 代理客户端，
 * 支持主/备用域名切换与统一超时配置。
 *
 * @author 孙士雄
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(RestClient.class)
@ConditionalOnProperty(name = "eagle.zhetaoke.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ZhetaokeProperties.class)
public class ZhetaokeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ZhetaokeClient zhetaokeClient(ZhetaokeProperties properties,
                                         EagleHttpServiceClientFactory factory) {
        log.info("ZhetaokeClient auto-configured, baseUrl={}", properties.getBaseUrl());
        return factory.createClient(ZhetaokeClient.class, properties.getBaseUrl());
    }
}
