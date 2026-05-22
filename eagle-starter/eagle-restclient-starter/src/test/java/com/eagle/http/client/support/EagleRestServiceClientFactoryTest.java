package com.eagle.http.client.support;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link EagleRestServiceClientFactory} 单元测试。
 *
 * @author 孙士雄
 */
class EagleRestServiceClientFactoryTest {

    @Test
    void shouldCreateDeclarativeHttpServiceClient() {
        EagleRestServiceClientFactory factory = new EagleRestServiceClientFactory();

        InventoryClient client = factory.createClient(InventoryClient.class, "http://inventory.example");

        assertThat(client).isNotNull();
    }

    @Test
    void shouldCreateLoadBalancedClientFromServiceId() {
        EagleRestServiceClientFactory factory = new EagleRestServiceClientFactory();

        InventoryClient client = factory.createLoadBalancedClient(InventoryClient.class, "eagle-inventory-server");

        assertThat(client).isNotNull();
    }

    @HttpExchange("/api/items")
    interface InventoryClient {

        @GetExchange("/{id}")
        String getItem(@PathVariable Long id);
    }
}
