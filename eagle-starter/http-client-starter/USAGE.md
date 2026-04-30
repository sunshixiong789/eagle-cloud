# http-client-starter（eagle-feign-starter）— Feign 客户端（自动 Token / 租户 / XID 透传）

## 何时使用

- 服务间 RPC 调用（声明式 HTTP 客户端）
- 跨服务需要透传当前用户 / 租户 / 分布式事务 XID
- 统一错误码转换（下游 HTTP 错误自动转为本地异常）

## 何时不要使用

- 客户端 → 服务端通信（用普通 RestClient / 浏览器 fetch）
- 简单一次性 HTTP 调用（用 `RestClient`）
- 文件上传 / 下载等流式场景（Feign 不适合）

## 依赖与启用

```gradle
implementation project(':eagle-starter:http-client-starter')
```

主应用加 `@EnableFeignClients`：

```java
@EnableFeignClients(basePackages = "com.eagle")
@SpringBootApplication
public class MyApplication { }
```

```yaml
eagle.feign:
  enabled: true
  connect-timeout: 2s
  read-timeout: 5s
  log-level: BASIC                     # NONE / BASIC / HEADERS / FULL
```

## 核心 API

| 类 | 用途 |
|---|---|
| `FeignAuthInterceptor` | 自动透传 `Authorization` JWT Token + B3 链路追踪头 |
| `FeignTenantInterceptor` | 自动透传 `X-Tenant-Id` |
| `SeataXidRequestInterceptor` | 自动透传 Seata XID（条件加载，存在 Seata 依赖时生效） |
| `FeignErrorDecoder` | 下游 HTTP 错误 → 本地异常体系（404→NotFoundException 等） |
| `FeignProperties` | 配置项 |

## 最小示例

```java
// 1) 定义 FeignClient（位于 infrastructure/remote/）
@FeignClient(name = "eagle-inventory-server", path = "/api/v1/inventory")
public interface InventoryFeignClient {

    @GetMapping("/{productId}/stock")
    StockResponse getStock(@PathVariable Long productId);

    @PostMapping("/lock")
    void lockStock(@RequestBody LockStockRequest request);

    /** 分页：必须用 @SpringQueryMap 让 Pageable 展开为查询参数 */
    @GetMapping("/items")
    Page<ItemResponse> findItems(@SpringQueryMap Pageable pageable);
}

// 2) 注入使用
@Service
@RequiredArgsConstructor
public class OrderApplicationService {
    private final InventoryFeignClient inventoryClient;

    public void createOrder(CreateOrderRequest req) {
        // Token / 租户 / XID 自动透传，无需手动设置
        StockResponse stock = inventoryClient.getStock(req.getProductId());

        // 异常已被 FeignErrorDecoder 转换：
        // 404 → NotFoundException, 409 → DomainException, 500 → ServiceException
        // 无需 try-catch
    }
}
```

## 错误转换映射

| 下游 HTTP | 抛出 | 触发场景 |
|-----------|------|---------|
| 404 | `NotFoundException` | 资源不存在 |
| 400 / 409 | `DomainException` | 业务规则冲突 |
| 401 / 403 | `ServiceException`（认证失败）| Token 失效或越权 |
| 429 | `ServiceException` | 限流 |
| 5xx | `ServiceException` | 下游异常 |

调用方**无需 try-catch**，全局异常处理器统一返回。

## 配置项

| key | 类型 | 默认 | 说明 |
|---|---|---|---|
| `eagle.feign.enabled` | boolean | `true` | 总开关 |
| `eagle.feign.connect-timeout` | Duration | `2s` | 连接超时 |
| `eagle.feign.read-timeout` | Duration | `5s` | 读超时 |
| `eagle.feign.log-level` | enum | `BASIC` | Feign 日志级别 |
| `eagle.feign.retry.max-attempts` | int | `0` | 失败重试次数 |
| `eagle.feign.tenant.enabled` | boolean | `true` | 启用租户透传 |

## 常见错误

- ❌ FeignClient 定义在 `application/` 包 → ✅ 必须在 `infrastructure/remote/`
- ❌ 加 fallback 默认实现 → ✅ 失败应上抛，由调用方决定降级
- ❌ `Pageable` 不加 `@SpringQueryMap` → ✅ 必须加（不加分页参数静默丢失）
- ❌ FeignClient 上加 `@Transactional` → ✅ 远程调用不应参与本地事务
- ❌ 在 `@Transactional` 内同步远程调用 → ✅ 用领域事件 AFTER_COMMIT 异步触发
- ❌ 手动 `request.getHeader("Authorization")` 转发 → ✅ 自动透传，不要手写

## 关联规则

- `.claude/rules/11-feign.md` — FeignClient 完整规范
- `.claude/rules/16-transaction-distributed.md` — Seata XID 透传
- `.claude/rules/12-security.md` — JWT 透传
- `.claude/rules/17-tenant-permission.md` — 租户 ID 透传
