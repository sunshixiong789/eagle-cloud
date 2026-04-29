# Feign 远程调用规范

## FeignClient 定义位置

`@FeignClient` 接口定义在调用方服务的 `infrastructure/remote/` 包中：

```java
// infrastructure/remote/InventoryFeignClient.java
@FeignClient(name = "eagle-inventory-server", path = "/api/inventory")
public interface InventoryFeignClient {
    @GetMapping("/{productId}/stock")
    StockResponse getStock(@PathVariable Long productId);
}
```

## 错误处理

`eagle-feign-starter` 已内置 `FeignErrorDecoder`，自动将下游 HTTP 错误转换为项目异常体系：

| 下游状态码 | 转换结果 |
|-----------|---------|
| 404 | `NotFoundException`（`ExternalErrorCode.EXTERNAL_SERVICE_ERROR`）|
| 400 / 409 | `DomainException` |
| 403 / 429 | `ServiceException` |
| 其他 | `ServiceException` |

调用方**无需手动 try-catch** Feign 异常，全局异常处理器统一处理。

## 透传机制（自动，无需手动配置）

`eagle-feign-starter` 自动注册以下拦截器：

- **FeignAuthInterceptor** — 透传当前请求的 `Authorization` JWT Token + B3 链路追踪头
- **SeataXidRequestInterceptor**（可选）— 透传 Seata 分布式事务 XID（`TX_XID` 头），仅在 Seata 依赖存在时生效

## 禁止

- 禁止在 FeignClient 中定义 fallback 默认实现（失败应上抛，由调用方决定降级策略）
- 禁止手动拼装 HTTP 请求（`RestTemplate` / `WebClient`），统一使用 FeignClient
- 禁止在 FeignClient 接口上加 `@Transactional`（远程调用不应参与本地事务）
