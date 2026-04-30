# 多租户与数据权限规范

技术栈：
- `eagle-tenant-starter` — 多租户：`TenantContextHolder` + `TenantIdFilter`（HTTP 过滤器）+ `@TenantFilter`（Service/Repository 切面）+ `TenantDatabaseRoutingAspect`（DATABASE 模式数据源路由）
- `eagle-row-security-starter` — 行级数据权限：基于 `DataPermissionAspect` + JPA `Specification` 注入
- `eagle-dynamic-datasource-starter` — 主从 / 多数据源动态路由（与 tenant-starter DATABASE 模式正交）

## 多租户隔离策略

| 策略 | 配置 | 实现机制 |
|------|------|---------|
| **行级隔离**（默认推荐）| `eagle.tenant.mode=COLUMN` | 每张表 `tenant_id` 字段 + Hibernate `@FilterDef` / `@Filter` + `@TenantFilter` 切面激活 |
| **DB 隔离** | `eagle.tenant.mode=DATABASE` | `TenantDatabaseRoutingAspect` 路由不同 DataSource |

**禁止**应用层手动拼接 `WHERE tenant_id = ?`——必须通过 starter 自动注入。

## 租户 ID 透传链路

```
浏览器 ─ X-Tenant-Id ─→ Gateway ─ JWT claim ─→ ResourceServer ─→ TenantIdFilter
                                                                    ↓
                                                          TenantContextHolder.setTenantId(...)
                                                                    ↓
                                                  ┌──────────────┴──────────────┐
                                            JPA Hibernate Filter         Feign 透传 X-Tenant-Id
```

- 网关将 JWT 中的 `tenant_id` claim 写入 `X-Tenant-Id` 请求头
- `TenantIdFilter` 在请求入口写入 `TenantContextHolder`，请求结束 `clear()`
- 异步任务通过 `TaskDecorator` 透传（`eagle-common-starter` 默认装配）

## 实体定义（COLUMN 模式）

实体必须：
1. 实现 `TenantAware` 接口
2. 自带 Hibernate `@FilterDef` + `@Filter` 注解（**starter 不会替你加**）
3. `@PrePersist` 自动填充 `tenantId`

```java
@Entity
@Getter @Setter
@Table(name = "t_order", indexes = {
    @Index(name = "idx_tenant_status", columnList = "tenant_id, status")
})
@FilterDef(name = "tenantFilter",
           parameters = @ParamDef(name = "tenantId", type = String.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Order extends BaseAggregateRoot<Order> implements TenantAware {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private String tenantId;

    @Override public String getTenantId() { return tenantId; }
    @Override public void setTenantId(String t) { this.tenantId = t; }

    @PrePersist
    void fillTenant() {
        if (tenantId == null) {
            tenantId = TenantContextHolder.getTenantId();
        }
    }
}
```

- `tenantId` 字段 `nullable = false`、`updatable = false`（创建后不可改）
- 所有索引以 `tenant_id` 为前导列（最左前缀加速租户查询）
- 跨租户共享数据（系统字典、全局配置）使用配置的 `default-tenant-id`（默认 `"0"`）占位

## 启用过滤（@TenantFilter 标在 Service / Repository 上）

```java
// ✅ 标在 Service 类或方法上 — 切面激活 Hibernate Filter
@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderRepository orderRepository;

    @TenantFilter
    @Transactional(readOnly = true)
    public List<Order> findAll() {
        // SQL 自动追加：WHERE tenant_id = :currentTenant
        return orderRepository.findAll();
    }
}

// ❌ 错误：@TenantFilter 不要标在 @Entity 上（无效）
```

## 跨租户操作

`eagle-tenant-starter` **不提供** `@CrossTenant` 注解。跨租户操作通过编程式切换上下文实现：

```java
// ✅ 仅超级管理员场景：编程式切换上下文
@PreAuthorize("hasRole('super_admin')")
@Transactional(readOnly = true)
public List<Order> findAllAcrossTenants() {
    String original = TenantContextHolder.getTenantId();
    try {
        // 不调用 @TenantFilter 的方法 → 不会注入 WHERE 条件 → 跨租户可见
        return adminOrderRepository.findAllNative();
    } finally {
        if (original != null) {
            TenantContextHolder.setTenantId(original);
        }
    }
}
```

或在租户超管平台用编程式切换：

```java
// ✅ 切换到目标租户执行操作
TenantContextHolder.setTenantId("tenant_001");
try {
    return orderRepository.findAll();
} finally {
    TenantContextHolder.clear();
}
```

- 跨租户操作必须**审计日志**（含操作员、原 tenantId、目标 tenantId、reason）
- **禁止**普通业务接口跨租户

## 数据源路由（DATABASE 模式）

`eagle.tenant.mode=DATABASE` 时，`TenantDatabaseRoutingAspect` 自动根据 `TenantContextHolder.getTenantId()` 选择 DataSource——业务代码无需任何注解，与 COLUMN 模式相同的 Service 调用方式。

具体多个数据源的注册由项目侧基础设施完成（参见 `eagle-dynamic-datasource-starter` 或自定义 `DataSource` Bean）。

## 行级数据权限

适用场景：本部门可见 / 本人可见 / 自定义部门组合 / 全部可见。

### 工作机制

`@DataPermission` 切面拦截方法的**第一个 `Specification` 参数**，根据当前用户范围追加过滤条件。范围由：

1. `DataPermissionContext.getScope()`（ThreadLocal 强制覆盖）
2. `DataPermissionProvider.getCurrentUserDataScope()`（业务方实现）

依次决定。

### 注解使用

```java
@Service
public class OrderQueryService {

    private final OrderRepository orderRepository;

    /** 用全局默认字段名（eagle.data-permission.default-dept-field / default-user-field）*/
    @DataPermission
    public Page<Order> findAll(Specification<Order> spec, Pageable pageable) {
        return orderRepository.findAll(spec, pageable);
    }

    /** 显式指定字段名（覆盖全局默认）*/
    @DataPermission(deptField = "departmentId", userField = "creatorId")
    public Page<Order> search(Specification<Order> spec, Pageable pageable) {
        return orderRepository.findAll(spec, pageable);
    }

    /** 显式跳过过滤（仅特殊场景）*/
    @DataPermissionIgnore
    @PreAuthorize("hasRole('super_admin')")
    public List<Order> exportAll() {
        return orderRepository.findAll();
    }
}
```

### DataScope 范围枚举（5 种）

| 类型 | 行为 | 边界处理 |
|------|------|---------|
| `ALL` | 无过滤（全部数据） | — |
| `SELF` | `userField = currentUserId` | userId null → 无过滤（log warn） |
| `DEPT` | `deptField = currentDeptId` | deptId null → 退化 SELF |
| `DEPT_AND_CHILD` | `deptField IN (childDeptIds)` | 无子部门 → 退化 DEPT |
| `CUSTOM` | `deptField IN (customDeptIds)` | 空集合 → 退化 SELF |

### 业务方必须实现 DataPermissionProvider

```java
@Component
@RequiredArgsConstructor
public class MyDataPermissionProvider implements DataPermissionProvider {
    private final DeptRepository deptRepository;

    @Override
    public DataScope getCurrentUserDataScope() {
        if (SecurityUtils.hasRole("admin")) return DataScope.ALL;
        if (SecurityUtils.hasRole("manager")) return DataScope.DEPT_AND_CHILD;
        return DataScope.SELF;
    }

    @Override public Long getCurrentUserId() { return SecurityUtils.getCurrentUserId(); }
    @Override public Long getCurrentUserDeptId() { return SecurityUtils.getCurrentDeptId(); }
    @Override public Set<Long> getCurrentUserCustomDeptIds() { return Set.of(); }
    @Override public Set<Long> getChildDeptIds(Long deptId) {
        return deptRepository.findAllChildIds(deptId);
    }
}
```

**禁止**：
- 在 Service 内部手动 `if (currentUser.role == ADMIN) ... else ...` 判断数据范围
- SQL 拼接数据权限条件（必须走切面注入）
- 方法签名缺少 `Specification` 参数（切面无法拦截）

## 多租户 + 数据权限组合

两者**叠加**生效（先租户过滤，再数据范围过滤）：

```sql
SELECT * FROM t_order
WHERE tenant_id = ?              -- Hibernate Filter 注入（@TenantFilter）
  AND dept_id IN (?, ?, ?)        -- DataPermissionAspect 注入（@DataPermission）
  AND deleted = false             -- 软删除条件
```

## 单元测试

```java
@BeforeEach
void setUp() {
    TenantContextHolder.setTenantId("test_tenant");
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
        String tenantId = TenantContextHolder.getTenantId();
        return () -> {
            TenantContextHolder.setTenantId(tenantId);
            try { runnable.run(); }
            finally { TenantContextHolder.clear(); }
        };
    };
}
```

`eagle-common-starter` 已默认装配此装饰器，使用 `taskExecutor` 池的 `@Async` 方法自动透传，业务代码无需手动处理。

## 禁止清单

- 禁止在 SQL 中硬编码 `tenant_id` 值
- 禁止跨租户引用聚合根 ID（每个租户 ID 空间独立）
- 禁止数据迁移脚本不带 `tenant_id` 条件
- 禁止 `tenantId` 缺索引（任何 tenant 范围查询都会全表扫描）
- 禁止 `@TenantFilter` 标在 `@Entity` 上（应标在 Service/Repository）
- 禁止业务接口跨租户访问（应在请求设计层解决；超管平台用编程式切换 + 审计）
