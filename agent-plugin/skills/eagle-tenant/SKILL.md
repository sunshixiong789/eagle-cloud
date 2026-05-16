---
name: eagle-tenant
description: Use when implementing multi-tenancy in eagle-cloud projects — TenantContextHolder (getTenantId/setTenantId/clear, NOT getCurrentTenantId), TenantAware interface, @TenantFilter on Service/Repository (NOT entity), Hibernate @FilterDef/@Filter on entities, COLUMN vs DATABASE mode
---

# eagle-tenant-starter — 多租户上下文 + Hibernate Filter 自动过滤

## 何时使用

- SaaS / B 端多租户产品
- 行级隔离（每张表 `tenant_id` 字段，COLUMN 模式）
- DB 隔离（不同租户独立数据库，DATABASE 模式）

## 何时不要使用

- 单租户产品（不要预先引入复杂度）

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-tenant-starter')
```

```yaml
eagle.tenant:
  enabled: true                    # 默认 false，必须显式开启
  mode: COLUMN                     # COLUMN（共享库分字段）/ DATABASE（独立数据库）
  header-name: X-Tenant-Id
  default-tenant-id: "0"
```

`TenantIdFilter` 自动注册为 Servlet Filter——从请求头解析租户 ID 写入 `TenantContextHolder`，请求结束自动 `clear()`。

## 核心 API

| 类 / 注解                        | 说明                                                                                             |
|-------------------------------|------------------------------------------------------------------------------------------------|
| `TenantContextHolder`         | 静态：`setTenantId(String)` / `getTenantId()` / `clear()`                                         |
| `TenantAware`                 | 实体接口：`getTenantId() / setTenantId(String)`                                                     |
| `@TenantFilter`               | 标在 **Service / Repository 方法或类**上（不是实体），触发 Hibernate Filter 自动注入 `WHERE tenant_id = :tenantId` |
| `TenantIdFilter`              | HTTP Filter，自动注册                                                                               |
| `TenantFilterAspect`          | `@TenantFilter` 切面                                                                             |
| `TenantDatabaseRoutingAspect` | DATABASE 模式数据源路由切面                                                                             |

## 实体配合规范（COLUMN 模式必备）

实体本身需要：

1. 实现 `TenantAware`
2. 加 `@FilterDef` + `@Filter` Hibernate 注解
3. 自动填充 `tenantId`（`@PrePersist`）

```java

@Entity
@Getter
@Setter
@FilterDef(name = "tenantFilter",
        parameters = @ParamDef(name = "tenantId", type = String.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Table(indexes = @Index(name = "idx_tenant_status", columnList = "tenant_id, status"))
public class Order extends BaseAggregateRoot<Order> implements TenantAware {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private String tenantId;

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(String t) {
        this.tenantId = t;
    }

    @PrePersist
    void fillTenant() {
        if (tenantId == null) tenantId = TenantContextHolder.getTenantId();
    }
}
```

## 业务代码使用

```java
// ✅ 在需要租户过滤的方法/类上加 @TenantFilter
@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderRepository orderRepository;

    @TenantFilter
    @Transactional(readOnly = true)
    public List<Order> findAll() {
        return orderRepository.findAll();   // SQL 自动加 WHERE tenant_id = ?
    }
}

// ✅ 编程式（特殊场景：跨租户后台任务）
TenantContextHolder.

setTenantId("tenant_001");
try{
List<Order> orders = orderService.findAllForTenant();
}finally{
        TenantContextHolder.

clear();
}

// ✅ 测试：显式设置 + 清理
@BeforeEach
void setup() {
    TenantContextHolder.setTenantId("test");
}

@AfterEach
void clear() {
    TenantContextHolder.clear();
}
```

## 配置项

| key                              | 类型      | 默认            | 说明                    |
|----------------------------------|---------|---------------|-----------------------|
| `eagle.tenant.enabled`           | boolean | **`false`**   | 总开关（默认关）              |
| `eagle.tenant.mode`              | enum    | `COLUMN`      | `COLUMN` / `DATABASE` |
| `eagle.tenant.header-name`       | String  | `X-Tenant-Id` | 解析 HTTP 头名            |
| `eagle.tenant.default-tenant-id` | String  | `"0"`         | 缺失时兜底                 |

## 常见错误

- ❌ `getCurrentTenantId() / setCurrentTenantId(...)` → ✅ 真名是 **`getTenantId() / setTenantId(...)`**
- ❌ `@TenantFilter` 标在 `@Entity` 上 → ✅ 标在 **Service / Repository** 方法或类上
- ❌ 实体没有 `@FilterDef`/`@Filter` 注解 → ✅ 必须自行声明（starter 不会替你加）
- ❌ 异步任务不传上下文 → ✅ 用 starter 注册的 `TaskDecorator`，或手动 `set/clear`
- ❌ 测试不清理 ThreadLocal → ✅ `@AfterEach` 必须 `clear()`
- ❌ 写代码默认认为 `enabled=true` → ✅ 默认是 **`false`**，必须显式开启

## 关联规则

- `.claude/rules/17-tenant-permission.md`
- `.claude/rules/06-database.md` — `tenant_id` 索引前导
