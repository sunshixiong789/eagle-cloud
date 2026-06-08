package com.eagle.http.client.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link EagleRestServiceClientFactory} 单元测试。
 *
 * @author eagle
 */
class EagleRestServiceClientFactoryTest {

    @Test
    @DisplayName("应创建声明式 HTTP 服务客户端")
    void shouldCreateDeclarativeHttpServiceClient() {
        EagleRestServiceClientFactory factory = new EagleRestServiceClientFactory();

        InventoryClient client = factory.createClient(InventoryClient.class, "http://inventory.example");

        assertThat(client).isNotNull();
    }

    @Test
    @DisplayName("应根据服务 ID 创建负载均衡客户端")
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
