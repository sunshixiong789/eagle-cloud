# 安全、多租户与日志

三层架构：授权服务器（`eagle-auth-service`）+ 资源服务器（`eagle-resource-server-starter`）+ 网关。

## 获取当前用户：只有一种写法

starter 把 JWT 转成以 `EagleUser` 为 principal 的 `EagleAuthentication`：

```java
// ✅ 二选一
EagleUser user = SecurityUtils.getCurrentUser();          // 或 getCurrentUserId()
public UserResponse me(@AuthenticationPrincipal EagleUser principal) { ... }

// ❌ 绕过 EagleUser 抽象，当前配置下可能拿到 null principal
public UserResponse me(@AuthenticationPrincipal Jwt jwt) { ... }

// ❌ 自己解码 Token
String token = request.getHeader("Authorization").substring(7);
```

业务服务**禁止**自行实现 Token 解析；服务间调用由 `eagle-restclient-starter` 自动透传 `Authorization`，**禁止**手动拼装。

接口权限声明见 `03-api-error.md`。

## Token

- 有效期由 auth-service 配置：`access-token-ttl-seconds: 3600`（1 小时）、`refresh-token-ttl-seconds: 2592000`（30 天）
- 仅签名不加密 —— **禁止**在 Token 中携带密码、身份证、手机号明文
- 撤销与在线态走 Redis（[OnlineUserAdapter](eagle-services/eagle-auth-service/src/main/java/com/eagle/auth/core/infrastructure/adapter/OnlineUserAdapter.java)）：
  - 黑名单 `token:blacklist:{jti}`
  - 账号在线索引 `account:online:{accountId}`
- **禁止**在 URL 中传 Token（会被日志 / Referer / 浏览器历史捕获）

## 密码与凭证

- BCrypt cost = 12（`new BCryptPasswordEncoder(12)`）。**禁止** MD5 / SHA1 / 明文
- 密码字段加 `@JsonProperty(access = WRITE_ONLY)` 防序列化输出
- 第三方密钥（DB / OSS / SMS / 支付）用 Jasypt：`password: ENC(...)`，主密钥经环境变量 `JASYPT_ENCRYPTOR_PASSWORD` 注入，**禁止**写入 Git / 配置文件 / 镜像
- 字段级加密用 `eagle-encrypt-starter` 的 `EncryptedStringConverter` + `@Convert`
- **禁止** `Random` 生成 Token / 验证码，必须 `SecureRandom`

## 脱敏：只有日志脱敏，没有响应脱敏

**本仓库没有 `@Sensitive` 注解，也没有响应体自动脱敏机制** —— 不要臆造。

唯一可用的是 [LogMask](eagle-starter/eagle-common-starter/src/main/java/com/eagle/common/util/LogMask.java)，静态方法，**仅用于日志输出，不用于响应体和存储**：

```java
log.info("用户注册 username={}, phone={}, email={}",
        username, LogMask.phone(phone), LogMask.email(email));
```

| 方法 | 效果 |
|---|---|
| `LogMask.phone(s)` | `13800001234` → `138****1234` |
| `LogMask.email(s)` | `alice@example.com` → `a***@example.com` |
| `LogMask.idCard(s)` | → `110***********1234` |
| `LogMask.token(s)` | 仅前 8 位 + `***` |

入参为 `null` 或过短时返回 `***`，避免脱敏算法本身泄漏长度信息。

**响应体需要脱敏时**：目前没有统一机制，在 DTO 的静态工厂 / Mapper 里显式处理，并在 PR 中说明。不要在 Controller 里手写 `substring`。

## 输入安全

| 风险 | 防护 |
|---|---|
| SQL 注入 | 统一 JPA 命名参数 / 方法名查询，**禁止**拼接 SQL |
| XSS | 富文本走 OWASP Java HTML Sanitizer 白名单 |
| SSRF | 外部 URL 调用前校验域名白名单，**禁止**拿用户输入直接发 HTTP |
| 反序列化 | **禁止** `ObjectInputStream` 反序列化用户输入 |
| 文件上传 | 后缀白名单 + 魔数检测真实 MIME（**不信任**前端 `Content-Type`）+ 重命名为 UUID（详见 `eagle-oss-minio` skill） |

## 速率限制

登录 / 注册 / 短信验证码用 `eagle-redis-starter` 的 `RedisRateLimiter`；业务接口走网关 Sentinel。**禁止**自行实现限流计数器。

---

# 多租户与数据权限

## 租户上下文

- API：`TenantContextHolder.getTenantId()` / `setTenantId()` / `clear()`（**没有** `getCurrentTenantId()`）
- 装配条件是 `eagle.tenant.mode`（`column` / `database`，默认 `COLUMN`）—— **不存在 `eagle.tenant.enabled`**
- 跨线程传递由 `ContextPropagationConfig` 统一处理；异步任务结束必须 `clear()`
- 路由 key 只来自可信上下文，**不信任前端传的 tenantId / deptId / userId**

## 隔离

- COLUMN 模式：业务表带 `tenant_id`，**应用层禁止手动拼 `WHERE tenant_id = ?`**，由 starter 注入
- `@TenantFilter` 标在 **Service / Repository** 上，**不标在 `@Entity` 上**
- DATABASE 模式走 starter 数据源路由，不在业务代码手选 DataSource

## 行级数据权限

- `@DataPermission(deptField, userField)` 声明范围
- `DataScope` 固定五值：`ALL` / `SELF` / `DEPT` / `DEPT_AND_CHILD` / `CUSTOM`
- 业务方实现 `DataPermissionProvider`
- 先租户隔离，再行级过滤

## 跨租户操作

必须显式提供 reason + 写审计日志 + `try/finally` 恢复上下文。**禁止**普通业务路径隐式跨租户查询或批量操作。

---

# 审计日志

用 `eagle-audit-log-starter` 的 `@AuditLog`，异步落表 **`eagle_audit_log`**（[AuditLogRecord](eagle-starter/eagle-audit-log-starter/src/main/java/com/eagle/audit/model/AuditLogRecord.java)）。

必须审计的操作：

- 登录 / 登出 / 密码修改 / Token 刷新
- 角色 / 权限分配
- 敏感数据导出（用户列表、订单列表）
- 删除聚合根
- 跨租户管理操作

多租户项目用 `TenantAwareSecurityAuditLogUserProvider`（普通的 `SecurityAuditLogUserProvider` 不带 tenantId）。

---

# 日志规范

## 级别

| 级别 | 用途 |
|---|---|
| `INFO` | 关键业务里程碑、跨服务调用摘要、MQ 收发摘要、任务开始/结束 |
| `WARN` | 可恢复但需关注：降级、限流、外部依赖不稳定 |
| `ERROR` | 需排查或告警的失败，**异常对象作为最后一个独立参数**传入 |
| `DEBUG/TRACE` | 请求体、响应体、SQL 参数、高频循环细节 |

## 必须埋点

状态机变更（创建 / 支付 / 取消 / 退款 / 注册 / 注销 / 授权）、涉及金额或库存或配额的写操作、远程调用与 MQ 与分布式事务与分布式锁的边界、外部依赖（支付 / 短信 / OSS）、定时与异步任务的开始结束耗时结果、登录登出鉴权失败。

## MDC

traceId / spanId 由 `eagle-tracing-starter` 注入。自定义 MDC 必须 `try/finally` 成对 `put/remove`，否则线程池污染。

## 禁止清单

- `System.out` / `printStackTrace()`
- 字符串拼接日志、丢失堆栈的异常日志
- 每层都打进入/退出日志；循环内打 `INFO`
- 完整入参 / 出参 / 请求体 / 响应体打到 `INFO`
- 输出密码、Token、Cookie、验证码、密钥、身份证、银行卡、**未经 `LogMask` 处理的手机号/邮箱**
