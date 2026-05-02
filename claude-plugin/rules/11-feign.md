# HTTP Client 远程调用规范

> 历史文件名保留为 `11-feign.md`，当前规范已迁移到 Spring RestClient / HTTP Service Interface。

## 客户端定义位置

HTTP Service Interface 定义在调用方服务的 `infrastructure/remote/` 包中：

```java
// infrastructure/remote/InventoryClient.java
@HttpExchange("/api/v1/inventory")
public interface InventoryClient {

    @GetExchange("/{productId}/stock")
    StockResponse getStock(@PathVariable Long productId);
}
```

客户端 Bean 在基础设施配置中创建：

```java
@Configuration(proxyBeanMethods = false)
class RemoteClientConfiguration {

    @Bean
    InventoryClient inventoryClient(EagleHttpServiceClientFactory factory) {
        return factory.createLoadBalancedClient(InventoryClient.class, "eagle-inventory-service");
    }
}
```

## 错误处理

`http-client-starter` 内置 `EagleResponseErrorHandler`，自动将下游 HTTP 错误转换为项目异常体系：

| 下游状态码 | 转换结果 |
|------------|----------|
| 400 | `DomainException` |
| 404 | `NotFoundException` |
| 409 | `ConflictException` |
| 403 / 429 / 5xx / 其他 | `ServiceException` |

调用方无需手动 try-catch HTTP 客户端异常，全局异常处理器统一处理。

## 透传机制

`http-client-starter` 自动为 `RestClient.Builder` 注册以下拦截器：

- `PropagatingHeadersClientHttpRequestInterceptor`：透传 `Authorization`、`Accept-Language`、请求 ID、压测标记。
- `TenantClientHttpRequestInterceptor`：透传 `X-Tenant-Id`，仅在 `eagle-tenant-starter` 存在时生效。
- `SeataXidClientHttpRequestInterceptor`：透传 `TX_XID`，仅在 Seata 存在时生效。

## 分页与复杂参数

HTTP Service Interface 中分页查询优先显式声明查询参数：

```java
@GetExchange("/items")
Page<ItemResponse> findItems(@RequestParam int page,
                             @RequestParam int size,
                             @RequestParam String sort);
```

复杂查询条件使用专用 Request DTO，并通过 `@RequestParam` 或 `@RequestBody` 明确绑定方式。

## 禁止

- 禁止在客户端接口上加 `@Transactional`。
- 禁止在客户端接口中定义 fallback 默认实现。
- 禁止在业务代码里手动透传 Token / 租户 ID / XID。
- 禁止把文件上传、流式下载塞进通用 RPC 接口；这些场景直接使用 `RestClient`。
- 禁止在事务中随意同步远程调用；必须明确强一致、最终一致或补偿策略。
