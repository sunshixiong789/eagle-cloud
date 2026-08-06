package com.eagle.http.client.config;

import com.eagle.common.http.HttpClientProperties;
import com.eagle.http.client.error.EagleResponseErrorHandler;
import com.eagle.http.client.interceptor.PropagatingHeadersClientHttpRequestInterceptor;
import com.eagle.http.client.interceptor.SeataXidClientHttpRequestInterceptor;
import com.eagle.http.client.support.EagleRestClientCustomizer;
import com.eagle.http.client.support.EagleRestServiceClientFactory;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.RestClientBuilderConfigurer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Eagle RestClient 同步 HTTP 客户端自动配置。
 *
 * <p>提供：
 * <ul>
 *   <li>统一错误转换 {@link EagleResponseErrorHandler}</li>
 *   <li>入站 Header / 压测标记 / 租户 ID / Seata XID 透传拦截器</li>
 *   <li>{@link RestClient.Builder} 与 LoadBalancer 版本</li>
 *   <li>声明式接口工厂 {@link EagleRestServiceClientFactory}</li>
 * </ul>
 *
 * <p>反应式（WebFlux）场景请使用 {@code eagle-webclient-starter}。
 *
 * @author eagle
 */
@Slf4j
@AutoConfiguration(after = RestClientAutoConfiguration.class)
@ConditionalOnClass(RestClient.class)
@EnableConfigurationProperties(HttpClientProperties.class)
public class EagleRestClientAutoConfiguration {

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
            ObjectProvider<PropagatingHeadersClientHttpRequestInterceptor> propagatingHeadersInterceptor,
            ObjectProvider<SeataXidClientHttpRequestInterceptor> seataInterceptor,
            EagleResponseErrorHandler errorHandler) {
        return new EagleRestClientCustomizer(
                properties,
                propagatingHeadersInterceptor.stream().<ClientHttpRequestInterceptor>map(i -> i).toList(),
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
    public EagleRestServiceClientFactory eagleRestServiceClientFactory(
            RestClient.Builder restClientBuilder,
            ObjectProvider<HttpServiceProxyFactory> proxyFactory,
            @Qualifier("loadBalancedRestClientBuilder")
            ObjectProvider<RestClient.Builder> loadBalancedRestClientBuilder) {
        return new EagleRestServiceClientFactory(
                restClientBuilder,
                loadBalancedRestClientBuilder.getIfAvailable(() -> restClientBuilder),
                proxyFactory.getIfAvailable());
    }

    /**
     * 入站 Header 透传拦截器：依赖 {@link HttpServletRequest}，仅在 servlet web 环境注册。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(HttpServletRequest.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    static class ServletPropagationConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public PropagatingHeadersClientHttpRequestInterceptor propagatingHeadersClientHttpRequestInterceptor(
                HttpClientProperties properties) {
            return new PropagatingHeadersClientHttpRequestInterceptor(properties);
        }
    }

    /**
     * 为服务发现调用提供专用 {@link RestClient.Builder}。
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

}
