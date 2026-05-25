package com.eagle.http.client.support;

import lombok.NoArgsConstructor;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.Objects;

/**
 * 同步阻塞 HTTP Service Interface 代理工厂（基于 {@link RestClient}）。
 *
 * <p>适合在业务配置中创建声明式客户端：
 * <pre>{@code
 * @Bean
 * InventoryClient inventoryClient(EagleRestServiceClientFactory factory) {
 *     return factory.createLoadBalancedClient(InventoryClient.class, "eagle-inventory-service");
 * }
 * }</pre>
 *
 * <p>反应式（WebFlux）路径请使用 {@code eagle-webclient-starter} 提供的
 * {@code EagleReactiveServiceClientFactory}。
 *
 * @author eagle
 */
@NoArgsConstructor
public class EagleRestServiceClientFactory {

    private RestClient.Builder restClientBuilder = RestClient.builder();

    private RestClient.Builder loadBalancedRestClientBuilder = RestClient.builder();

    private HttpServiceProxyFactory defaultProxyFactory;

    public EagleRestServiceClientFactory(RestClient.Builder restClientBuilder,
                                         RestClient.Builder loadBalancedRestClientBuilder,
                                         HttpServiceProxyFactory defaultProxyFactory) {
        this.restClientBuilder = restClientBuilder;
        this.loadBalancedRestClientBuilder = loadBalancedRestClientBuilder;
        this.defaultProxyFactory = defaultProxyFactory;
    }

    public <T> T createClient(Class<T> serviceType) {
        if (defaultProxyFactory != null) {
            return defaultProxyFactory.createClient(serviceType);
        }
        return createClient(serviceType, restClientBuilder.clone().build());
    }

    public <T> T createClient(Class<T> serviceType, String baseUrl) {
        Objects.requireNonNull(baseUrl, "baseUrl must not be null");
        RestClient restClient = restClientBuilder.clone().baseUrl(baseUrl).build();
        return createClient(serviceType, restClient);
    }

    public <T> T createLoadBalancedClient(Class<T> serviceType, String serviceId) {
        Objects.requireNonNull(serviceId, "serviceId must not be null");
        String baseUrl = serviceId.startsWith("http://") || serviceId.startsWith("https://")
                ? serviceId
                : "http://" + serviceId;
        RestClient restClient = loadBalancedRestClientBuilder.clone().baseUrl(baseUrl).build();
        return createClient(serviceType, restClient);
    }

    public <T> T createClient(Class<T> serviceType, RestClient restClient) {
        Objects.requireNonNull(serviceType, "serviceType must not be null");
        Objects.requireNonNull(restClient, "restClient must not be null");
        HttpServiceProxyFactory proxyFactory =
                HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build();
        return proxyFactory.createClient(serviceType);
    }
}
