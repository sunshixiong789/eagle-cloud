# eagle-tenant-starter — 多租户上下文 + 数据源路由 + 实体过滤

## 何时使用

- SaaS / B 端多租户产品
- 同一服务支撑多个客户、需要数据隔离
- 行级隔离（每张表 `tenant_id`）或 Schema 隔离（不同 DataSource）

## 何时不要使用

- 单租户产品（不要为"未来可能多租户"预先引入复杂度）
- 全局共享数据（用 `tenant_id = '__GLOBAL__'` 占位即可，不需要新方案）

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-tenant-starter')
```

```yaml
eagle.tenant:
  enabled: true
  header-name: X-Tenant-Id              # 透传请求头
  jwt-claim-name: tenant_id             # JWT 中读取的字段
  default-tenant-id: __GLOBAL__         # 缺失时兜底
  filter-enabled: true                  # 是否启用 @TenantFilter 实体过滤
  database-routing-enabled: false       # Schema 隔离（按需开启）
  datasources:                          # Schema 隔离时配置
    tenant_001:
      url: jdbc:mysql://...
```

## 核心 API

| 类 / 接口 | 用途 |
|---|---|
| `TenantContextHolder` | 租户 ID `ThreadLocal` 上下文（`set` / `get` / `clear`）|
| `TenantAware` | 实现该接口的实体启用 `tenantId` 自动注入 |
| `@TenantFilter` | 实体类注解：自动注入 `WHERE tenant_id = ?` |
| `TenantFilterAspect` | 实体过滤切面 |
| `TenantDatabaseRoutingAspect` | 数据源路由切面（Schema 隔离） |
| `TenantIdFilter` | HTTP Filter：从请求头 / JWT 写入 `TenantContextHolder` |

## 最小示例

```java
// 实体启用租户过滤
@Entity
@Table(name = "t_order", indexes = {
    @Index(name = "idx_tenant_status", columnList = "tenant_id, status")
})
@TenantFilter
public class Order extends BaseAggregateRoot<Order> implements TenantAware {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private String tenantId;

    @Override public String getTenantId() { return tenantId; }
    @Override public void setTenantId(String t) { this.tenantId = t; }
}

// 业务代码无需关心租户 — 自动从 ThreadLocal 注入
@Service
public class OrderService {
    public List<Order> findAll() {
        // SQL 自动加 WHERE tenant_id = '当前租户'
        return orderRepository.findAll();
    }
}

// 异步任务必须装饰 ThreadLocal（eagle-common-starter 的 TaskExecutor 已自动装饰）
@Async("eagleTaskExecutor")
public void processAsync() {
    String tenantId = TenantContextHolder.getCurrentTenantId();  // 自动透传
}

// 测试中显式设置上下文
@BeforeEach void setup() { TenantContextHolder.setCurrentTenantId("test"); }
@AfterEach  void clear() { TenantContextHolder.clear(); }
```

## 配置项

| key | 类型 | 默认 | 说明 |
|---|---|---|---|
| `eagle.tenant.enabled` | boolean | `true` | 总开关 |
| `eagle.tenant.header-name` | String | `X-Tenant-Id` | HTTP 请求头名 |
| `eagle.tenant.jwt-claim-name` | String | `tenant_id` | JWT claim 名 |
| `eagle.tenant.filter-enabled` | boolean | `true` | 启用 `@TenantFilter` |
| `eagle.tenant.database-routing-enabled` | boolean | `false` | Schema 隔离 |

## 常见错误

- ❌ 应用层手动 `WHERE tenant_id = ?` → ✅ 用 `@TenantFilter`，自动注入
- ❌ 异步任务不传上下文 → ✅ 使用 `eagleTaskExecutor`（已装饰）或手动 `TaskDecorator`
- ❌ `tenantId` 字段没索引 → ✅ 必须为索引前导列
- ❌ 测试不清理 ThreadLocal → ✅ `@AfterEach TenantContextHolder.clear()`
- ❌ 普通业务接口加 `@CrossTenant` → ✅ 仅超管接口允许

## 关联规则

- `.claude/rules/17-tenant-permission.md` — 租户隔离策略 / 跨租户操作 / 异步透传
- `.claude/rules/06-database.md` — 索引设计（`tenant_id` 前导）
