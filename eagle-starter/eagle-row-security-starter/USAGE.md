# eagle-row-security-starter — 行级数据权限（部门 / 本人 / 自定义范围）

## 何时使用

- 中后台管理系统的"本部门可见 / 本人可见 / 自定义部门组合"
- 同一接口返回结果需要按角色过滤数据范围
- 与多租户配合（先租户过滤，再数据范围过滤）

## 何时不要使用

- 简单"管理员看全部，普通用户看自己"——直接 `@PreAuthorize` SpEL 即可
- 跨服务的数据分发权限（应在数据源服务做过滤后返回）

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-row-security-starter')
```

```yaml
eagle.data-permission:
  enabled: true
  ignore-super-admin: true        # 超管 ROLE_SUPER_ADMIN 跳过过滤
```

需要业务方实现 `DataPermissionProvider` 提供"当前用户的部门列表 / 角色"。

## 核心 API

| 类 / 接口 / 注解 | 用途 |
|---|---|
| `@DataPermission` | 方法注解：声明数据范围（`type` / `deptColumn` / `creatorColumn`）|
| `@DataPermissionIgnore` | 方法注解：跳过本次过滤 |
| `DataScope` | 范围枚举：`ALL` / `DEPT_ONLY` / `DEPT_AND_CHILD` / `SELF_ONLY` / `CUSTOM` |
| `DataPermissionProvider` | **必须实现**：提供当前用户部门、角色信息 |
| `DataPermissionContext` | 当前请求的数据权限上下文 |
| `DataPermissionAspect` | 切面（自动注入 SQL 条件） |
| `DataPermissionHelper` | 编程式查询当前数据范围 |

## 最小示例

```java
// 1) 实现 Provider（业务方提供）
@Component
@RequiredArgsConstructor
public class MyDataPermissionProvider implements DataPermissionProvider {
    private final UserRepository userRepository;

    @Override
    public Set<Long> getCurrentUserDeptIds() {
        Long userId = SecurityUtils.getCurrentUserId();
        return userRepository.findDeptIdsByUserId(userId);
    }

    @Override
    public DataScope getCurrentUserDataScope() {
        // 根据当前用户角色决定范围
        if (SecurityUtils.hasRole("admin")) return DataScope.ALL;
        return DataScope.DEPT_AND_CHILD;
    }
}

// 2) 在 Controller / Service 上使用
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    @DataPermission(
        type = DataScope.DEPT_AND_CHILD,
        deptColumn = "dept_id",
        creatorColumn = "creator_id"
    )
    @GetMapping
    public Page<OrderResponse> list(@SpringQueryMap Pageable pageable) {
        return orderQueryService.findAll(pageable);
    }

    /** 跨范围操作（如导出全部）— 显式忽略 */
    @DataPermissionIgnore
    @PreAuthorize("hasRole('super_admin')")
    @GetMapping("/export-all")
    public Resource exportAll() { ... }
}

// 3) 编程式（特殊场景）
Set<Long> deptIds = DataPermissionHelper.getCurrentDeptIds();
```

## 配置项

| key | 类型 | 默认 | 说明 |
|---|---|---|---|
| `eagle.data-permission.enabled` | boolean | `true` | 总开关 |
| `eagle.data-permission.ignore-super-admin` | boolean | `true` | 超管跳过 |
| `eagle.data-permission.super-admin-role` | String | `super_admin` | 超管角色名 |

## 常见错误

- ❌ 在 Service 内 `if (admin) ... else ...` 手动判断 → ✅ 用 `@DataPermission`
- ❌ SQL 拼接数据范围 → ✅ 让 starter 拦截器统一注入
- ❌ 不实现 `DataPermissionProvider` → ✅ 必须提供（默认无业务部门信息）
- ❌ 只 `ignore`，不 `@PreAuthorize('admin')` → ✅ 同时声明权限注解防越权

## 关联规则

- `.claude/rules/17-tenant-permission.md` — 多租户 + 数据权限组合
- `.claude/rules/05-api.md` — `@PreAuthorize` 权限注解
- `.claude/rules/12-security.md` — 越权防护
