---
name: eagle-webclient
description: Use when implementing reactive HTTP clients in eagle-cloud WebFlux services — EagleReactiveServiceClientFactory for @HttpExchange proxies, EagleWebClientCustomizer auto-applies propagating headers / tenant / Seata-XID filter functions, EagleWebClientErrorFilter converts 4xx/5xx to AppException, for Servlet services use eagle-restclient instead
---

# eagle-webclient-starter — 响应式 HTTP 客户端（WebFlux）

## 何时使用

- 服务使用 **Spring WebFlux**，需要非阻塞调用其他服务
- 网关层（`eagle-gateway-server`）或 R2DBC 服务调用下游
- 需要自动透传 JWT / 租户 ID / Seata XID（响应式版本）

## 何时不要使用

- 普通 Servlet 服务（非 WebFlux）→ 用 `eagle-restclient-starter`（阻塞 RestClient）
- 禁止在 WebFlux 路径中使用 RestTemplate / `RestClient`（会阻塞 Netty EventLoop）

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-webclient-starter')
```

引入后 `WebClient.Builder`（含全部 Filter）、`EagleReactiveServiceClientFactory`、`EagleWebClientCustomizer` 自动注册。

```yaml
eagle:
  http-client:
    connect-timeout: 2s
    read-timeout: 5s
    error-handler-enabled: true
    propagated-headers:
      - Authorization
      - Accept-Language
      - X-Request-Id
```

## 核心组件

| 类                                       | 用途                                                    |
|-----------------------------------------|-------------------------------------------------------|
| `EagleReactiveServiceClientFactory`     | 从 `WebClient.Builder` 构建声明式 HTTP Service Interface 代理 |
| `EagleWebClientCustomizer`              | 全局注册 Filter：Header 透传 / 租户 / Seata XID               |
| `PropagatingHeadersExchangeFilterFunction` | 透传 `Authorization`、`Accept-Language`、请求 ID 等          |
| `TenantExchangeFilterFunction`          | 透传 `X-Tenant-Id`（`eagle-tenant-starter` 存在时激活）        |
| `SeataXidExchangeFilterFunction`        | 透传 `TX_XID`（Seata 存在时激活）                              |
| `EagleWebClientErrorFilter`             | 4xx/5xx 转为 `AppException` 体系                          |

## 声明式客户端（`@HttpExchange`）

```java
// 1) 接口定义（infrastructure/remote/）
@HttpExchange("/api/v1/inventory")
public interface ReactiveInventoryClient {

    @GetExchange("/{productId}/stock")
    Mono<StockResponse> getStock(@PathVariable Long productId);

    @PostExchange("/lock")
    Mono<Void> lockStock(@RequestBody LockStockRequest request);

    @GetExchange("/items")
    Flux<ItemResponse> streamItems();
}

// 2) 创建 Bean（负载均衡版本）
@Configuration(proxyBeanMethods = false)
class ReactiveRemoteConfig {

    @Bean
    ReactiveInventoryClient inventoryClient(EagleReactiveServiceClientFactory factory) {
        return factory.createLoadBalancedClient(
                ReactiveInventoryClient.class, "eagle-inventory-service");
    }

    // 指定 baseUrl 版本
    @Bean
    ReactiveInventoryClient inventoryClientFixed(EagleReactiveServiceClientFactory factory) {
        return factory.createClient(ReactiveInventoryClient.class, "http://inventory:8082");
    }
}

// 3) 业务使用
@Service
@RequiredArgsConstructor
public class OrderApplicationService {

    private final ReactiveInventoryClient inventoryClient;

    public Mono<OrderResponse> createOrder(CreateOrderRequest request) {
        return inventoryClient.getStock(request.productId())
                .flatMap(stock -> {
                    if (stock.available() < request.quantity()) {
                        return Mono.error(OrderErrorCode.INSUFFICIENT_STOCK.toDomainException());
                    }
                    return inventoryClient.lockStock(new LockStockRequest(...));
                })
                .thenReturn(OrderResponse.of(...));
    }
}
```

## 直接使用 WebClient

```java
// 外部 API（不需要服务发现）
@Bean
WebClient wechatWebClient(WebClient.Builder builder) {
    return builder.clone()
            .baseUrl("https://api.weixin.qq.com")
            .defaultHeader("Accept", "application/json")
            .build();
}
```

## 错误转换

| 下游 HTTP 状态     | 抛出异常                |
|-----------------|---------------------|
| `400`           | `DomainException`   |
| `404`           | `NotFoundException` |
| `409`           | `ConflictException` |
| `403/429/5xx/其他`| `ServiceException`  |

调用方通常无需捕获 HTTP 异常，由全局异常处理器统一处理（WebFlux 版）。

## 常见错误

- ❌ Servlet 服务引入 `eagle-webclient-starter` → ✅ Servlet 用 `eagle-restclient-starter`
- ❌ WebFlux 路径调用 `RestClient`（阻塞）→ ✅ 用 `WebClient` 或声明式 `@HttpExchange`
- ❌ 在 `flatMap` 内抛受检异常 → ✅ 包装为 `Mono.error(new RuntimeException(e))`
- ❌ 未订阅 `Mono/Flux` 导致调用无效 → ✅ 确保调用链在 Controller 层 `return`

## 关联规则

- `.claude/rules/11-feign.md` — HTTP Client 位置 / 错误处理
- `.claude/rules/12-security.md` — JWT 透传规范
- `.claude/rules/17-tenant-permission.md` — 租户 ID 透传
