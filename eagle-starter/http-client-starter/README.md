# http-client-starter

基于 Spring `RestClient` 与 HTTP Service Interface 的远程调用 starter，替代 OpenFeign。
模块提供服务发现、声明式 HTTP 接口、Header 自动透传、租户透传、Seata XID 透传、
统一错误转换、超时配置和 Spring Boot 观测链路集成。

## 模块能力

| 组件                                               | 说明                                              |
|--------------------------------------------------|-------------------------------------------------|
| `PropagatingHeadersClientHttpRequestInterceptor` | 透传 `Authorization`、`Accept-Language`、请求 ID、压测标记 |
| `TenantClientHttpRequestInterceptor`             | 透传 `X-Tenant-Id`，仅存在 `eagle-tenant-starter` 时注册 |
| `SeataXidClientHttpRequestInterceptor`           | 透传 `TX_XID`，仅存在 Seata 时注册                       |
| `EagleResponseErrorHandler`                      | 将下游 HTTP 错误转换为项目异常体系                            |
| `EagleRestClientCustomizer`                      | 为所有自动配置的 `RestClient.Builder` 注入超时、拦截器、错误处理     |
| `EagleHttpServiceClientFactory`                  | 创建 Spring HTTP Service Interface 代理             |

## 依赖关系

```text
http-client-starter
├── eagle-common-starter
├── spring-boot-restclient
├── spring-cloud-starter-loadbalancer
├── spring-boot-starter-webmvc
├── eagle-tenant-starter              ← 可选，compileOnly
└── seata-spring-boot-starter         ← 可选，compileOnly
```

## 快速开始

```gradle
dependencies {
    implementation project(':eagle-starter:http-client-starter')
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
    InventoryClient inventoryClient(EagleHttpServiceClientFactory factory) {
        return factory.createLoadBalancedClient(InventoryClient.class, "eagle-inventory-service");
    }
}
```

## 配置项

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

## 自动配置 Bean

| Bean                                             | 类型                                               | 说明                                    |
|--------------------------------------------------|--------------------------------------------------|---------------------------------------|
| `propagatingHeadersClientHttpRequestInterceptor` | `PropagatingHeadersClientHttpRequestInterceptor` | 基础 Header 透传                          |
| `eagleResponseErrorHandler`                      | `EagleResponseErrorHandler`                      | 统一错误转换                                |
| `eagleRestClientCustomizer`                      | `EagleRestClientCustomizer`                      | 全局 RestClient 定制                      |
| `eagleHttpServiceProxyFactory`                   | `HttpServiceProxyFactory`                        | 默认 HTTP Service 代理工厂                  |
| `eagleHttpServiceClientFactory`                  | `EagleHttpServiceClientFactory`                  | 业务侧创建声明式客户端的入口                        |
| `loadBalancedRestClientBuilder`                  | `RestClient.Builder`                             | 带 Spring Cloud LoadBalancer 的 Builder |

## 外部 HTTP API

调用三方 API 时直接复用自动配置后的 `RestClient.Builder`：

```java
@Bean
RestClient wechatRestClient(RestClient.Builder builder) {
    return builder.clone()
            .baseUrl("https://api.weixin.qq.com")
            .defaultHeader("Accept", "application/json")
            .build();
}
```

如果外部 API 不应接收当前用户 Token，可以使用原生 `RestClient.builder()` 创建独立客户端，
或声明自定义 `RestClient` Bean。

## 错误处理

`EagleResponseErrorHandler` 会读取下游响应体：

- 标准 `ErrorResult`：提取 `message`
- 非 JSON 响应：使用原始响应体，最长保留 200 字符
- 空响应体：回退为 `HTTP <status>`

异常映射：

| HTTP 状态              | 异常                  |
|----------------------|---------------------|
| 400                  | `DomainException`   |
| 404                  | `NotFoundException` |
| 409                  | `ConflictException` |
| 403 / 429 / 5xx / 其他 | `ServiceException`  |

## 业务场景覆盖

- 内部 RPC：`EagleHttpServiceClientFactory#createLoadBalancedClient`
- 固定 URL 调用：`createClient(type, "https://example.com")`
- 单次复杂请求：直接使用 `RestClient`
- 文件上传：`RestClient` + `MultipartBodyBuilder`
- 流式下载：`RestClient.exchange(...)`
- 分页：显式声明 `page`、`size`、`sort` 查询参数
- 异步任务：没有 Servlet 请求时自动跳过用户 Header 透传
- 多租户：引入 `eagle-tenant-starter` 后自动透传租户
- 分布式事务：引入 Seata 后自动透传 XID

## 使用约束

- 远程客户端接口放在调用方 `infrastructure/remote/` 包。
- 客户端接口只描述 HTTP 契约，不加 `@Transactional`。
- 不在客户端接口中做 fallback 默认实现；降级由应用服务按业务语义处理。
- 事务内远程调用需明确一致性模型：强一致用 Seata，最终一致用领域事件或 MQ。
