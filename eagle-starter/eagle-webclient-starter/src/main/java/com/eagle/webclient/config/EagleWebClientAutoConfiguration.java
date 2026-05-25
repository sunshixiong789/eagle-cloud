package com.eagle.webclient.config;

import com.eagle.common.http.HttpClientProperties;
import com.eagle.webclient.error.EagleWebClientErrorFilter;
import com.eagle.webclient.interceptor.PropagatingHeadersExchangeFilterFunction;
import com.eagle.webclient.interceptor.SeataXidExchangeFilterFunction;
import com.eagle.webclient.interceptor.TenantExchangeFilterFunction;
import com.eagle.webclient.support.EagleReactiveServiceClientFactory;
import com.eagle.webclient.support.EagleWebClientCustomizer;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Eagle WebClient 反应式 HTTP 客户端自动配置。
 *
 * <p>提供：
 * <ul>
 *   <li>统一错误转换 {@link EagleWebClientErrorFilter}</li>
 *   <li>入站 Header / 压测标记 / 租户 ID / Seata XID 透传 filter（reactive 版）</li>
 *   <li>{@code eagleWebClientBuilder} 与 LoadBalancer 版本</li>
 *   <li>声明式接口工厂 {@link EagleReactiveServiceClientFactory}</li>
 * </ul>
 *
 * <p>同步阻塞场景请使用 {@code eagle-restclient-starter}。
 *
 * @author eagle
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(WebClient.class)
@EnableConfigurationProperties(HttpClientProperties.class)
public class EagleWebClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public EagleWebClientErrorFilter eagleWebClientErrorFilter(
            ObjectProvider<ObjectMapper> objectMapperProvider) {
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        return new EagleWebClientErrorFilter(objectMapper);
    }

    /**
     * Reactive 版入站 Header / 压测标记透传 filter。
     *
     * <p>入站 header 透传依赖 {@link org.springframework.web.server.ServerWebExchange}
     * 存在于 Reactor Context（WebFlux web 处理链路自动注入），非 web 上下文跳过；
     * 压测标记走 {@code PressureTestContext}（已桥接到 Reactor Context），全场景可用。
     */
    @Bean
    @ConditionalOnMissingBean
    public PropagatingHeadersExchangeFilterFunction propagatingHeadersExchangeFilterFunction(
            HttpClientProperties properties) {
        return new PropagatingHeadersExchangeFilterFunction(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public EagleWebClientCustomizer eagleWebClientCustomizer(
            HttpClientProperties properties,
            ObjectProvider<PropagatingHeadersExchangeFilterFunction> propagatingHeadersFilter,
            ObjectProvider<TenantExchangeFilterFunction> tenantFilter,
            ObjectProvider<SeataXidExchangeFilterFunction> seataFilter,
            EagleWebClientErrorFilter errorFilter) {
        return new EagleWebClientCustomizer(
                properties,
                propagatingHeadersFilter.stream().<ExchangeFilterFunction>map(f -> f).toList(),
                tenantFilter.stream().<ExchangeFilterFunction>map(f -> f).toList(),
                seataFilter.stream().<ExchangeFilterFunction>map(f -> f).toList(),
                errorFilter);
    }

    @Bean("eagleWebClientBuilder")
    @ConditionalOnMissingBean(name = "eagleWebClientBuilder")
    public WebClient.Builder eagleWebClientBuilder(EagleWebClientCustomizer customizer) {
        WebClient.Builder builder = WebClient.builder();
        customizer.customize(builder);
        log.info("Eagle WebClient.Builder enabled");
        return builder;
    }

    @Bean
    @ConditionalOnMissingBean
    public EagleReactiveServiceClientFactory eagleReactiveServiceClientFactory(
            @Qualifier("eagleWebClientBuilder") WebClient.Builder webClientBuilder,
            @Qualifier("loadBalancedWebClientBuilder")
            ObjectProvider<WebClient.Builder> loadBalancedWebClientBuilder) {
        return new EagleReactiveServiceClientFactory(
                webClientBuilder,
                loadBalancedWebClientBuilder.getIfAvailable(() -> webClientBuilder));
    }

    /**
     * Reactive 服务发现客户端 builder（仅在 spring-cloud-loadbalancer 存在时注册）。
     */
    @Slf4j
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(LoadBalanced.class)
    static class LoadBalancedWebClientConfiguration {

        @Bean("loadBalancedWebClientBuilder")
        @LoadBalanced
        @ConditionalOnMissingBean(name = "loadBalancedWebClientBuilder")
        public WebClient.Builder loadBalancedWebClientBuilder(EagleWebClientCustomizer customizer) {
            WebClient.Builder builder = WebClient.builder();
            customizer.customize(builder);
            log.info("Eagle load-balanced WebClient.Builder enabled");
            return builder;
        }
    }

    /**
     * Reactive Seata XID 透传 filter。
     */
    @Slf4j
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.apache.seata.core.context.RootContext")
    static class ReactiveSeataConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public SeataXidExchangeFilterFunction seataXidExchangeFilterFunction() {
            log.info("WebClient Seata XID propagation enabled");
            return new SeataXidExchangeFilterFunction();
        }
    }

    /**
     * Reactive 租户 ID 透传 filter。
     */
    @Slf4j
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.eagle.tenant.TenantContextHolder")
    static class ReactiveTenantConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public TenantExchangeFilterFunction tenantExchangeFilterFunction() {
            log.info("WebClient tenant ID propagation enabled");
            return new TenantExchangeFilterFunction();
        }
    }
}
