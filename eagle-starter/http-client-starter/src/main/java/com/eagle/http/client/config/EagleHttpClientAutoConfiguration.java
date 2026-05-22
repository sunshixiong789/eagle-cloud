package com.eagle.http.client.config;

import com.eagle.http.client.error.EagleResponseErrorHandler;
import com.eagle.http.client.interceptor.PropagatingHeadersClientHttpRequestInterceptor;
import com.eagle.http.client.interceptor.SeataXidClientHttpRequestInterceptor;
import com.eagle.http.client.interceptor.TenantClientHttpRequestInterceptor;
import com.eagle.http.client.properties.HttpClientProperties;
import com.eagle.http.client.reactive.EagleWebClientCustomizer;
import com.eagle.http.client.reactive.filter.PropagatingHeadersExchangeFilterFunction;
import com.eagle.http.client.reactive.filter.SeataXidExchangeFilterFunction;
import com.eagle.http.client.reactive.filter.TenantExchangeFilterFunction;
import com.eagle.http.client.support.EagleHttpServiceClientFactory;
import com.eagle.http.client.support.EagleRestClientCustomizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.RestClientBuilderConfigurer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.List;

/**
 * Eagle HTTP 客户端自动配置。
 *
 * <p>基于 Spring {@link RestClient} 与 HTTP Service Interface，提供服务间 HTTP 调用的
 * Header 透传、统一错误转换、超时配置、LoadBalancer 支持与声明式代理工厂。
 *
 * @author 孙士雄
 */
@Slf4j
@AutoConfiguration(after = RestClientAutoConfiguration.class)
@ConditionalOnClass(RestClient.class)
@EnableConfigurationProperties(HttpClientProperties.class)
public class EagleHttpClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PropagatingHeadersClientHttpRequestInterceptor propagatingHeadersClientHttpRequestInterceptor(
            HttpClientProperties properties) {
        return new PropagatingHeadersClientHttpRequestInterceptor(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public EagleResponseErrorHandler eagleResponseErrorHandler(ObjectProvider<ObjectMapper> objectMapperProvider) {
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        return new EagleResponseErrorHandler(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public EagleRestClientCustomizer eagleRestClientCustomizer(
            HttpClientProperties properties,
            PropagatingHeadersClientHttpRequestInterceptor propagatingHeadersInterceptor,
            ObjectProvider<TenantClientHttpRequestInterceptor> tenantInterceptor,
            ObjectProvider<SeataXidClientHttpRequestInterceptor> seataInterceptor,
            EagleResponseErrorHandler errorHandler) {
        return new EagleRestClientCustomizer(
                properties,
                List.of(propagatingHeadersInterceptor),
                tenantInterceptor.stream().toList(),
                seataInterceptor.stream().toList(),
                errorHandler);
    }

    @Bean
    @ConditionalOnMissingBean
    public HttpServiceProxyFactory eagleHttpServiceProxyFactory(RestClient.Builder restClientBuilder) {
        RestClient restClient = restClientBuilder.clone().build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build();
    }

    @Bean
    @ConditionalOnMissingBean
    public EagleHttpServiceClientFactory eagleHttpServiceClientFactory(
            RestClient.Builder restClientBuilder,
            ObjectProvider<HttpServiceProxyFactory> proxyFactory,
            @Qualifier("loadBalancedRestClientBuilder")
            ObjectProvider<RestClient.Builder> loadBalancedRestClientBuilder) {
        return new EagleHttpServiceClientFactory(
                restClientBuilder,
                loadBalancedRestClientBuilder.getIfAvailable(() -> restClientBuilder),
                proxyFactory.getIfAvailable());
    }

    /**
     * 为服务发现调用提供专用 {@link RestClient.Builder}。
     *
     * <p>使用方式：{@code factory.createLoadBalancedClient(UserClient.class, "eagle-user-service")}。
     */
    @Slf4j
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(LoadBalanced.class)
    static class LoadBalancerConfiguration {

        @Bean("loadBalancedRestClientBuilder")
        @LoadBalanced
        @ConditionalOnMissingBean(name = "loadBalancedRestClientBuilder")
        public RestClient.Builder loadBalancedRestClientBuilder(RestClientBuilderConfigurer configurer) {
            log.info("Eagle load-balanced RestClient.Builder enabled");
            return configurer.configure(RestClient.builder());
        }
    }

    /**
     * Seata XID 透传（仅在 seata-spring-boot-starter 存在于类路径时注册）。
     */
    @Slf4j
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.apache.seata.core.context.RootContext")
    static class SeataConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public SeataXidClientHttpRequestInterceptor seataXidClientHttpRequestInterceptor() {
            log.info("RestClient Seata XID propagation enabled");
            return new SeataXidClientHttpRequestInterceptor();
        }
    }

    /**
     * 租户 ID 透传（仅在 eagle-tenant-starter 存在于类路径时注册）。
     */
    @Slf4j
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.eagle.tenant.TenantContextHolder")
    static class TenantConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public TenantClientHttpRequestInterceptor tenantClientHttpRequestInterceptor() {
            log.info("RestClient tenant ID propagation enabled");
            return new TenantClientHttpRequestInterceptor();
        }
    }

    /**
     * 响应式 {@link WebClient} 装配：业务侧通过 {@code eagleWebClientBuilder} 取定制好的
     * Builder，再 {@code .baseUrl(...).build()} 构造自己的客户端。
     *
     * <p>仅在引入 {@code spring-boot-starter-webflux} 后生效。
     */
    @Slf4j
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(WebClient.class)
    static class WebClientConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public PropagatingHeadersExchangeFilterFunction propagatingHeadersExchangeFilterFunction(
                HttpClientProperties properties) {
            return new PropagatingHeadersExchangeFilterFunction(properties);
        }

        @Bean
        @ConditionalOnMissingBean
        public EagleWebClientCustomizer eagleWebClientCustomizer(
                PropagatingHeadersExchangeFilterFunction baseFilter,
                ObjectProvider<TenantExchangeFilterFunction> tenantFilter,
                ObjectProvider<SeataXidExchangeFilterFunction> seataFilter) {
            return new EagleWebClientCustomizer(
                    List.of(baseFilter),
                    tenantFilter.stream().toList(),
                    seataFilter.stream().toList());
        }

        @Bean("eagleWebClientBuilder")
        @ConditionalOnMissingBean(name = "eagleWebClientBuilder")
        public WebClient.Builder eagleWebClientBuilder(EagleWebClientCustomizer customizer) {
            WebClient.Builder builder = WebClient.builder();
            customizer.customize(builder);
            log.info("Eagle WebClient.Builder enabled");
            return builder;
        }

        @Configuration(proxyBeanMethods = false)
        @ConditionalOnClass(name = "com.eagle.tenant.TenantContextHolder")
        static class ReactiveTenantConfiguration {

            @Bean
            @ConditionalOnMissingBean
            public TenantExchangeFilterFunction tenantExchangeFilterFunction() {
                return new TenantExchangeFilterFunction();
            }
        }

        @Configuration(proxyBeanMethods = false)
        @ConditionalOnClass(name = "org.apache.seata.core.context.RootContext")
        static class ReactiveSeataConfiguration {

            @Bean
            @ConditionalOnMissingBean
            public SeataXidExchangeFilterFunction seataXidExchangeFilterFunction() {
                return new SeataXidExchangeFilterFunction();
            }
        }
    }
}
