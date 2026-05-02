---
name: eagle-feign-client
description: Use when implementing service-to-service HTTP clients in eagle-cloud projects — Spring RestClient / HTTP Service Interface, automatic JWT/tenant-id/Seata-XID propagation, EagleResponseErrorHandler converting downstream HTTP errors to AppException hierarchy
---

# http-client-starter — RestClient / HTTP Service 客户端

## 何时使用

- 服务间同步 RPC（声明式 HTTP Service Interface）
- 调用外部 HTTP API（直接使用自动配置后的 `RestClient.Builder`）
- 需要透传当前用户 JWT、Accept-Language、压测标记、租户 ID、Seata XID
- 下游错误自动转 `AppException`

## 依赖与启用

```gradle
implementation project(':eagle-starter:http-client-starter')
```

无需 `@EnableFeignClients`。

```yaml
eagle:
  http-client:
    connect-timeout: 2s
    read-timeout: 5s
    propagated-headers:
      - Authorization
      - Accept-Language
      - X-Request-Id
      - X-Correlation-Id
```

## 最小示例

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

## 核心拦截器

| 拦截器 | 透传内容 | 触发条件 |
|--------|----------|----------|
| `PropagatingHeadersClientHttpRequestInterceptor` | `Authorization`、`Accept-Language`、请求 ID、`X-Eagle-Gray` | 始终启用 |
| `TenantClientHttpRequestInterceptor` | `X-Tenant-Id` | `eagle-tenant-starter` 在类路径时 |
| `SeataXidClientHttpRequestInterceptor` | `TX_XID` | Seata 在类路径时 |

## 错误转换

下游返回 `ErrorResult` JSON 时，自动提取 `message` 字段透传：

| 下游 HTTP | 抛出 |
|-----------|------|
| 404 | `NotFoundException` |
| 409 | `ConflictException` |
| 400 | `DomainException` |
| 403 / 429 / 5xx / 其他 | `ServiceException` |

## 常见错误

- HTTP Service Interface 放在 `application/` 包 → 必须放在 `infrastructure/remote/`
- 客户端接口上加 `@Transactional` → 远程调用不参与本地事务
- 分页直接传 `Pageable` → 显式声明 `page`、`size`、`sort`
- 文件上传 / 流式下载使用通用 RPC 接口 → 直接使用 `RestClient`
- 手动转发 Token / 租户 ID / XID → starter 自动透传

## 关联规则

- `.claude/rules/11-feign.md`（历史文件名，内容为 HTTP Client 规范）
- `.claude/rules/16-transaction-distributed.md` — XID 透传
- `.claude/rules/12-security.md` — JWT 透传
- `.claude/rules/17-tenant-permission.md` — 租户透传
