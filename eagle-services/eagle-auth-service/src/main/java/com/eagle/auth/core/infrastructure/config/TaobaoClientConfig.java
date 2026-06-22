package com.eagle.auth.core.infrastructure.config;

import com.taobao.api.DefaultTaobaoClient;
import com.taobao.api.TaobaoClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 淘宝 TOP 客户端装配。仅当 {@code eagle.taobao.app.enabled=true} 时注册。
 *
 * <p>构造参数对照实际 SDK（{@code taobao-sdk-java-auto}，与
 * ease-mind-servers/zhetaoke-starter 同款本地 jar）确认：{@link DefaultTaobaoClient}
 * 没有 (serverUrl, appKey, appSecret, format, signMethod) 5 参构造，必须显式传超时，
 * 用 (serverUrl, appKey, appSecret, format, connectTimeoutMs, readTimeoutMs, signMethod) 7 参版本。
 *
 * @author sunshixiong
 */
@Configuration
public class TaobaoClientConfig {

    @Bean
    @ConditionalOnProperty(prefix = "eagle.taobao.app", name = "enabled", havingValue = "true")
    public TaobaoClient taobaoAppClient(TaobaoAppProperties properties) {
        return new DefaultTaobaoClient(
                properties.getServerUrl(),
                properties.getAppKey(),
                properties.getAppSecret(),
                "json",
                properties.getConnectTimeoutMs(),
                properties.getReadTimeoutMs(),
                properties.getSignMethod());
    }
}
