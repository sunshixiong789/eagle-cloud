# eagle-row-security-starter — 行级数据权限（基于 JPA Specification）

## 何时使用

- 中后台"本部门可见 / 本人可见 / 自定义部门组合 / 全部可见"
- 同一接口按角色返回不同数据范围

## 何时不要使用

- 简单的"管理员看全部，普通用户看自己" → 直接 `@PreAuthorize`
- 跨服务的数据范围（应在数据源服务做过滤）

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-row-security-starter')
```

```yaml
eagle.data-permission:
  enabled: true
  default-dept-field: deptId       # 实体默认部门字段
  default-user-field: id           # 实体默认用户字段（SELF 范围用）
```

业务方**必须**实现 `DataPermissionProvider` 提供"当前用户的部门 / 角色 / 范围"。

## 核心 API

| 类 / 接口 / 注解                             | 说明                                                                                                                                                    |
|-----------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| `@DataPermission(deptField, userField)` | 标在方法上，切面拦截**第一个 `Specification` 类型参数**追加过滤条件                                                                                                          |
| `@DataPermissionIgnore`                 | 显式跳过本次过滤                                                                                                                                              |
| `DataScope`（枚举）                         | `ALL / SELF / DEPT / DEPT_AND_CHILD / CUSTOM`                                                                                                         |
| `DataPermissionProvider`                | **业务方实现**：`getCurrentUserDataScope()` / `getCurrentUserDeptId()` / `getCurrentUserId()` / `getCurrentUserCustomDeptIds()` / `getChildDeptIds(deptId)` |
| `DataPermissionContext`                 | ThreadLocal 强制覆盖 scope（优先级高于 Provider）                                                                                                                |
| `DataPermissionAspect`                  | `@DataPermission` 切面                                                                                                                                  |
| `DataPermissionHelper`                  | 静态：`specification(provider, deptField, userField [, existingSpec])`                                                                                   |

## 工作流程

1. 业务方法的**第一个 `Specification` 参数**会被切面替换为：原条件 + 数据权限过滤
2. 范围由 `DataPermissionContext.getScope()`（ThreadLocal）→ `provider.getCurrentUserDataScope()` 决定
3. 字段名优先级：`@DataPermission(deptField=..., userField=...)` > 全局配置默认值

## 最小示例

```java
// 1) 业务方实现 Provider
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

    @Override
    public Long getCurrentUserId() {
        return SecurityUtils.getCurrentUserId();
    }

    @Override
    public Long getCurrentUserDeptId() {
        return SecurityUtils.getCurrentDeptId();
    }

    @Override
    public Set<Long> getCurrentUserCustomDeptIds() {
        return Set.of();
    }

    @Override
    public Set<Long> getChildDeptIds(Long deptId) {
        return deptRepository.findAllChildIds(deptId);
    }
}

// 2) Service / Repository 方法的第一个 Specification 参数会被切面包装
@Service
public class OrderQueryService {

    private final OrderRepository orderRepository;

    @DataPermission                                    // 用全局默认字段名（deptId / id）
    public Page<Order> findAll(Specification<Order> spec, Pageable pageable) {
        return orderRepository.findAll(spec, pageable);
    }

    @DataPermission(deptField = "deptId", userField = "creatorId")
    public Page<Order> search(Specification<Order> spec, Pageable pageable) {
        return orderRepository.findAll(spec, pageable);
    }

    @DataPermissionIgnore                              // 跳过过滤（仅特殊场景）
    @PreAuthorize("hasRole('super_admin')")
    public List<Order> exportAll() {
        return orderRepository.findAll();
    }
}

// 3) 编程式（不用注解）
public Page<Order> queryProgrammatically(Pageable pageable) {
    Specification<Order> spec = DataPermissionHelper.specification(
            provider, "deptId", "id"
    );
    return orderRepository.findAll(spec, pageable);
}

// 4) 强制覆盖范围（ThreadLocal）
DataPermissionContext.

setScope(DataScope.ALL);
try{
        return queryService.

findAll(...);
}finally{
        DataPermissionContext.

clear();
}
```

## 配置项

| key                                        | 类型      | 默认       | 说明        |
|--------------------------------------------|---------|----------|-----------|
| `eagle.data-permission.enabled`            | boolean | `true`   | 总开关       |
| `eagle.data-permission.default-dept-field` | String  | `deptId` | 实体默认部门字段名 |
| `eagle.data-permission.default-user-field` | String  | `id`     | 实体默认用户字段名 |

## DataScope 行为

| 范围               | SQL 注入                         | 边界处理                        |
|------------------|--------------------------------|-----------------------------|
| `ALL`            | 无过滤                            | —                           |
| `SELF`           | `userField = currentUserId`    | userId null → 无过滤（log warn） |
| `DEPT`           | `deptField = currentDeptId`    | deptId null → 退化 SELF       |
| `DEPT_AND_CHILD` | `deptField IN (childDeptIds)`  | 无子部门 → 退化 DEPT              |
| `CUSTOM`         | `deptField IN (customDeptIds)` | 空集合 → 退化 SELF               |

`DataPermissionProvider.getCurrentUserDataScope()` 返回 null 时**安全降级到 SELF**。

## 常见错误

- ❌ 枚举写 `DEPT_ONLY / SELF_ONLY` → ✅ 真名是 **`DEPT / SELF`**
- ❌ `@DataPermission(type = ..., deptColumn = ..., creatorColumn = ...)` → ✅ 真实只有 `deptField` 和 `userField`，**没有
  type**（type 由 Provider 决定）
- ❌ 方法签名不带 `Specification` 参数 → ✅ 切面只拦截第一个 `Specification` 参数；纯无 Spec 方法不生效
- ❌ 不实现 Provider → ✅ 必须实现（无业务部门信息）
- ❌ 用 SQL 拼接数据范围 → ✅ 让切面自动注入

## 关联规则

- `.claude/rules/17-tenant-permission.md`
- `.claude/rules/05-api.md` — `@PreAuthorize` 配合
