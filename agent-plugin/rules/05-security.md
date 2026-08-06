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

## 自定义 `SecurityFilterChain` 的强制动作

starter 默认 chain 已经接好了 JWT → `EagleUser` 的转换。一旦业务服务自定义 chain **取代**默认 chain，
这个转换就没了，而且**不会报错** —— principal 退化成原生 `Jwt`，权限集合为空，
所有 `hasRole(...)` 静默失效变成 403，排查成本极高。

```java
// ✅ 自定义 chain 时必须显式接回 Converter
@Bean
SecurityFilterChain filterChain(HttpSecurity http, EagleJwtAuthenticationConverter converter) {
    return http
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/internal/**").permitAll()
                    .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))   // ← 漏了它 = hasRole 全废
            .build();
}
```

自检：自定义 chain 后跑一个带 `@PreAuthorize("hasRole('admin')")` 的接口，403 就是漏了 Converter。
能不自定义就不自定义 —— 公开路径优先走 yml `eagle.resource-server.permit-paths`（见 `03-api-error.md`）。

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

`eagle-sentinel-starter` **已移除**，当前有两条限流路径：

| 场景 | 手段 |
|---|---|
| 声明式（方法级） | `eagle-resilience-starter` 的 `@RateLimit`（配 `RateLimitBehavior`），配置前缀 `eagle.resilience` |
| 编程式（登录 / 注册 / 短信验证码） | `eagle-redis-starter` 的 `RedisRateLimiter`；auth 侧另有 `eagle.security.login-rate-limit` |

**禁止**自行实现限流计数器。

---

# 数据权限

## 多租户能力已整体移除

`eagle-tenant-starter` 源码已清空并移出 `settings.gradle`。**以下全部不存在，不要写、也不要"顺手补回来"**：

- `TenantContextHolder`、`@TenantFilter`、`TenantIdFilter`
- 配置键 `eagle.tenant.*`（含 `mode` / `enabled`）
- `ContextPropagationConfig` 的租户传播（现仅传播 MDC `requestId` 与 `PressureTestContext`）
- `TenantAwareSecurityAuditLogUserProvider`

残留痕迹**不代表能力还在**：`AuditLogRecord` 仍有 `tenant_id` 列与 `idx_audit_log_tenant` 索引，
但已无写入方，恒为 null；少数类的 Javadoc 里还提到 `TenantContextHolder`，那是历史说明。
新表**不要**再加 `tenant_id` 列。

若将来要恢复多租户，须重新引入 starter 并同步重写本节 —— 在那之前，
任何"租户隔离"需求都要先和需求方确认，不要自行用 `WHERE tenant_id = ?` 土法实现。

## 行级数据权限

无 `@DataPermission` / `DataPermissionProvider` / `DataPermissionContext` —— **不要按注解式数据权限写代码**。

当前只剩业务侧的范围枚举 [DataScope](eagle-services/eagle-system-service/src/main/java/com/eagle/system/base/domain/model/enums/DataScope.java)，作为 `Role` 的普通字段（默认 `SELF`）：

```java
private DataScope dataScope = DataScope.SELF;   // ALL / SELF / DEPT / DEPT_AND_CHILD / CUSTOM
```

需要按范围过滤时，在应用服务或 Repository 查询条件里**显式**处理。

---

# 审计日志

用 `eagle-audit-log-starter` 的 `@AuditLog`，异步落表 **`eagle_audit_log`**（[AuditLogRecord](eagle-starter/eagle-audit-log-starter/src/main/java/com/eagle/audit/model/AuditLogRecord.java)）。

必须审计的操作：

- 登录 / 登出 / 密码修改 / Token 刷新
- 角色 / 权限分配
- 敏感数据导出（用户列表、订单列表）
- 删除聚合根

用户身份由 `SecurityAuditLogUserProvider` 提供（多租户版本已随 tenant-starter 一并移除）。

---

# 可观测性（日志 / 指标 / 追踪）

三者分工：**日志**回答"发生了什么"，**指标**回答"发生了多少/多快"，**追踪**回答"在哪一环"。
不要用日志代替指标 —— 靠 `grep` 日志算 QPS 是反模式。

## 指标：`BusinessMetrics`

[BusinessMetrics](eagle-starter/eagle-common-starter/src/main/java/com/eagle/common/metrics/BusinessMetrics.java)
由 `eagle-common-starter` 自动装配（存在 `MeterRegistry` 时生效），**直接注入即可**，不要自建 `Counter`/`Timer`：

```java
@Service
@RequiredArgsConstructor
public class OrderApplicationService {

    private final BusinessMetrics metrics;

    @Transactional(rollbackFor = Exception.class)
    public void createOrder(CreateOrderRequest req) {
        Timer.Sample sample = metrics.startTimer();
        try {
            // ... 业务
            metrics.incrementOrderCreated(req.channel());
        } finally {
            metrics.recordDuration("order.create", sample);
        }
    }
}
```

现有语义化方法（指标名统一 `eagle.` 前缀）：

| 方法 | 指标 | tag |
|---|---|---|
| `incrementOrderCreated(channel)` | `eagle.order.created` | `channel` |
| `incrementOrderCancelled(reason)` | `eagle.order.cancelled` | `reason` |
| `incrementPaymentSuccess(method)` | `eagle.payment.success` | `method` |
| `incrementPaymentFailed(reason)` | `eagle.payment.failed` | `reason` |
| `incrementInventoryDeducted(warehouseId)` | `eagle.inventory.deducted` | `warehouse` |
| `incrementInventoryInsufficient()` | `eagle.inventory.insufficient` | — |
| `incrementRateLimited(resource)` | `eagle.rate.limited` | `resource` |
| `incrementCircuitBreaker(service)` | `eagle.circuit.breaker` | `service` |
| `startTimer()` / `recordDuration(op, sample)` | `eagle.{op}` 耗时 | — |

**现状**：`BusinessMetrics` 目前**业务代码零使用**（只有自身单测覆盖）。新写涉及金额 / 库存 / 状态机流转的用例时应当补上。

## 必须埋指标的场景

- 金额、库存、配额的增减
- 状态机流转（创建 / 支付 / 取消 / 退款）
- 限流触发、熔断开合、降级发生
- 外部依赖（支付 / 短信 / OSS）的成功率与耗时

## tag 基数红线

**tag 值必须是低基数枚举**。把 `userId`、`orderId`、`traceId`、原始 URL 当 tag 会导致时序库指标爆炸（每个值一条时间序列），这是压垮监控系统最常见的原因。高基数信息放日志或追踪，不放指标。

## 追踪

traceId / spanId 由 `eagle-tracing-starter` 注入 MDC，日志自动带上。**生产必须调低采样率** —— `eagle.tracing.sampling-probability` 默认 `1.0`（全采样）。

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
