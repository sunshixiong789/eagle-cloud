# eagle-webclient-starter

基于 Spring `WebClient` 与 HTTP Service Interface (`@HttpExchange`) 的 **反应式** 远程调用 starter。
适用于 WebFlux 业务服务、需要反应式接口的批处理任务等。

> 同步阻塞场景请使用 **`eagle-restclient-starter`**。

模块提供：服务发现 LoadBalancer、声明式反应式 HTTP 接口、入站 Header 自动透传（从 Reactor Context
读取 `ServerWebExchange`）、统一错误转换。
类路径上如果还有 Seata，会额外透传 `TX_XID`（本仓库默认不装配）。

## 模块能力

| 组件                                            | 说明                                                                       |
|-----------------------------------------------|--------------------------------------------------------------------------|
| `PropagatingHeadersExchangeFilterFunction`    | 透传入站请求 Header / 压测标记（reactive 版，从 ServerWebExchange 取入站 header）          |
| `SeataXidExchangeFilterFunction`              | 透传 `TX_XID`，仅类路径存在 Seata 时注册（本仓库默认不引入） |
| `EagleWebClientErrorFilter`                   | 把下游 4xx/5xx 响应转换为项目异常体系（`ExchangeFilterFunction` 实现）                     |
| `EagleWebClientCustomizer`                    | 应用上述 filter 到 `WebClient.Builder`                                        |
| `EagleReactiveServiceClientFactory`           | 创建 Spring HTTP Service Interface 反应式代理（基于 `WebClientAdapter`）            |

## 依赖关系

```text
eagle-webclient-starter
├── eagle-common-starter              ← 提供 HttpClientProperties（与 restclient-starter 共享）
├── spring-webflux                    ← 提供 WebClient / ExchangeFilterFunction / ServerWebExchange
├── spring-cloud-starter-loadbalancer
├── reactor-netty-http                ← 默认 reactive HTTP connector
└── seata-spring-boot-starter         ← 可选 compileOnly；本仓库已移除 seata-starter，默认不装配
```

## 快速开始

```gradle
dependencies {
    implementation project(':eagle-starter:eagle-webclient-starter')
}
```

```java
@HttpExchange("/api/v1/inventory")
public interface ReactiveInventoryClient {

    @GetExchange("/{productId}/stock")
    Mono<StockResponse> getStock(@PathVariable Long productId);

    @PostExchange("/lock")
    Mono<Void> lockStock(@RequestBody LockStockRequest request);
}
```

```java
@Configuration(proxyBeanMethods = false)
class RemoteClientConfiguration {

    @Bean
    ReactiveInventoryClient inventoryClient(EagleReactiveServiceClientFactory factory) {
        return factory.createLoadBalancedClient(
                ReactiveInventoryClient.class, "eagle-inventory-service");
    }
}
```

## 配置项

配置 prefix `eagle.http-client.*` 与 `eagle-restclient-starter` 共享，同一服务两套客户端共用一份配置。
完整字段见 `eagle-restclient-starter/README.md`。

## 透传机制

- **入站 Header**：从 `ServerWebExchange`（由 Spring WebFlux 的 `ServerWebExchangeContextFilter` 自动注入到 Reactor Context）取入站请求头，按 `propagated-headers` 名单复制到下游 ClientRequest。非 web 上下文（独立 reactive 任务）跳过此步骤。
- **压测标记**：`PressureTestContext` 已通过 `eagle-common-starter` 的 `ContextPropagationConfig` 桥接到 Reactor Context，全场景可用。
- **Seata XID**：仅当类路径存在 Seata 时复用 `RootContext.getXID()`；本仓库默认没有这条链路。
- **多租户**：`eagle-tenant-starter` 已移除，不再透传 `X-Tenant-Id`。

## 错误处理

异常映射与 RestClient 路径完全一致：

| HTTP 状态              | 异常                  |
|----------------------|---------------------|
| 400                  | `DomainException`   |
| 404                  | `NotFoundException` |
| 409                  | `ConflictException` |
| 403 / 429 / 5xx / 其他 | `ServiceException`  |

异常通过 `Mono.error()` 传播，业务代码可用 `.onErrorResume(...)` 处理。

## 使用约束

- 反应式客户端接口放在调用方 `infrastructure/remote/` 包
- 客户端接口只描述 HTTP 契约，不加 `@Transactional`
- 不在客户端接口中做 fallback 默认实现；降级由应用服务按业务语义处理
- **Servlet 服务慎用** `WebClient`：反应式 API 在阻塞调用线程上 `.block()` 会增加复杂度，推荐用 `eagle-restclient-starter`
