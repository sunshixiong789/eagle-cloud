# 多租户与数据权限规范

技术栈：
- `eagle-tenant-starter` — 多租户：`TenantContextHolder` + `TenantIdFilter`（HTTP 过滤器）+ `TenantDatabaseRoutingAspect`（动态数据源路由）+ `@TenantFilter`（实体过滤）
- `eagle-row-security-starter` — 行级数据权限：基于 AspectJ + SQL 拦截
- `eagle-dynamic-datasource-starter` — 多数据源动态路由

## 多租户隔离策略

| 策略 | 适用 | 实现 |
|------|------|------|
| **Schema 隔离**（推荐）| 中等租户数（< 100）| `TenantDatabaseRoutingAspect` 路由不同 DataSource |
| **行级隔离**（默认）| 大量租户、SaaS | 每张表 `tenant_id` 字段 + `@TenantFilter` |
| **DB 隔离** | 极少数大客户 | `eagle-dynamic-datasource-starter` |

**禁止**应用层手动拼接 `WHERE tenant_id = ?`——必须通过 starter 自动注入。

## 租户 ID 透传链路

```
浏览器 ─ X-Tenant-Id ─→ Gateway ─ JWT claim ─→ ResourceServer ─→ TenantIdFilter
                                                                    ↓
                                                          TenantContextHolder.set(tenantId)
                                                                    ↓
                                                  ┌──────────────┴──────────────┐
                                            JPA @TenantFilter           Feign 透传 X-Tenant-Id
```

- 网关将 JWT 中的 `tenant_id` claim 写入 `X-Tenant-Id` 请求头
- `TenantIdFilter` 在请求入口写入 `TenantContextHolder`
- 请求结束 / 异步任务结束必须 `TenantContextHolder.clear()` 防止线程池泄漏

## 实体定义

所有租户内业务实体必须包含 `tenantId` 字段：

```java
@Entity
@Table(name = "t_order", indexes = {
    @Index(name = "idx_tenant_status", columnList = "tenant_id, status")
})
@TenantFilter   // 自动注入 WHERE tenant_id = :currentTenant
public class Order extends BaseAggregateRoot<Order> {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private String tenantId;

    @PrePersist
    void fillTenant() {
        if (tenantId == null) {
            tenantId = TenantContextHolder.getCurrentTenantId();
        }
    }
}
```

- `tenantId` 字段 `nullable = false`、`updatable = false`（创建后不可改）
- 所有索引以 `tenant_id` 为前导列（最左前缀加速租户查询）
- 跨租户共享数据（系统字典、全局配置）使用 `tenant_id = '__GLOBAL__'` 占位

## 跨租户操作

仅超级管理员（`ROLE_SUPER_ADMIN`）允许跨租户：

```java
// ✅ 显式声明跨租户（绕过 @TenantFilter）
@PreAuthorize("hasRole('super_admin')")
@CrossTenant
public List<Order> findAllAcrossTenants() { ... }
```

- 跨租户操作必须**审计日志**（含操作员、原 tenantId、目标 tenantId、reason）
- **禁止**普通业务接口出现 `@CrossTenant`

## 数据源路由（Schema 隔离场景）

```java
// ✅ 通过注解切换租户数据源
@Tenant("dynamic")
@Service
public class TenantOrderService { ... }

// ✅ 编程式切换（特殊场景）
TenantContextHolder.setCurrentTenantId("tenant_001");
try {
    return orderRepository.findAll();
} finally {
    TenantContextHolder.clear();
}
```

`@Tenant("dynamic")` 让 `TenantDatabaseRoutingAspect` 根据当前租户上下文选择 DataSource，名称必须在 `eagle.tenant.datasources` 配置中存在。

## 行级数据权限

**适用场景**：本部门可见 / 本人可见 / 自定义部门组合 / 全部可见。

```java
// ✅ 注解声明数据范围
@DataPermission(
    type = DataPermissionType.DEPT_AND_CHILD,    // 本部门及子部门
    deptColumn = "dept_id",
    creatorColumn = "creator_id"
)
@GetMapping
public Page<OrderResponse> list(@SpringQueryMap Pageable pageable) {
    return orderQueryService.findAll(pageable);
}
```

数据范围枚举：

| 类型 | 说明 |
|------|------|
| `ALL` | 全部数据（仅超管） |
| `DEPT_ONLY` | 仅本部门 |
| `DEPT_AND_CHILD` | 本部门及子部门 |
| `SELF_ONLY` | 仅本人 |
| `CUSTOM` | 自定义部门组合（从 `t_role_dept` 查） |

**禁止**：
- 在 Service 内部手动 `if (currentUser.role == ADMIN)` 判断数据范围
- 数据权限 SQL 拼接（必须走 starter 拦截器）

## 多租户 + 数据权限组合

两者**叠加**生效（先租户过滤，再数据范围过滤）：

```sql
SELECT * FROM t_order
WHERE tenant_id = ?              -- @TenantFilter 注入
  AND dept_id IN (?, ?, ?)        -- @DataPermission 注入
  AND deleted = false             -- 软删除条件
```

## 单元测试

```java
// ✅ 测试中显式设置租户上下文
@BeforeEach
void setUp() {
    TenantContextHolder.setCurrentTenantId("test_tenant");
}

@AfterEach
void tearDown() {
    TenantContextHolder.clear();
}
```

**禁止**测试不清理 ThreadLocal，否则跨用例污染。

## 异步任务的租户上下文

```java
// ✅ TaskExecutor 必须装饰传递 ThreadLocal
@Bean
public TaskDecorator tenantAwareDecorator() {
    return runnable -> {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        return () -> {
            TenantContextHolder.setCurrentTenantId(tenantId);
            try { runnable.run(); }
            finally { TenantContextHolder.clear(); }
        };
    };
}
```

`eagle-common-starter` 已默认装配此装饰器，业务代码无需手动处理。

## 禁止清单

- 禁止在 SQL 中硬编码 `tenant_id` 值
- 禁止跨租户引用聚合根 ID（每个租户 ID 空间独立）
- 禁止数据迁移脚本不带 `tenant_id` 条件
- 禁止 `tenantId` 缺索引（任何 tenant 范围查询都会全表扫描）
- 禁止使用 `@CrossTenant` 满足业务需求（业务不应跨租户，请求设计层面解决）
