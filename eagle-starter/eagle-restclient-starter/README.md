# eagle-restclient-starter

基于 Spring `RestClient` 与 HTTP Service Interface (`@HttpExchange`) 的 **同步阻塞** 远程调用 starter，
替代 OpenFeign。适用于 servlet web 服务、批处理、命令行任务等所有 **非反应式** 调用场景。

> 反应式（WebFlux）服务请使用配套的 **`eagle-webclient-starter`**，避免在 reactive 链路里阻塞 event loop。

模块提供：服务发现 LoadBalancer、声明式 HTTP 接口、入站 Header 自动透传、
统一错误转换、超时配置和 Spring Boot 观测链路集成。
类路径上如果还有 Seata，会额外透传 `TX_XID`（`eagle-seata-starter` 已移除，本仓库默认不会装配）。

## 模块能力

| 组件                                               | 说明                                              |
|--------------------------------------------------|-------------------------------------------------|
| `PropagatingHeadersClientHttpRequestInterceptor` | 透传 `Authorization`、`Accept-Language`、请求 ID、压测标记（servlet 入站环境） |
| `SeataXidClientHttpRequestInterceptor`           | 透传 `TX_XID`，仅类路径存在 Seata 时注册（本仓库默认不引入） |
| `EagleResponseErrorHandler`                      | 将下游 HTTP 错误转换为项目异常体系                            |
| `EagleRestClientCustomizer`                      | 为所有自动配置的 `RestClient.Builder` 注入超时、拦截器、错误处理     |
| `EagleRestServiceClientFactory`                  | 创建 Spring HTTP Service Interface 同步代理           |

## 依赖关系

```text
eagle-restclient-starter
├── eagle-common-starter              ← 提供 HttpClientProperties（与 webclient-starter 共享）
├── spring-boot-restclient
├── spring-cloud-starter-loadbalancer
├── spring-web                        ← 不再拖 Tomcat / Spring MVC
├── jakarta.servlet-api               ← compileOnly，运行时由 servlet 服务自身提供
└── seata-spring-boot-starter         ← 可选 compileOnly；本仓库已移除 seata-starter，默认不装配
```

## 快速开始

```gradle
dependencies {
    implementation project(':eagle-starter:eagle-restclient-starter')
}
```

```java
@HttpExchange("/api/v1/inventory")
public interface InventoryClient {

    @GetExchange("/{productId}/stock")
    StockResponse getStock(@PathVariable Long productId);

    @PostExchange("/lock")
    void lockStock(@RequestBody LockStockRequest request);
}
```

```java
@Configuration(proxyBeanMethods = false)
class RemoteClientConfiguration {

    @Bean
    InventoryClient inventoryClient(EagleRestServiceClientFactory factory) {
        return factory.createLoadBalancedClient(InventoryClient.class, "eagle-inventory-service");
    }
}
```

## 配置项

配置 prefix `eagle.http-client.*` 与 `eagle-webclient-starter` 共享（同一服务两套客户端共用一份）。

```yaml
eagle:
  http-client:
    connect-timeout: 2s
    read-timeout: 5s
    error-handler-enabled: true
    buffer-content: true
    pressure-test-header-enabled: true
    propagated-headers:
      - Authorization
      - Accept-Language
      - X-Request-Id
      - X-Correlation-Id
```

| 配置项                                              | 类型             | 默认值      | 说明                    |
|--------------------------------------------------|----------------|----------|-----------------------|
| `eagle.http-client.connect-timeout`              | `Duration`     | `2s`     | TCP 连接建立超时            |
| `eagle.http-client.read-timeout`                 | `Duration`     | `5s`     | 响应读取超时                |
| `eagle.http-client.error-handler-enabled`        | `boolean`      | `true`   | 是否启用统一错误转换            |
| `eagle.http-client.buffer-content`               | `boolean`      | `true`   | 是否缓冲响应体，便于错误处理和日志重复读取 |
| `eagle.http-client.pressure-test-header-enabled` | `boolean`      | `true`   | 是否透传压测标记              |
| `eagle.http-client.propagated-headers`           | `List<String>` | 见上方 YAML | 从入站请求自动透传的 Header     |

## 错误处理

异常映射：

| HTTP 状态              | 异常                  |
|----------------------|---------------------|
| 400                  | `DomainException`   |
| 404                  | `NotFoundException` |
| 409                  | `ConflictException` |
| 403 / 429 / 5xx / 其他 | `ServiceException`  |

## 使用约束

- 远程客户端接口放在调用方 `infrastructure/remote/` 包
- 客户端接口只描述 HTTP 契约，不加 `@Transactional`
- 不在客户端接口中做 fallback 默认实现；降级由应用服务按业务语义处理
- **WebFlux 服务禁用** `RestClient`：会阻塞 event loop，请用 `eagle-webclient-starter`
- 事务内远程调用需明确一致性模型：不要在 `@Transactional` 里调远程；跨服务走本地事务 + AMQP 集成事件 + 消费方幂等
