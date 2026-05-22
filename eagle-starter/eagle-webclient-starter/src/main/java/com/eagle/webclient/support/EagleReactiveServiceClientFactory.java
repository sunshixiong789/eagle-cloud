package com.eagle.webclient.support;

import lombok.NoArgsConstructor;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.Objects;

/**
 * 反应式 HTTP Service Interface 代理工厂（基于 {@link WebClient}）。
 *
 * <p>适合 WebFlux 业务服务中创建声明式响应式客户端：
 * <pre>{@code
 * @Bean
 * ReactiveInventoryClient inventoryClient(EagleReactiveServiceClientFactory factory) {
 *     return factory.createLoadBalancedClient(
 *             ReactiveInventoryClient.class, "eagle-inventory-service");
 * }
 *
 * @HttpExchange("/api/items")
 * interface ReactiveInventoryClient {
 *     @GetExchange("/{id}") Mono<ItemDto> getItem(@PathVariable Long id);
 * }
 * }</pre>
 *
 * <p>同步阻塞场景请使用 {@code eagle-restclient-starter} 提供的
 * {@code EagleRestServiceClientFactory}。
 *
 * @author 孙士雄
 */
@NoArgsConstructor
public class EagleReactiveServiceClientFactory {

    private WebClient.Builder webClientBuilder = WebClient.builder();

    private WebClient.Builder loadBalancedWebClientBuilder = WebClient.builder();

    public EagleReactiveServiceClientFactory(WebClient.Builder webClientBuilder,
                                             WebClient.Builder loadBalancedWebClientBuilder) {
        this.webClientBuilder = webClientBuilder;
        this.loadBalancedWebClientBuilder = loadBalancedWebClientBuilder;
    }

    public <T> T createClient(Class<T> serviceType) {
        return createClient(serviceType, webClientBuilder.clone().build());
    }

    public <T> T createClient(Class<T> serviceType, String baseUrl) {
        Objects.requireNonNull(baseUrl, "baseUrl must not be null");
        WebClient webClient = webClientBuilder.clone().baseUrl(baseUrl).build();
        return createClient(serviceType, webClient);
    }

    public <T> T createLoadBalancedClient(Class<T> serviceType, String serviceId) {
        Objects.requireNonNull(serviceId, "serviceId must not be null");
        String baseUrl = serviceId.startsWith("http://") || serviceId.startsWith("https://")
                ? serviceId
                : "http://" + serviceId;
        WebClient webClient = loadBalancedWebClientBuilder.clone().baseUrl(baseUrl).build();
        return createClient(serviceType, webClient);
    }

    public <T> T createClient(Class<T> serviceType, WebClient webClient) {
        Objects.requireNonNull(serviceType, "serviceType must not be null");
        Objects.requireNonNull(webClient, "webClient must not be null");
        HttpServiceProxyFactory proxyFactory =
                HttpServiceProxyFactory.builderFor(WebClientAdapter.create(webClient)).build();
        return proxyFactory.createClient(serviceType);
    }
}
