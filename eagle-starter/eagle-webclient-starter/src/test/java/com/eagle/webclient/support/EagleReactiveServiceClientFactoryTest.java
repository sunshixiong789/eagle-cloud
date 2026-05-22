package com.eagle.webclient.support;

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
 * @author 孙士雄
 */
class EagleReactiveServiceClientFactoryTest {

    @Test
    void shouldCreateDeclarativeReactiveClient() {
        EagleReactiveServiceClientFactory factory = new EagleReactiveServiceClientFactory(
                WebClient.builder(), WebClient.builder());

        ReactiveInventoryClient client = factory.createClient(
                ReactiveInventoryClient.class, "http://inventory.example");

        assertThat(client).isNotNull();
    }

    @Test
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
