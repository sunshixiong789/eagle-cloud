# eagle-data-permission-starter

基于 AOP + JPA Specification 的行级数据权限控制模块，支持全部、本人、本部门、本部门及子部门、自定义部门五种权限范围，提供注解式和编程式两种使用方式。

## 目录

- [模块概述](#模块概述)
- [引入依赖](#引入依赖)
- [自动配置说明](#自动配置说明)
- [配置参考](#配置参考)
- [实现 DataPermissionProvider](#实现-datapermissionprovider)
- [权限范围说明](#权限范围说明)
- [功能使用说明](#功能使用说明)
    - [@DataPermission 注解](#datapermission-注解)
    - [@DataPermissionIgnore 跳过权限](#datapermissionignore-跳过权限)
    - [DataPermissionContext 编程式控制](#datapermissioncontext-编程式控制)
    - [DataPermissionHelper 手动构建](#datapermissionhelper-手动构建)
- [与 DDD 架构集成](#与-ddd-架构集成)
- [常见问题](#常见问题)

---

## 模块概述

### 提供的能力

| 分类   | 组件                       | 说明                             |
|------|--------------------------|--------------------------------|
| 注解过滤 | `@DataPermission`        | 标记方法，AOP 自动追加 Specification 条件 |
| 跳过权限 | `@DataPermissionIgnore`  | 标记方法，绕过数据权限过滤                  |
| 编程式  | `DataPermissionContext`  | ThreadLocal 覆盖，优先级高于 Provider  |
| 手动构建 | `DataPermissionHelper`   | 直接生成 Specification，不依赖 AOP     |
| 权限提供 | `DataPermissionProvider` | 业务方实现的接口，提供当前用户权限信息            |

### 工作原理

```
HTTP 请求
   │
   ▼
Controller → Application Service
                    │
                    │ @DataPermission
                    ▼
             DataPermissionAspect（AOP 拦截）
                    │
                    ├─ 读取权限范围（DataPermissionContext 优先）
                    ├─ 构建 Specification（DataPermissionHelper）
                    └─ 追加到原 Specification 参数
                              │
                              ▼
                    Repository.findAll(spec)
                              │
                              ▼
                    SQL WHERE + 数据权限条件
```

### 依赖关系

```
eagle-data-permission-starter
├── eagle-common-starter      ← 异常体系
├── eagle-data-jpa-starter    ← JPA Specification / Predicate
└── aspectjweaver             ← AOP 织入
```

---

## 引入依赖

在需要数据权限控制的服务模块 `build.gradle` 中添加：

```gradle
dependencies {
    implementation project(':eagle-starter:eagle-data-permission-starter')
}
```

引入后自动生效，还需要实现 [DataPermissionProvider](#实现-datapermissionprovider) 接口。

---

## 自动配置说明

`DataPermissionAutoConfiguration` 生效条件（同时满足）：

| 条件                                   | 说明               |
|--------------------------------------|------------------|
| `eagle.data-permission.enabled=true` | 配置开关，默认 `true`   |
| 容器中存在 `DataPermissionProvider` Bean  | **业务方必须自行实现并注册** |

满足条件后自动注册 `DataPermissionAspect` Bean。若需替换默认切面，声明同类型 Bean 即可覆盖：

```java

@Bean
public DataPermissionAspect dataPermissionAspect(DataPermissionProvider provider,
                                                 DataPermissionProperties properties) {
    return new MyCustomDataPermissionAspect(provider, properties);
}
```

---

## 配置参考

```yaml
eagle:
  data-permission:
    enabled: true                  # 是否启用数据权限（默认 true）
    default-dept-field: deptId     # 实体中部门 ID 字段的默认名称（默认 deptId）
    default-user-field: id         # 实体中用户 ID 字段的默认名称（默认 id）
```

### 配置项说明

| 配置项                                        | 默认值      | 说明                                     |
|--------------------------------------------|----------|----------------------------------------|
| `eagle.data-permission.enabled`            | `true`   | 设为 `false` 时切面不生效，所有查询不追加权限条件          |
| `eagle.data-permission.default-dept-field` | `deptId` | 全局默认部门字段名，`@DataPermission` 注解未指定时使用此值 |
| `eagle.data-permission.default-user-field` | `id`     | 全局默认用户字段名，`@DataPermission` 注解未指定时使用此值 |

> **字段名优先级：** `@DataPermission(deptField = "xxx")` 显式设置 > `eagle.data-permission.default-dept-field` 全局配置

---

## 实现 DataPermissionProvider

`DataPermissionProvider` 是数据权限的核心扩展点，必须在业务服务中实现并注册为 Spring Bean。

```java
/**
 * 数据权限提供者实现（放在 eagle-system-server 的 infrastructure/security/ 下）。
 */
@Component
@RequiredArgsConstructor
public class SecurityDataPermissionProvider implements DataPermissionProvider {

    private final UserRepository userRepository;
    private final DeptRepository deptRepository;

    /**
     * 获取当前用户的数据权限范围。
     * 取用户所有角色中权限最大的那个（ALL > CUSTOM > DEPT_AND_CHILD > DEPT > SELF）。
     */
    @Override
    public DataScope getCurrentUserDataScope() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return DataScope.SELF;
        }
        return userRepository.findMaxDataScopeByUserId(userId)
                .orElse(DataScope.SELF);
    }

    @Override
    public Long getCurrentUserDeptId() {
        return SecurityUtils.getCurrentUserDeptId();
    }

    @Override
    public Long getCurrentUserId() {
        return SecurityUtils.getCurrentUserId();
    }

    /**
     * 获取用户自定义授权的部门 ID 集合（CUSTOM 范围使用）。
     * 通常存储在角色-部门关联表中。
     */
    @Override
    public Set<Long> getCurrentUserCustomDeptIds() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Collections.emptySet();
        }
        return userRepository.findCustomDeptIdsByUserId(userId);
    }

    /**
     * 获取指定部门及其所有子部门 ID（DEPT_AND_CHILD 范围使用）。
     * 建议实现时增加缓存，避免每次查询都递归查部门树。
     */
    @Override
    @Cacheable(value = "DEPT_CHILDREN_CACHE", key = "#deptId")
    public Set<Long> getChildDeptIds(Long deptId) {
        return deptRepository.findAllChildIds(deptId);
    }
}
```

> **性能建议：** `getChildDeptIds()` 通常涉及递归查询部门树，强烈建议加 `@Cacheable` 缓存，避免每次数据查询都触发部门树遍历。

---

## 权限范围说明

| 枚举值              | 含义        | 生成的 SQL 条件                      |
|------------------|-----------|---------------------------------|
| `ALL`            | 全部数据（不过滤） | 无 WHERE 条件                      |
| `CUSTOM`         | 自定义授权部门   | `deptId IN (自定义部门 ID 列表)`       |
| `DEPT_AND_CHILD` | 本部门及子部门   | `deptId IN (本部门 ID, 子部门 ID...)` |
| `DEPT`           | 仅本部门      | `deptId = 当前用户部门 ID`            |
| `SELF`           | 仅本人       | `id = 当前用户 ID`                  |

**降级规则（字段为空时自动降级）：**

```
DEPT/DEPT_AND_CHILD → deptId 为 null → 降级到 SELF
CUSTOM → customDeptIds 为空 → 降级到 SELF（并输出 WARN 日志）
SELF → userId 为 null → 不追加过滤（相当于不限制，并输出 WARN 日志）
```

---

## 功能使用说明

### @DataPermission 注解

在 Application Service 或 Repository 方法上标注，切面自动找到第一个 `Specification<T>` 参数并追加权限条件。

**基本用法：**

```java

@Service
@RequiredArgsConstructor
public class UserApplicationService {

    private final UserRepository userRepository;

    /**
     * 查询用户列表，自动追加数据权限条件。
     * deptField/userField 使用全局配置（eagle.data-permission.default-dept-field）。
     */
    @DataPermission
    @Transactional(readOnly = true)
    public Page<UserResponse> findUsers(UserQueryRequest request, Pageable pageable) {
        Specification<User> spec = UserSpecification.build(request);
        // AOP 自动将 spec 替换为 spec.and(权限条件)
        return userRepository.findAll(spec, pageable).map(userMapper::toResponse);
    }
}
```

**自定义字段名（实体字段名与默认不一致时）：**

```java
// 实体类中部门字段叫 "department"，用户字段叫 "creatorId"
@DataPermission(deptField = "department", userField = "creatorId")
@Transactional(readOnly = true)
public Page<OrderResponse> findOrders(OrderQueryRequest request, Pageable pageable) {
    return orderRepository.findAll(OrderSpecification.build(request), pageable)
            .map(orderMapper::toResponse);
}
```

**全局修改字段名默认值（推荐方式）：**

```yaml
# 若项目中所有实体的部门字段都叫 "departmentId"，统一在配置文件修改
eagle:
  data-permission:
    default-dept-field: departmentId
    default-user-field: creatorId
```

```java
// 此时无需每个方法都写 deptField
@DataPermission
public Page<OrderResponse> findOrders(...) { ...}
```

> **注意：** `@DataPermission` 方法必须有 `Specification<T>` 类型参数，否则权限过滤**不会生效**并输出 `WARN` 日志。

---

### @DataPermissionIgnore 跳过权限

在方法上添加 `@DataPermissionIgnore`，该方法将以全量数据执行，绕过数据权限过滤。

**适用场景：**

- 超级管理员专属操作
- 全局统计、汇总报表
- 系统内部调用（权限检查无意义）

```java

@Service
@RequiredArgsConstructor
public class ReportApplicationService {

    private final OrderRepository orderRepository;

    /**
     * 全局销售报表：超管查所有数据，不受部门权限限制。
     */
    @DataPermissionIgnore
    @Transactional(readOnly = true)
    public SalesReportResponse generateGlobalReport(LocalDate start, LocalDate end) {
        // 此方法不会触发数据权限过滤
        List<Order> allOrders = orderRepository.findAll(OrderSpec.byDateRange(start, end));
        return reportMapper.toSalesReport(allOrders);
    }

    /**
     * 普通用户查询：正常应用数据权限。
     */
    @DataPermission
    @Transactional(readOnly = true)
    public Page<OrderResponse> findMyOrders(OrderQueryRequest request, Pageable pageable) {
        return orderRepository.findAll(OrderSpec.build(request), pageable)
                .map(orderMapper::toResponse);
    }
}
```

---

### DataPermissionContext 编程式控制

当无法通过注解控制权限时（定时任务、异步方法、需要动态切换范围），使用 `DataPermissionContext`。

**优先级：** `DataPermissionContext` 设置的范围 > `DataPermissionProvider` 返回的范围。

#### 忽略权限（推荐 Lambda 版）

```java

@Scheduled(cron = "0 0 2 * * ?")
public void nightlyDataSync() {
    // 定时任务需要处理全量数据，忽略权限过滤
    DataPermissionContext.ignorePermission(() -> {
        List<User> allUsers = userRepository.findAll(activeUserSpec);
        syncService.syncToWarehouse(allUsers);
    });
}
```

#### 临时切换到指定权限范围

```java
public void exportDeptData(Long targetDeptId) {
    // 临时切换为只看指定部门数据（DEPT 范围）
    DataPermissionContext.withScope(DataScope.DEPT, () -> {
        // 此块内所有 @DataPermission 方法都以 DEPT 范围执行
        Page<User> deptUsers = userService.findUsers(query, pageable);
        exportService.export(deptUsers.getContent());
    });
}
```

#### 有返回值的场景

```java
public ReportData buildCrossOrgReport() {
    // 跨部门统计，需要访问所有数据
    return DataPermissionContext.ignorePermission(() ->
            reportRepository.findAll(reportSpec)
                    .stream()
                    .collect(Collectors.groupingBy(Report::getDeptId))
    );
}
```

#### 手动管理（必须 finally 清理）

```java
public void batchProcess() {
    try {
        DataPermissionContext.setScope(DataScope.ALL);
        // 处理业务...
        processAllUsers();
    } finally {
        DataPermissionContext.clear();  // 必须清理，防止 ThreadLocal 内存泄漏
    }
}
```

> **异步注意：** `DataPermissionContext` 基于 `ThreadLocal`，**不会自动继承到子线程**。
> `@Async` 方法或线程池任务中需在子线程重新设置：
>
> ```java
> @Async
> public void asyncTask() {
>     DataPermissionContext.ignorePermission(() -> {
>         // 异步线程内显式设置权限
>         doSomething();
>     });
> }
> ```

---

### DataPermissionHelper 手动构建

在不便使用 AOP（如 `@Query` 方法、动态构建 Specification 的场景），直接调用工具类手动构建权限条件。

```java

@Service
@RequiredArgsConstructor
public class UserApplicationService {

    private final UserRepository userRepository;
    private final DataPermissionProvider permissionProvider;

    @Transactional(readOnly = true)
    public Page<UserResponse> findUsersWithManualPermission(UserQueryRequest request,
                                                            Pageable pageable) {
        Specification<User> businessSpec = UserSpecification.build(request);

        // 手动构建并合并权限条件（内部会读 DataPermissionContext 或 Provider）
        Specification<User> finalSpec = DataPermissionHelper.specification(
                permissionProvider,
                "deptId",    // 部门字段名
                "id",        // 用户字段名
                businessSpec // 已有业务条件（会与权限条件 AND 合并）
        );

        return userRepository.findAll(finalSpec, pageable).map(userMapper::toResponse);
    }
}
```

---

## 与 DDD 架构集成

### 文件放置位置

```
eagle-system-server/
└── src/main/java/com/eagle/system/
    ├── base/
    │   ├── application/service/
    │   │   └── UserApplicationService.java     ← @DataPermission 加在这里
    │   └── infrastructure/
    │       └── security/
    │           └── SecurityDataPermissionProvider.java  ← Provider 实现放这里
    └── config/
        └── DataPermissionConfig.java           ← 注册 Provider Bean（可选）
```

### 典型完整示例

**实体（领域层）：**

```java

@Entity
@Table(name = "t_user")
@Getter
@NoArgsConstructor
public class User extends BaseAggregateRoot<User> {

    @Column(name = "dept_id", comment = "部门 ID")
    private Long deptId;

    @Column(name = "username", nullable = false, length = 50, comment = "用户名")
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, comment = "状态")
    private UserStatus status = UserStatus.ACTIVE;
}
```

**Specification 查询条件构建（领域层或基础设施层）：**

```java
public class UserSpecification {

    public static Specification<User> build(UserQueryRequest request) {
        return Specification
                .where(likeUsername(request.getUsername()))
                .and(equalStatus(request.getStatus()))
                .and(equalDeptId(request.getDeptId()));
    }

    private static Specification<User> likeUsername(String username) {
        return (root, query, cb) -> {
            if (username == null || username.isBlank()) return null;
            return cb.like(root.get("username"), "%" + username + "%");
        };
    }

    private static Specification<User> equalStatus(UserStatus status) {
        return (root, query, cb) -> {
            if (status == null) return null;
            return cb.equal(root.get("status"), status);
        };
    }

    private static Specification<User> equalDeptId(Long deptId) {
        return (root, query, cb) -> {
            if (deptId == null) return null;
            return cb.equal(root.get("deptId"), deptId);
        };
    }
}
```

**应用服务（应用层）—— @DataPermission 加在这里：**

```java

@Service
@RequiredArgsConstructor
public class UserApplicationService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * 分页查询用户（自动追加数据权限条件）。
     */
    @DataPermission
    @Transactional(readOnly = true)
    public Page<UserResponse> findUsers(UserQueryRequest request, Pageable pageable) {
        Specification<User> spec = UserSpecification.build(request);
        return userRepository.findAll(spec, pageable).map(userMapper::toResponse);
    }

    /**
     * 超管查看全部用户（绕过数据权限）。
     */
    @DataPermissionIgnore
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Page<UserResponse> findAllUsersForAdmin(UserQueryRequest request, Pageable pageable) {
        return userRepository.findAll(UserSpecification.build(request), pageable)
                .map(userMapper::toResponse);
    }

    /**
     * 删除用户（写操作不需要数据权限，权限校验通过 @PreAuthorize 控制）。
     */
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasRole('admin') or #userId == authentication.principal.id")
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }
}
```

**Controller（表现层）—— 不加 @DataPermission：**

```java

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "用户管理")
public class UserController {

    private final UserApplicationService userApplicationService;

    @GetMapping
    @PreAuthorize("hasRole('admin') or hasAuthority('user:list')")
    public Page<UserResponse> findUsers(UserQueryRequest request, Pageable pageable) {
        return userApplicationService.findUsers(request, pageable);
    }
}
```

---

## 常见问题

**Q: 加了 `@DataPermission` 但查询结果没有过滤，数据还是全量的？**

A: 依次检查：

1. 容器中是否注册了 `DataPermissionProvider` Bean（缺少会导致切面不创建）
2. `DataPermissionProvider.getCurrentUserDataScope()` 是否返回了 `ALL`（ALL 范围不过滤）
3. 方法参数中是否有 `Specification<T>` 类型的参数（缺少时切面无法注入条件，会打 WARN 日志）
4. 调用是否通过 Spring 代理（同类内部调用 AOP 不生效）

---

**Q: 日志里出现 `@DataPermission on method [...] but no Specification parameter found`？**

A: 方法上有 `@DataPermission` 注解，但方法参数中没有 `Specification<T>` 类型的参数，切面无法注入权限条件。解决方案：

1. 在方法签名中增加 `Specification<T>` 参数
2. 如果不便修改签名，改用 `DataPermissionHelper` 手动构建

---

**Q: 定时任务查询的数据不对（只有部分数据）？**

A: 定时任务没有用户登录的 Security Context，`DataPermissionProvider` 可能返回 `SELF` 范围，导致只查到部分数据。解决方案：

```java

@Scheduled(cron = "0 0 2 * * ?")
public void scheduledTask() {
    // 方式一：@DataPermissionIgnore 注解
    // 方式二：DataPermissionContext（定时任务场景推荐）
    DataPermissionContext.ignorePermission(() -> {
        List<User> allUsers = userApplicationService.findUsersForSchedule();
    });
}
```

---

**Q: `CUSTOM` 权限范围自定义部门 ID 从哪里来？**

A: 由 `DataPermissionProvider.getCurrentUserCustomDeptIds()` 提供，通常存在角色-部门关联表中。典型数据模型：

```
t_role ─── t_role_dept_scope ─── t_dept
  id              role_id              id
  name            dept_id
  data_scope(CUSTOM)
```

当角色的 `dataScope = CUSTOM` 时，查 `t_role_dept_scope` 表获取该角色被授权的部门 ID 集合。

---

**Q: `DEPT_AND_CHILD` 范围查询很慢？**

A: `getChildDeptIds()` 默认每次都递归查询部门树。务必在实现类中加 `@Cacheable`：

```java

@Override
@Cacheable(value = "DEPT_CHILDREN_CACHE", key = "#deptId")
public Set<Long> getChildDeptIds(Long deptId) {
    return deptRepository.findAllChildIds(deptId);
}
```

部门结构变更时失效缓存：

```java

@CacheEvict(value = "DEPT_CHILDREN_CACHE", allEntries = true)
public void updateDept(Long deptId, UpdateDeptRequest request) { ...}
```

---

**Q: `DataPermissionContext` 在 `@Async` 方法中不生效？**

A: `ThreadLocal` 不会自动传递到子线程。`@Async` 方法运行在独立线程中，父线程的 `DataPermissionContext`
设置不可见。解决方案：在异步方法内部重新设置：

```java

@Async
public void asyncExport() {
    // 在子线程内显式忽略权限
    DataPermissionContext.ignorePermission(() -> {
        exportService.doExport();
    });
}
```

---

**Q: 写操作（新增、修改、删除）需要加 `@DataPermission` 吗？**

A: **不需要**。`@DataPermission` 仅用于控制**查询范围**（读操作）。写操作的权限控制通过 `@PreAuthorize` 完成，例如：

```java

@PreAuthorize("hasRole('admin') or #userId == authentication.principal.id")
public void updateUser(Long userId, UpdateUserRequest request) { ...}
```