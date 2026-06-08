package com.eagle.webclient.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link EagleReactiveServiceClientFactory} 单元测试。
 *
 * @author eagle
 */
class EagleReactiveServiceClientFactoryTest {

    @Test
    @DisplayName("应创建声明式响应式客户端")
    void shouldCreateDeclarativeReactiveClient() {
        EagleReactiveServiceClientFactory factory = new EagleReactiveServiceClientFactory(
                WebClient.builder(), WebClient.builder());

        ReactiveInventoryClient client = factory.createClient(
                ReactiveInventoryClient.class, "http://inventory.example");

        assertThat(client).isNotNull();
    }

    @Test
    @DisplayName("应根据服务 ID 创建负载均衡响应式客户端")
    void shouldCreateLoadBalancedReactiveClientFromServiceId() {
        EagleReactiveServiceClientFactory factory = new EagleReactiveServiceClientFactory(
                WebClient.builder(), WebClient.builder());

        ReactiveInventoryClient client = factory.createLoadBalancedClient(
                ReactiveInventoryClient.class, "eagle-inventory-server");

        assertThat(client).isNotNull();
    }

    @HttpExchange("/api/items")
    interface ReactiveInventoryClient {

        @GetExchange("/{id}")
        Mono<String> getItem(@PathVariable Long id);
    }
}
