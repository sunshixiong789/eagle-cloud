# 黑名单 + 用户冻结 设计文档

- **日期**：2026-05-19
- **范围**：`eagle-system-service`（`auth` 域 + 少量 `base` 域清理）
- **状态**：设计草案（待评审）
- **作者**：sunshixiong（with Claude Code）

---

## 1. 背景与目标

### 1.1 现状

| 能力 | 现状 | 位置 |
|------|------|------|
| Token 黑名单（JTI 级，强制下线） | ✅ 已有 | `OnlineUserPort.isBlacklisted` + Redis `token:blacklist:{jti}` + `BlacklistAwareJwtDecoder` |
| 账号锁定 | ⚠️ 仅 `Account.locked` boolean，无原因 / 期限 / 操作人 / 审计 | `auth/domain/model/Account.lock()/unlock()`、`AccountController` |
| IP 登录限流 | ✅ 已有 | `LoginRateLimitFilter` + `LoginAttemptService` |
| 身份级黑名单（手机号 / IP / openid / 邮箱 / 账号 ID） | ❌ 缺失 | — |
| `UserLockedEvent`（base 域） | 🟡 遗留：Account 已迁移到 auth，base 内字段已无 lock | 顺手清理 |

### 1.2 目标

1. 升级账号锁定为完整的"冻结"模型：原因、到期时间、操作人、备注、审计事件
2. 新增**租户级**身份黑名单聚合：支持 ACCOUNT_ID / PHONE / EMAIL / IP / OPENID 五种类型，可永久或带过期时间
3. 黑名单接入登录链路（密码登录、短信登录、微信登录）与注册端点，拦截未注册身份与 IP
4. 冻结时同时强制下线该账号已签发的所有 JWT
5. 错误码、i18n、单元测试齐备，符合 `12-security.md` / `17-tenant-permission.md` / `09-testing.md` / `18-openapi.md` 规范

### 1.3 非目标

- **不实现自动冻结**（登录失败次数累计 → 自动冻结账号）。当前阶段冻结仅由管理员显式触发
- **不实现 Flyway 迁移脚本**。当前为开发阶段，dev profile 走 `ddl-auto=update` 自动同步；prod 启用 Flyway 时再补 V202605xxxx 脚本
- **不实现全局（跨租户）IP 黑名单**。当前黑名单全部为租户级；如未来需要全局 IP 黑名单，按"默认租户 + 新增 GLOBAL_IP 类型"扩展
- **不实现风控规则引擎 / Compliance 独立模块**

---

## 2. 领域模型

### 2.1 Account 聚合升级（auth 域，全局，不带租户）

```
auth/domain/model/Account.java
  字段变更：
    - Boolean locked            ❌ 移除（采用过渡期，先标 @Deprecated 双写一周）
    + AccountStatus status      ✅ 新增（枚举 ACTIVE / FROZEN，@Enumerated STRING，nullable=false，default ACTIVE）
    + AccountFreeze freeze      ✅ 新增（@Embedded 值对象，status=ACTIVE 时所有字段为 null）

auth/domain/model/enums/AccountStatus.java
  ACTIVE, FROZEN

auth/domain/model/enums/FreezeReason.java
  ADMIN,            // 管理员手动
  RISK_CONTROL,     // 风控触发（预留，当前未使用）
  OTHER             // 兼容旧 lock 迁移

auth/domain/model/valueobject/AccountFreeze.java   @Embeddable
  FreezeReason reason          @Enumerated(STRING)
  LocalDateTime freezeUntil    null = 永久
  Long operatorId              冻结操作人（必填，管理员 ID）
  String operatorName          冻结操作人姓名（必填）
  String remark                length 255，可选
  LocalDateTime frozenAt       冻结发生时间
```

**业务方法（聚合根）：**

| 方法 | 入参 | 校验 / 行为 | 注册事件 |
|------|------|-------------|----------|
| `freezeByAdmin(operatorId, operatorName, reason, until, remark)` | 操作人、原因、到期时间（null=永久）、备注 | 当前必须 ACTIVE；`until` 非 null 时必须 > now | `AccountFrozenEvent` |
| `unfreeze(operatorId, operatorName)` | 操作人 | 当前必须 FROZEN | `AccountUnfrozenEvent(source=ADMIN)` |
| `tryAutoUnfreezeIfExpired()` | — | 登录路径懒触发，`freezeUntil < now()` 时切回 ACTIVE | `AccountUnfrozenEvent(source=AUTO)` |

**旧 `lock() / unlock()` 标 `@Deprecated`**：内部委托 `freezeByAdmin(systemOperator, FreezeReason.OTHER, null, "legacy lock API")` 与 `unfreeze`，过渡期一周后删除。

### 2.2 Blacklist 聚合根（auth 域，**租户级**）

```
auth/domain/model/Blacklist.java
  @Entity
  @Table(name = "auth_blacklist", indexes = {
    @Index(name="uk_blacklist_tenant_type_value", columnList="tenant_id,type,value", unique=true),
    @Index(name="idx_blacklist_tenant_expires", columnList="tenant_id,expires_at")
  })
  @FilterDef(name="tenantFilter", parameters=@ParamDef(name="tenantId", type=String.class))
  @Filter(name="tenantFilter", condition="tenant_id = :tenantId")
  implements TenantAware

  Long id
  String tenantId             nullable=false, updatable=false, length=64
  BlacklistType type          @Enumerated(STRING), nullable=false
  String value                length 128, nullable=false
  String reason               length 255
  LocalDateTime expiresAt     null = 永久
  Long operatorId             null = 系统自动
  String operatorName
  + 审计 + version（继承 BaseAggregateRoot）

  @PrePersist
  void fillTenant() {
      if (tenantId == null) tenantId = TenantContextHolder.getTenantId();
  }

auth/domain/model/enums/BlacklistType.java
  ACCOUNT_ID, PHONE, EMAIL, IP, OPENID
```

**工厂方法**：`Blacklist.create(type, value, reason, expiresAt, operatorId, operatorName)`，`@PostPersist` 注册 `BlacklistAddedEvent`；删除前调 `publishRemovedEvent()` 注册 `BlacklistRemovedEvent`。

**多租户行为**：
- 不同租户允许同一 `(type, value)`（唯一约束包含 `tenant_id`）
- 黑名单命中判断永远在当前租户上下文内
- 登录 / 注册前：`tenantId` 来自 `X-Tenant-Id` header（由 `eagle-tenant-starter` 的 `TenantIdFilter` 写入 `TenantContextHolder`）；未带 header 时回落 `default-tenant-id`（默认 `"0"`）
- 已认证后的访问：`tenantId` 来自 JWT claim
- IP 黑名单也租户级（同一 IP 在租户 A 黑、租户 B 仍可访问）

### 2.3 领域事件

包路径：`auth/domain/event/`（已有 `@NamedInterface("event")`）

| 事件 | 触发 | 主要订阅 |
|------|------|----------|
| `AccountFrozenEvent(accountId, username, reason, freezeUntil, operatorId)` | freeze 后 | (a) 强制下线该账号所有 JTI；(b) `evictUserCache(username)`；(c) `t_audit_log`（已有 audit-log starter） |
| `AccountUnfrozenEvent(accountId, username, source=ADMIN/AUTO, operatorId)` | unfreeze 后 | 缓存失效、审计 |
| `BlacklistAddedEvent(id, tenantId, type, value, expiresAt)` | 新增黑名单后 | Redis Set 追加；若 `type=ACCOUNT_ID` 附带强制下线 |
| `BlacklistRemovedEvent(id, tenantId, type, value)` | 删除黑名单后 | Redis Set 移除 |

---

## 3. 数据库

**当前为开发阶段，dev profile 通过 JPA `ddl-auto=update` 自动同步表结构。** 字段定义与索引以本节为准；prod 启用 Flyway 时再产出迁移脚本。

### 3.1 `auth_account` 变更

新增列（与 `AccountFreeze` 值对象映射）：

| 列名 | 类型 | 约束 | 备注 |
|------|------|------|------|
| `status` | VARCHAR(20) | NOT NULL DEFAULT 'ACTIVE' | 账号状态 |
| `freeze_reason` | VARCHAR(20) | NULL | 枚举 FreezeReason |
| `freeze_until` | TIMESTAMP | NULL | null = 永久 |
| `frozen_by` | BIGINT | NULL | 操作人 ID |
| `frozen_by_name` | VARCHAR(64) | NULL | 操作人姓名 |
| `freeze_remark` | VARCHAR(255) | NULL | 备注 |
| `frozen_at` | TIMESTAMP | NULL | 冻结时间 |

**`locked` 列保留** 至下次发布前的过渡期（一周），双写以兼容旧客户端调用 `/accounts/{id}/lock|unlock`。

### 3.2 `auth_blacklist` 新表

| 列 | 类型 | 约束 |
|------|------|------|
| `id` | BIGINT | PK AUTO_INCREMENT |
| `tenant_id` | VARCHAR(64) | NOT NULL, updatable=false |
| `type` | VARCHAR(20) | NOT NULL |
| `value` | VARCHAR(128) | NOT NULL |
| `reason` | VARCHAR(255) | NULL |
| `expires_at` | TIMESTAMP | NULL |
| `operator_id` | BIGINT | NULL |
| `operator_name` | VARCHAR(64) | NULL |
| `version` | INT | NOT NULL DEFAULT 0 |
| 审计 4 字段 + `deleted` TINYINT | | — |

**索引：**
- `uk_blacklist_tenant_type_value(tenant_id, type, value)` 唯一
- `idx_blacklist_tenant_expires(tenant_id, expires_at)`

---

## 4. 应用服务接口

### 4.1 `AccountApplicationService`（已有，增补 / 替换）

```java
// 新增（替换旧 lockAccount / unlockAccount）
void freezeAccount(Long accountId, FreezeAccountCommand cmd);
void unfreezeAccount(Long accountId, Long operatorId, String operatorName);

// 旧方法保留，标 @Deprecated 内部委托
@Deprecated public void lockAccount(Long accountId) { ... }
@Deprecated public void unlockAccount(Long accountId) { ... }
```

`FreezeAccountCommand`（application/command/）：
- `FreezeReason reason`（默认 ADMIN）
- `LocalDateTime freezeUntil`（null = 永久）
- `String remark`（可选）
- `Long operatorId / String operatorName`（从 SecurityContext 提取，由 Controller 填充）

### 4.2 `BlacklistApplicationService`（新增）

```java
@TenantFilter
Page<BlacklistResponse> queryBlacklist(BlacklistQuery query, Pageable pageable);

BlacklistResponse addToBlacklist(AddBlacklistCommand cmd);   // 内部 setTenantId by @PrePersist
void removeFromBlacklist(Long id);
boolean isBlacklisted(BlacklistType type, String value);     // 优先 Redis，降级 DB
```

`BlacklistChecker`（auth/infrastructure/security）封装多类型批量判断，不在 ApplicationService 中。

---

## 5. Controller / OpenAPI

### 5.1 `AccountController`（已有，调整）

| HTTP | 路径 | 权限 | 说明 |
|------|------|------|------|
| PATCH | `/accounts/{accountId}/freeze` | `hasRole('admin')` | body: `FreezeAccountRequest` |
| PATCH | `/accounts/{accountId}/unfreeze` | `hasRole('admin')` | — |
| PATCH | `/accounts/{accountId}/lock` | `hasRole('admin')` | **@Deprecated**，内部转发 freeze |
| PATCH | `/accounts/{accountId}/unlock` | `hasRole('admin')` | **@Deprecated**，内部转发 unfreeze |
| GET | `/accounts/{accountId}` | `hasRole('admin')` | 响应 DTO 增加 `status`、`freezeUntil`、`freezeReason`、`frozenByName` |

### 5.2 `BlacklistController`（新增）

| HTTP | 路径 | 权限 |
|------|------|------|
| GET | `/admin/blacklist?type=&value=&page&size&sort` | `hasRole('admin')` |
| POST | `/admin/blacklist` body: `AddBlacklistRequest` | `hasRole('admin')` |
| DELETE | `/admin/blacklist/{id}` | `hasRole('admin')` |

所有接口含 `@Tag / @Operation / @Schema`（按 `18-openapi.md`），分页参数遵循 `@ParameterObject + @PageableDefault`。

`AddBlacklistRequest`：`type`（必填 enum）/ `value`（必填，校验最大长度）/ `reason`（可选）/ `expiresAt`（可选，必须 > now）。

`FreezeAccountRequest`：`reason`（必填）/ `freezeUntil`（可选）/ `remark`（可选）。

---

## 6. 登录链路改造

### 6.1 `BlacklistChecker`（auth/infrastructure/security 新增）

```java
@Component
@RequiredArgsConstructor
public class BlacklistChecker {
    private final BlacklistApplicationService blacklist;

    public void checkLogin(String username, String phone, String ip, Long accountId) {
        if (ip != null && blacklist.isBlacklisted(BlacklistType.IP, ip))
            throw AuthErrorCode.IP_BLACKLISTED.toServiceException();
        if (phone != null && blacklist.isBlacklisted(BlacklistType.PHONE, phone))
            throw AuthErrorCode.IDENTITY_BLACKLISTED.toServiceException();
        if (accountId != null && blacklist.isBlacklisted(BlacklistType.ACCOUNT_ID, String.valueOf(accountId)))
            throw AuthErrorCode.IDENTITY_BLACKLISTED.toServiceException();
    }
    public void checkRegister(String phone, String email, String ip) { /* PHONE / EMAIL / IP */ }
    public void checkWechat(String openid, String ip) { /* OPENID / IP */ }
}
```

所有判断走 Redis（命中 `SISMEMBER`），调用都发生在 `TenantContextHolder` 已被 `TenantIdFilter` 填充之后。

### 6.2 接入点

| 位置 | 接入方式 |
|------|----------|
| `EagleUserDetailsService.loadUserByUsername` | (1) 加载 Account 前先调 `checkLogin(null, null, ip, null)` 拦 IP；(2) 加载到 Account 后再调 `checkLogin(null, account.phone, ip, account.id)` 拦 PHONE / ACCOUNT_ID |
| `SmsCodeAuthenticationProvider` | authenticate 前调 `checkLogin(null, phone, ip, null)`；查到 Account 后再调一次带 `accountId` |
| `WechatMiniProgramAuthenticationProvider` / `WechatAppAuthenticationProvider` | authenticate 前调 `checkWechat(openid, ip)` |
| `AccountController.register`（`/accounts/register`） | 入口前调 `checkRegister(phone, email, ip)` |

### 6.3 冻结后强制下线（同意采用此策略）

```java
// auth/infrastructure/event/AccountSecurityEventHandler.java
@Async("taskExecutor")
@TransactionalEventListener(phase = AFTER_COMMIT)
@Transactional(propagation = REQUIRES_NEW)
public void onAccountFrozen(AccountFrozenEvent event) {
    List<String> jtis = onlineUserPort.listJtisByAccount(event.accountId());
    jtis.forEach(onlineUserPort::forceLogout);
    log.info("frozen account force-logout: accountId={}, jtiCount={}", event.accountId(), jtis.size());
}
```

为支撑反查，`OnlineUserPort` 增加：

```java
List<String> listJtisByAccount(Long accountId);
```

`OnlineUserAdapter`（Redis 实现）维护一个反向索引 Set：

```
account:online:{accountId} → Set<jti>
```

`trackLogin(info)` 时 `SADD account:online:{accountId} jti` 并设置 TTL 与主记录一致；`forceLogout(jti)` 时同步 SREM。

---

## 7. 缓存策略

### 7.1 Blacklist Redis Key 设计

**租户级**，与现有 `token:blacklist:` 完全分离：

```
auth:blacklist:{tenantId}:ACCOUNT_ID   Set<value>
auth:blacklist:{tenantId}:PHONE        Set<value>
auth:blacklist:{tenantId}:EMAIL        Set<value>
auth:blacklist:{tenantId}:IP           Set<value>
auth:blacklist:{tenantId}:OPENID       Set<value>
```

### 7.2 加载与失效

| 时机 | 行为 |
|------|------|
| 应用启动 `@PostConstruct`（`BlacklistCacheWarmer`） | 全量扫描 DB（关闭 tenant filter 直接 native 查询），按 tenant + type 分桶 `SADD` |
| `BlacklistAddedEvent` AFTER_COMMIT | 单条 `SADD` |
| `BlacklistRemovedEvent` AFTER_COMMIT | 单条 `SREM` |
| 过期清理 | 暂不实现定时任务；`isBlacklisted` 时先查 Redis 命中，再回 DB 兜底校验 `expires_at`（懒过期） |
| Redis 异常 | 故障降级：直接查 DB（不缓存 null） |

**懒过期实现细节**：`SISMEMBER` 命中后调 `BlacklistRepository.findByTenantTypeValue(...)` 校验 `expires_at`；若已过期则视为未命中并触发 `removeFromBlacklist`（异步事件清理 Redis）。

> 后续若黑名单条目超过 10K 或过期清理压力大，再引入 `@XxlJob` 定时清理。

### 7.3 冻结状态缓存

不单独缓存。`AccountFrozenEvent / AccountUnfrozenEvent` 直接触发现有 `evictUserCache(username)`（base 域 `UserEventHandler` 已有机制）。

---

## 8. 错误码 & i18n

`AuthErrorCode` 现有占用 11001–11037，新增从 **11038** 起：

| 常量 | code | i18n key | zh_CN |
|------|------|----------|-------|
| `ACCOUNT_FROZEN` | 11038 | `error.account.frozen` | 账号已被冻结：{0} |
| `ACCOUNT_NOT_FROZEN` | 11039 | `error.account.not_frozen` | 账号未被冻结 |
| `ACCOUNT_FREEZE_UNTIL_INVALID` | 11040 | `error.account.freeze_until_invalid` | 冻结到期时间必须晚于当前时间 |
| `IDENTITY_BLACKLISTED` | 11041 | `error.auth.identity_blacklisted` | 该身份已被禁止访问 |
| `IP_BLACKLISTED` | 11042 | `error.auth.ip_blacklisted` | 当前 IP 已被禁止访问 |
| `BLACKLIST_DUPLICATE` | 11043 | `error.blacklist.duplicate` | 该黑名单条目已存在 |
| `BLACKLIST_NOT_FOUND` | 11044 | `error.blacklist.not_found` | 黑名单条目不存在 |

`ACCOUNT_LOCKED(11024)` / `ACCOUNT_NOT_LOCKED(11025)` 保留并标 `@Deprecated`，过渡期内向 frozen 转换错误消息。

i18n 同步更新 `messages_zh_CN.properties` + `messages_en.properties`。AuthErrorCode 类顶部注释更新区间为 11001–11044。

---

## 9. 测试策略（仅单元测试，按 `09-testing.md`）

| 测试类 | 覆盖路径 |
|--------|----------|
| `AccountFreezeTest` | freeze 正常 / 重复冻结抛 `ACCOUNT_FROZEN` / unfreeze 正常 / 未冻结解冻抛 `ACCOUNT_NOT_FROZEN` / `freezeUntil` <= now 抛 `ACCOUNT_FREEZE_UNTIL_INVALID` / `tryAutoUnfreezeIfExpired` 切换状态 + 注册事件 |
| `BlacklistTest` | 工厂方法校验 / 过期判断 / 事件注册 |
| `AccountApplicationServiceTest`（增补） | freezeAccount / unfreezeAccount happy + 异常路径，事件发布；旧 lockAccount 委托验证 |
| `BlacklistApplicationServiceTest` | 添加 / 删除 / 查询 / 重复唯一冲突 / Redis 缓存交互（Mock） |
| `BlacklistCheckerTest` | 各类型命中抛 `IDENTITY_BLACKLISTED` / `IP_BLACKLISTED`，未命中放行 |
| `OnlineUserAdapterTest`（增补） | `trackLogin` 写反向索引 + `listJtisByAccount` 返回 + `forceLogout` 清理反向索引 |
| `AccountSecurityEventHandlerTest` | `AccountFrozenEvent` 触发批量 forceLogout |

集成测试（`EagleSystemApplicationTests`）保持 `@Disabled`，不依赖基础设施。

---

## 10. 风险与回滚

| 风险 | 缓解 |
|------|------|
| Redis 与 DB 不一致 | 启动全量加载 + 事件失效 + 故障降级查 DB + 懒过期 |
| 黑名单数据量增长（> 10K） | 监控；超阈值再引入分页加载或定时清理任务 |
| 误冻结超级管理员 | `AccountApplicationService.freezeAccount` 增加目标角色非 `super_admin` 的校验（通过 `AuthorizationPort`） |
| `locked` 列移除破坏旧客户端 | 一周过渡期内 status 与 locked 双写，`/lock|unlock` 端点保留转发到 freeze/unfreeze |
| 多租户上下文缺失（登录前） | `TenantIdFilter` 早于 SecurityFilter 注入；未带 header 时回落 default tenant（`"0"`） |
| 强制下线影响合法已登录会话 | 仅在 `AccountFrozenEvent` 触发；正常解冻不重发签发 token，用户需重新登录 |

**回滚步骤**：
- 应用层：禁用 `BlacklistChecker` 注入（@ConditionalOnProperty `eagle.auth.blacklist.enabled: false`），删除 freeze 端点，恢复 lockAccount/unlockAccount 直接调用 Account.lock/unlock
- DB 层：保留新列与新表，不做 drop（数据无损）

---

## 11. 配置项

```yaml
eagle:
  auth:
    blacklist:
      enabled: true                   # 全局开关，默认 true
      cache-warm-on-startup: true     # 启动时全量加载，默认 true
```

无新增"自动冻结"相关配置（本期不实现）。

---

## 12. 实现切片（供 writing-plans 参考）

1. **Slice 1**：Account 冻结模型（`AccountStatus / FreezeReason / AccountFreeze` + Account 业务方法 + 事件） + 错误码 + 旧 lock 委托 + `AccountApplicationService` 增补 + `AccountController` freeze/unfreeze + 单测
2. **Slice 2**：Blacklist 多租户聚合根 + Repository + `BlacklistApplicationService` + `BlacklistController` + 错误码 + Redis Set 缓存 + `BlacklistCacheWarmer` + 单测
3. **Slice 3**：`BlacklistChecker` 接入各 AuthenticationProvider + 注册端点 + 集成位点单测
4. **Slice 4**：`OnlineUserPort.listJtisByAccount` + `OnlineUserAdapter` 反向索引 Set + `AccountSecurityEventHandler` 强制下线 + 单测
5. **Slice 5**：清理 base 域遗留 `UserLockedEvent` + `UserEventHandler.handleUserLocked` + 文档同步

---

## 13. 待评审点（写入 spec 前已与用户确认）

- ✅ 方案选择：A（扩展 Account 冻结 + 新建 Blacklist 聚合）
- ✅ Blacklist 改为租户级
- ✅ 当前阶段不实现自动冻结
- ✅ 当前阶段不写 Flyway 脚本（dev `ddl-auto=update`）
- ✅ 冻结时强制下线已签发 token（默认采用，与本期配套实现）
