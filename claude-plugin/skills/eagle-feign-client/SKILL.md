---
name: eagle-feign-client
description: Use when implementing service-to-service Feign clients in eagle-cloud projects — automatic JWT/tenant-id/Seata-XID propagation via FeignAuthInterceptor/FeignTenantInterceptor/SeataXidRequestInterceptor, FeignErrorDecoder converting downstream HTTP errors to AppException hierarchy, @SpringQueryMap for Pageable
---

# http-client-starter — Feign 客户端（自动透传 Token / 租户 / XID + 错误码转换）

## 何时使用

- 服务间 RPC（Feign 声明式 HTTP）
- 需要透传当前用户 JWT、Accept-Language、压测标记、租户 ID、Seata XID
- 下游错误自动转 `AppException`

## 何时不要使用

- 客户端 → 服务端通信（用普通 RestClient）
- 文件上传 / 流式下载（Feign 不适合）

## 依赖与启用

```gradle
implementation project(':eagle-starter:http-client-starter')
```

```yaml
eagle.feign:
  log-level: BASIC                # NONE / BASIC / HEADERS / FULL
  connect-timeout: 2000            # ms
  read-timeout: 5000                # ms
```

主应用加 `@EnableFeignClients`：

```java
@EnableFeignClients(basePackages = "com.eagle")
@SpringBootApplication
public class MyApplication { }
```

## 核心拦截器（自动注册）

| 拦截器 | 透传内容 | 触发条件 |
|--------|---------|---------|
| `FeignAuthInterceptor` | `Authorization` JWT、`Accept-Language`、`X-Eagle-Gray`（压测） | 始终启用，HTTP 上下文存在时 |
| `FeignTenantInterceptor` | `X-Tenant-Id`（来自 `TenantContextHolder`） | `eagle-tenant-starter` 在类路径时 |
| `SeataXidRequestInterceptor` | `TX_XID`（来自 `RootContext.getXID()`） | `seata-spring-boot-starter` 在类路径时 |

B3 链路追踪头由 Spring Cloud OpenFeign + Micrometer Tracing **自动**注入，无需手动处理。

## 错误转换（FeignErrorDecoder 自动注册）

下游返回 `ErrorResult` JSON 时，自动提取 `message` 字段透传：

| 下游 HTTP | 抛出 | 错误码 |
|-----------|------|--------|
| 404 | `NotFoundException` | `ExternalErrorCode.EXTERNAL_SERVICE_DETAIL` |
| 409 | `ConflictException` | 同上 |
| 400 | `DomainException` | 同上 |
| 403 / 429 / 5xx / 其他 | `ServiceException` | 同上 |

调用方**无需 try-catch**，全局异常处理器统一返回。

## 最小示例

```java
// FeignClient 定义在 infrastructure/remote/
@FeignClient(name = "eagle-inventory-server", path = "/api/v1/inventory")
public interface InventoryFeignClient {

    @GetMapping("/{productId}/stock")
    StockResponse getStock(@PathVariable Long productId);

    @PostMapping("/lock")
    void lockStock(@RequestBody LockStockRequest request);

    /** 分页：必须用 @SpringQueryMap */
    @GetMapping("/items")
    Page<ItemResponse> findItems(@SpringQueryMap Pageable pageable);
}

// 业务调用
@Service
@RequiredArgsConstructor
public class OrderApplicationService {

    private final InventoryFeignClient inventoryClient;

    public void create(CreateOrderRequest req) {
        // Token / 租户 / XID 自动透传
        StockResponse stock = inventoryClient.getStock(req.getProductId());
        // 异常已转换为 AppException 体系，无需 try-catch
    }
}
```

## 配置项

| key | 类型 | 默认 | 说明 |
|---|---|---|---|
| `eagle.feign.log-level` | enum | `BASIC` | NONE / BASIC / HEADERS / FULL |
| `eagle.feign.connect-timeout` | int | `2000` | 连接超时（**ms**，整数） |
| `eagle.feign.read-timeout` | int | `5000` | 读超时（**ms**，整数） |

⚠️ **starter 仅 3 个配置项**，没有 `enabled` / `retry` / `tenant.enabled` 等。

## 常见错误

- ❌ FeignClient 在 `application/` 包 → ✅ 必须在 `infrastructure/remote/`
- ❌ 加 fallback 默认实现 → ✅ 失败应上抛，由调用方决定降级
- ❌ `Pageable` 不加 `@SpringQueryMap` → ✅ 必须加（不加会静默丢失分页参数）
- ❌ FeignClient 上加 `@Transactional` → ✅ 远程调用不参与本地事务
- ❌ 在 `@Transactional` 内同步远程调用 → ✅ 用领域事件 AFTER_COMMIT
- ❌ 手动转发 Token / 租户 ID → ✅ 自动透传
- ❌ 配置写 `connect-timeout: 2s` Duration → ✅ 真实类型是 **`int` 毫秒**

## 关联规则

- `.claude/rules/11-feign.md`
- `.claude/rules/16-transaction-distributed.md` — XID 透传
- `.claude/rules/12-security.md` — JWT 透传
- `.claude/rules/17-tenant-permission.md` — 租户透传
