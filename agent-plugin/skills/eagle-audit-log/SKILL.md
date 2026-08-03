---
name: eagle-audit-log
description: Use when adding operation audit logging in eagle-cloud projects — @AuditLog annotation on service/controller methods, AuditLogHandler (JpaAuditLogHandler persists to t_audit_log, LoggingAuditLogHandler), AuditLogUserProvider, async event-driven via AuditLogEventListener
---

# eagle-audit-log-starter — 操作审计日志

## 何时使用

- 登录 / 登出、权限变更、敏感数据导出、聚合根删除等必须留存审计记录
- 需要在 Swagger UI 后台或安全报告中查询操作历史
- 替代"手工在每个方法里写 log.info"的散乱审计

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-audit-log-starter')
// JPA 持久化（推荐）还需要：
implementation project(':eagle-starter:eagle-data-jpa-starter')
```

```yaml
eagle:
  audit-log:
    enabled: true
    log-args: true          # 全局：是否记录请求参数
    log-result: false       # 全局：是否记录返回值
    async: true             # 异步写入（默认 true，用 taskExecutor 池）
```

## 核心 API

| 类 / 接口                              | 用途                                                |
|---------------------------------------|---------------------------------------------------|
| `@AuditLog`                           | 方法注解，声明要审计的操作                                      |
| `AuditLogHandler`                     | 处理器接口，自定义实现可覆盖默认行为                                 |
| `JpaAuditLogHandler`                  | 默认实现，持久化到 `t_audit_log`（`@ConditionalOnClass(JpaRepository)`) |
| `LoggingAuditLogHandler`              | 降级实现，无 JPA 时 fallback 到 `log.info`                |
| `AuditLogUserProvider`                | SPI，返回当前操作人信息（`userId / username / ip / tenantId`） |
| `SecurityAuditLogUserProvider`        | 默认实现，从 `SecurityContext` + `HttpServletRequest` 取值 |
| `TenantAwareSecurityAuditLogUserProvider` | 多租户版本，附加 `tenantId` 字段                          |
| `AuditLogEventListener`               | 异步事件监听器，`@TransactionalEventListener(AFTER_COMMIT)` |
| `AuditLogRepository`                  | JPA Repository，支持条件查询 + 分页                        |

## `@AuditLog` 注解

```java
// 标注在 Service 或 Controller 方法上
@AuditLog(
    module = "订单管理",
    action = "创建订单",
    logArgs = true,         // 是否记录入参（敏感接口设 false）
    logResult = false       // 是否记录返回值
)
@Transactional(rollbackFor = Exception.class)
public OrderResponse createOrder(CreateOrderRequest request) { ... }
```

- `action` 支持 SpEL（引用方法参数）：`action = "删除用户 #userId"`
- 敏感方法（密码修改、密钥操作）务必 `logArgs = false`

## 最小示例

```java
// 1) 标注业务方法
@Service
@RequiredArgsConstructor
public class RoleApplicationService {

    @AuditLog(module = "权限管理", action = "分配角色")
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, Set<Long> roleIds) {
        // 业务逻辑...
    }

    @AuditLog(module = "用户管理", action = "删除用户 #userId", logArgs = false)
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId) {
        // 业务逻辑...
    }
}

// 2) 自定义 AuditLogHandler（覆盖默认 Jpa 持久化行为）
@Component
@Primary
public class MqAuditLogHandler implements AuditLogHandler {

    private final DomainEventPublisher publisher;

    @Override
    public void handle(AuditLogRecord record) {
        publisher.publish("audit_log_events", "created",
                new AuditLogCreatedEvent(record));
    }
}

// 3) 查询审计日志（注入 AuditLogRepository）
@Service
@RequiredArgsConstructor
public class AuditQueryService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public Page<AuditLog> queryByOperator(Long operatorId, Pageable pageable) {
        return auditLogRepository.findAll(
                AuditLogSpecification.byOperatorId(operatorId), pageable);
    }
}
```

## t_audit_log 表结构

审计日志表由 Flyway 脚本自动创建（`JpaAuditLogHandler` 存在时）：

| 字段             | 说明              |
|----------------|-----------------|
| `operator_id`  | 操作人 ID         |
| `operator_name`| 操作人姓名          |
| `tenant_id`    | 租户 ID           |
| `module`       | 操作模块           |
| `action`       | 操作描述           |
| `args`         | 请求参数（JSON）      |
| `result`       | 返回结果（JSON，可选）   |
| `status`       | 成功 / 失败         |
| `error_msg`    | 失败原因            |
| `ip`           | 操作人 IP          |
| `user_agent`   | 浏览器标识           |
| `occurred_at`  | 操作时间            |

## 常见错误

- ❌ `@AuditLog` 标在私有方法上 → ✅ 必须是 `public` 方法且通过 Spring 代理调用（同类内部调用无效）
- ❌ 敏感接口未设 `logArgs = false` → ✅ 密码、Token 字段会被序列化进 `args` 字段
- ❌ 删除用户后找不到审计记录 → ✅ 确认 `async: true` 时 `taskExecutor` Bean 已注册（eagle-common-starter 默认注册）
- ❌ 多租户项目用 `SecurityAuditLogUserProvider` 缺少 tenantId → ✅ 改用 `TenantAwareSecurityAuditLogUserProvider`

## 关联规则

- `.claude/rules/05-security.md` — 哪些操作必须写审计日志 / 审计事件不靠 log.info，靠 @AuditLog
- `.claude/rules/04-data.md` — AFTER_COMMIT 异步事件
