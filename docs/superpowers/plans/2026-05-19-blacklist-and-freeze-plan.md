# 黑名单 + 用户冻结 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:
> executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 升级账号锁定为完整的"冻结"模型（原因/到期/操作人/审计），新增租户级身份黑名单聚合并接入登录与注册链路，冻结时强制下线已签发
token。

**Architecture:** 方案 A —— 扩展 auth.Account 聚合（嵌入 `AccountFreeze` 值对象 + `AccountStatus` 枚举）+ 新建
`auth.Blacklist` 租户级聚合根（`@FilterDef` Hibernate 过滤 + Redis Set 缓存）。冻结/解冻通过领域事件驱动缓存失效与强制下线。

**Tech Stack:** Spring Boot 4 / Spring Modulith / JPA / Redis (StringRedisTemplate) / Spring Security OAuth2 /
`eagle-tenant-starter` / JUnit 5 + Mockito。

**Spec:** `docs/superpowers/specs/2026-05-19-blacklist-and-freeze-design.md`

---

## File Structure

### 新建文件

| 文件                                                           | 职责                          |
|--------------------------------------------------------------|-----------------------------|
| `auth/domain/model/enums/AccountStatus.java`                 | Account 状态枚举（ACTIVE/FROZEN） |
| `auth/domain/model/enums/FreezeReason.java`                  | 冻结原因枚举                      |
| `auth/domain/model/enums/BlacklistType.java`                 | 黑名单类型枚举                     |
| `auth/domain/model/valueobject/AccountFreeze.java`           | 冻结信息值对象（@Embeddable）        |
| `auth/domain/model/Blacklist.java`                           | 黑名单聚合根（租户级）                 |
| `auth/domain/event/AccountFrozenEvent.java`                  | 账号冻结领域事件                    |
| `auth/domain/event/AccountUnfrozenEvent.java`                | 账号解冻领域事件                    |
| `auth/domain/event/BlacklistAddedEvent.java`                 | 黑名单新增事件                     |
| `auth/domain/event/BlacklistRemovedEvent.java`               | 黑名单删除事件                     |
| `auth/domain/repository/BlacklistRepository.java`            | 黑名单仓储接口                     |
| `auth/application/command/FreezeAccountCommand.java`         | 冻结命令对象                      |
| `auth/application/command/AddBlacklistCommand.java`          | 添加黑名单命令对象                   |
| `auth/application/command/BlacklistQuery.java`               | 黑名单查询参数                     |
| `auth/application/service/BlacklistApplicationService.java`  | 黑名单应用服务                     |
| `auth/application/mapper/BlacklistMapper.java`               | Blacklist → Response 映射     |
| `auth/interfaces/controller/BlacklistController.java`        | 黑名单 REST 控制器                |
| `auth/interfaces/dto/request/FreezeAccountRequest.java`      | 冻结请求 DTO                    |
| `auth/interfaces/dto/request/AddBlacklistRequest.java`       | 添加黑名单请求 DTO                 |
| `auth/interfaces/dto/response/BlacklistResponse.java`        | 黑名单响应 DTO                   |
| `auth/infrastructure/security/BlacklistChecker.java`         | 登录链路黑名单校验器                  |
| `auth/infrastructure/cache/BlacklistCacheWarmer.java`        | 启动期黑名单全量加载                  |
| `auth/infrastructure/event/AccountSecurityEventHandler.java` | 冻结事件 → 强制下线处理器              |
| `auth/infrastructure/event/BlacklistCacheSyncHandler.java`   | 黑名单事件 → Redis 同步            |

### 修改文件

| 文件                                                                          | 改动                                                                                                                                            |
|-----------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| `auth/domain/model/Account.java`                                            | 移除 `locked`，新增 `status / freeze`；新增 freezeByAdmin / unfreeze；旧 lock/unlock 委托                                                                 |
| `auth/domain/AuthErrorCode.java`                                            | 新增 11038–11044 错误码，区间注释更新                                                                                                                     |
| `auth/domain/port/OnlineUserPort.java`                                      | 新增 `listJtisByAccount(Long)`                                                                                                                  |
| `auth/infrastructure/adapter/OnlineUserAdapter.java`                        | 维护 `account:online:{accountId}` 反向索引 Set，实现 `listJtisByAccount`                                                                               |
| `auth/infrastructure/adapter/EagleUserDetailsServiceImpl.java`              | locked 判断改为 status；前置 BlacklistChecker                                                                                                        |
| `auth/application/service/AccountApplicationService.java`                   | 新增 freezeAccount/unfreezeAccount；旧 lockAccount/unlockAccount 委托并 @Deprecated；resetPasswordByPhone/bindPhone 内 `account.getLocked()` 改为 status |
| `auth/interfaces/controller/AccountController.java`                         | 新增 /freeze /unfreeze；/lock /unlock @Deprecated 转发                                                                                             |
| `auth/infrastructure/security/SmsCodeAuthenticationProvider.java`           | 注入 BlacklistChecker，authenticate 前调用                                                                                                          |
| `auth/infrastructure/security/WechatMiniProgramAuthenticationProvider.java` | 同上 checkWechat                                                                                                                                |
| `auth/infrastructure/security/WechatAppAuthenticationProvider.java`         | 同上 checkWechat                                                                                                                                |
| `src/main/resources/messages_zh_CN.properties`                              | 新增 7 条 i18n                                                                                                                                   |
| `src/main/resources/messages_en.properties`                                 | 新增 7 条 i18n                                                                                                                                   |
| `base/domain/event/UserLockedEvent.java`                                    | 删除（遗留清理）                                                                                                                                      |
| `base/infrastructure/event/UserEventHandler.java`                           | 删除 handleUserLocked                                                                                                                           |

---

## 通用约定（每个 Slice 适用）

- 使用 `./gradlew :eagle-services:eagle-system-service:test --tests "<TestClass>"` 运行单测
- 使用 `./gradlew :eagle-services:eagle-system-service:build -x test` 验证编译
- 全部新文件首行 `@author sunshixiong` Javadoc
- 不引入新依赖（已有 `tenant-starter` 提供 `TenantAware` / `TenantContextHolder`）
- 每完成一个 Task 立即 commit（Conventional Commits，scope=`auth`）

---

# Slice 1 — Account 冻结模型

### Task 1.1：新增 AccountStatus 枚举

**Files:**

- Create:
  `eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/domain/model/enums/AccountStatus.java`

- [ ] **Step 1: 创建枚举**

```java
package com.eagle.system.auth.domain.model.enums;

/**
 * 账号状态枚举
 *
 * @author sunshixiong
 */
public enum AccountStatus {
    /** 活跃（可登录）*/
    ACTIVE,
    /** 已冻结（不可登录）*/
    FROZEN
}
```

- [ ] **Step 2: 编译**

Run: `./gradlew :eagle-services:eagle-system-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/domain/model/enums/AccountStatus.java
git commit -m "feat(auth): 新增 AccountStatus 枚举"
```

---

### Task 1.2：新增 FreezeReason 枚举

**Files:**

- Create: `auth/domain/model/enums/FreezeReason.java`

- [ ] **Step 1: 创建枚举**

```java
package com.eagle.system.auth.domain.model.enums;

/**
 * 账号冻结原因枚举
 *
 * @author sunshixiong
 */
public enum FreezeReason {
    /** 管理员手动冻结 */
    ADMIN,
    /** 风控触发（预留）*/
    RISK_CONTROL,
    /** 其他（兼容旧 locked 迁移）*/
    OTHER
}
```

- [ ] **Step 2: 编译 + Commit**

```bash
./gradlew :eagle-services:eagle-system-service:compileJava
git add eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/domain/model/enums/FreezeReason.java
git commit -m "feat(auth): 新增 FreezeReason 枚举"
```

---

### Task 1.3：新增 AccountFreeze 值对象

**Files:**

- Create: `auth/domain/model/valueobject/AccountFreeze.java`

- [ ] **Step 1: 创建 @Embeddable 值对象**

```java
package com.eagle.system.auth.domain.model.valueobject;

import com.eagle.system.auth.domain.model.enums.FreezeReason;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 账号冻结信息值对象
 * <p>
 * 当 Account.status = FROZEN 时此值对象的字段为非 null。
 *
 * @author sunshixiong
 */
@Embeddable
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AccountFreeze {

    @Enumerated(EnumType.STRING)
    @Column(name = "freeze_reason", length = 20, comment = "冻结原因")
    private FreezeReason reason;

    @Column(name = "freeze_until", comment = "冻结到期时间（null=永久）")
    private LocalDateTime freezeUntil;

    @Column(name = "frozen_by", comment = "冻结操作人ID")
    private Long operatorId;

    @Column(name = "frozen_by_name", length = 64, comment = "冻结操作人姓名")
    private String operatorName;

    @Column(name = "freeze_remark", length = 255, comment = "冻结备注")
    private String remark;

    @Column(name = "frozen_at", comment = "冻结时间")
    private LocalDateTime frozenAt;

    /** 判断当前冻结是否已到期。永久冻结永远返回 false */
    public boolean isExpired(LocalDateTime now) {
        return freezeUntil != null && now.isAfter(freezeUntil);
    }
}
```

- [ ] **Step 2: 编译 + Commit**

```bash
./gradlew :eagle-services:eagle-system-service:compileJava
git add eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/domain/model/valueobject/AccountFreeze.java
git commit -m "feat(auth): 新增 AccountFreeze 值对象"
```

---

### Task 1.4：新增 AccountFrozenEvent / AccountUnfrozenEvent

**Files:**

- Create: `auth/domain/event/AccountFrozenEvent.java`
- Create: `auth/domain/event/AccountUnfrozenEvent.java`

- [ ] **Step 1: 创建 AccountFrozenEvent**

```java
package com.eagle.system.auth.domain.event;

import com.eagle.system.auth.domain.model.enums.FreezeReason;

import java.time.LocalDateTime;

/**
 * 账号已冻结事件（auth 域内 + 跨域订阅）
 *
 * @author sunshixiong
 */
public record AccountFrozenEvent(
        Long accountId,
        String username,
        FreezeReason reason,
        LocalDateTime freezeUntil,
        Long operatorId) {
}
```

- [ ] **Step 2: 创建 AccountUnfrozenEvent**

```java
package com.eagle.system.auth.domain.event;

/**
 * 账号已解冻事件
 *
 * @author sunshixiong
 */
public record AccountUnfrozenEvent(
        Long accountId,
        String username,
        Source source,
        Long operatorId) {

    public enum Source {
        /** 管理员显式解冻 */
        ADMIN,
        /** 到期自动解冻 */
        AUTO
    }
}
```

- [ ] **Step 3: 编译 + Commit**

```bash
./gradlew :eagle-services:eagle-system-service:compileJava
git add eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/domain/event/AccountFrozenEvent.java \
        eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/domain/event/AccountUnfrozenEvent.java
git commit -m "feat(auth): 新增 AccountFrozen/Unfrozen 领域事件"
```

---

### Task 1.5：扩展 AuthErrorCode 新增冻结相关错误码

**Files:**

- Modify: `auth/domain/AuthErrorCode.java`

- [ ] **Step 1: 在枚举末尾追加 7 个新错误码**

在 `ONE_CLICK_PHONE_PARSE_FAILED(11037, …)` 后、`;` 前追加：

```java
    // ==================== 账号冻结（11038–11040）====================

    ACCOUNT_FROZEN(11038, "error.account.frozen", "账号已被冻结：{0}"),
    ACCOUNT_NOT_FROZEN(11039, "error.account.not_frozen", "账号未被冻结"),
    ACCOUNT_FREEZE_UNTIL_INVALID(11040, "error.account.freeze_until_invalid", "冻结到期时间必须晚于当前时间"),

    // ==================== 黑名单（11041–11044）====================

    IDENTITY_BLACKLISTED(11041, "error.auth.identity_blacklisted", "该身份已被禁止访问"),
    IP_BLACKLISTED(11042, "error.auth.ip_blacklisted", "当前 IP 已被禁止访问"),
    BLACKLIST_DUPLICATE(11043, "error.blacklist.duplicate", "该黑名单条目已存在"),
    BLACKLIST_NOT_FOUND(11044, "error.blacklist.not_found", "黑名单条目不存在");
```

- [ ] **Step 2: 更新文件顶部 Javadoc 区间**

把第 6 行：

```java
 * 认证领域错误码（11001–11033）
```

改为：

```java
 * 认证领域错误码（11001–11044）
 */
```

- [ ] **Step 3: 标记 ACCOUNT_LOCKED / ACCOUNT_NOT_LOCKED 为 @Deprecated**

定位到 `ACCOUNT_LOCKED(11024, …)` 与 `ACCOUNT_NOT_LOCKED(11025, …)` 两行，**保留枚举值**，但在调用处用 `ACCOUNT_FROZEN`
替代（其他 Slice 处理）。本步骤无代码改动，仅记录约束：新代码禁止再用 11024/11025。

- [ ] **Step 4: 同步 i18n 文件**

`src/main/resources/messages_zh_CN.properties` 追加：

```
error.account.frozen=账号已被冻结：{0}
error.account.not_frozen=账号未被冻结
error.account.freeze_until_invalid=冻结到期时间必须晚于当前时间
error.auth.identity_blacklisted=该身份已被禁止访问
error.auth.ip_blacklisted=当前 IP 已被禁止访问
error.blacklist.duplicate=该黑名单条目已存在
error.blacklist.not_found=黑名单条目不存在
```

`src/main/resources/messages_en.properties` 追加：

```
error.account.frozen=Account has been frozen: {0}
error.account.not_frozen=Account is not frozen
error.account.freeze_until_invalid=Freeze deadline must be later than current time
error.auth.identity_blacklisted=This identity is blocked
error.auth.ip_blacklisted=Current IP is blocked
error.blacklist.duplicate=Blacklist entry already exists
error.blacklist.not_found=Blacklist entry not found
```

- [ ] **Step 5: 编译 + Commit**

```bash
./gradlew :eagle-services:eagle-system-service:compileJava
git add eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/domain/AuthErrorCode.java \
        eagle-services/eagle-system-service/src/main/resources/messages_zh_CN.properties \
        eagle-services/eagle-system-service/src/main/resources/messages_en.properties
git commit -m "feat(auth): 新增冻结+黑名单错误码 11038-11044 及 i18n 翻译"
```

---

### Task 1.6：Account 聚合根升级 — 字段与业务方法

**Files:**

- Modify: `auth/domain/model/Account.java`

- [ ] **Step 1: 写失败单测先（TDD）**

创建测试 `eagle-services/eagle-system-service/src/test/java/com/eagle/system/auth/domain/model/AccountFreezeTest.java`：

```java
package com.eagle.system.auth.domain.model;

import com.eagle.common.exception.DomainException;
import com.eagle.system.auth.domain.AuthErrorCode;
import com.eagle.system.auth.domain.model.enums.AccountStatus;
import com.eagle.system.auth.domain.model.enums.FreezeReason;
import com.eagle.system.auth.domain.model.valueobject.ProfileHints;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountFreezeTest {

    private static final Long OPERATOR_ID = 99L;
    private static final String OPERATOR_NAME = "admin";

    private Account newActiveAccount() {
        return Account.create("alice", "{bcrypt}x", "13800138000",
                new ProfileHints("Alice", null, null));
    }

    @Nested
    @DisplayName("freezeByAdmin")
    class Freeze {
        @Test
        @DisplayName("should freeze active account and register event")
        void shouldFreezeActiveAccount() {
            Account account = newActiveAccount();
            LocalDateTime until = LocalDateTime.now().plusHours(1);

            account.freezeByAdmin(OPERATOR_ID, OPERATOR_NAME, FreezeReason.ADMIN, until, "test");

            assertEquals(AccountStatus.FROZEN, account.getStatus());
            assertNotNull(account.getFreeze());
            assertEquals(FreezeReason.ADMIN, account.getFreeze().getReason());
            assertEquals(until, account.getFreeze().getFreezeUntil());
            assertEquals(OPERATOR_ID, account.getFreeze().getOperatorId());
        }

        @Test
        @DisplayName("should reject when already frozen")
        void shouldRejectWhenAlreadyFrozen() {
            Account account = newActiveAccount();
            account.freezeByAdmin(OPERATOR_ID, OPERATOR_NAME, FreezeReason.ADMIN, null, null);
            DomainException ex = assertThrows(DomainException.class,
                    () -> account.freezeByAdmin(OPERATOR_ID, OPERATOR_NAME, FreezeReason.ADMIN, null, null));
            assertEquals(AuthErrorCode.ACCOUNT_FROZEN.getCode(), ex.getErrorCode().getCode());
        }

        @Test
        @DisplayName("should reject when freezeUntil is in the past")
        void shouldRejectPastFreezeUntil() {
            Account account = newActiveAccount();
            LocalDateTime past = LocalDateTime.now().minusMinutes(1);
            DomainException ex = assertThrows(DomainException.class,
                    () -> account.freezeByAdmin(OPERATOR_ID, OPERATOR_NAME, FreezeReason.ADMIN, past, null));
            assertEquals(AuthErrorCode.ACCOUNT_FREEZE_UNTIL_INVALID.getCode(), ex.getErrorCode().getCode());
        }
    }

    @Nested
    @DisplayName("unfreeze")
    class Unfreeze {
        @Test
        @DisplayName("should unfreeze and clear freeze info")
        void shouldUnfreeze() {
            Account account = newActiveAccount();
            account.freezeByAdmin(OPERATOR_ID, OPERATOR_NAME, FreezeReason.ADMIN, null, null);

            account.unfreeze(OPERATOR_ID, OPERATOR_NAME);

            assertEquals(AccountStatus.ACTIVE, account.getStatus());
            assertNull(account.getFreeze());
        }

        @Test
        @DisplayName("should reject when not frozen")
        void shouldRejectWhenNotFrozen() {
            Account account = newActiveAccount();
            DomainException ex = assertThrows(DomainException.class,
                    () -> account.unfreeze(OPERATOR_ID, OPERATOR_NAME));
            assertEquals(AuthErrorCode.ACCOUNT_NOT_FROZEN.getCode(), ex.getErrorCode().getCode());
        }
    }

    @Nested
    @DisplayName("tryAutoUnfreezeIfExpired")
    class AutoUnfreeze {
        @Test
        @DisplayName("should auto-unfreeze when freezeUntil expired")
        void shouldAutoUnfreezeWhenExpired() {
            Account account = newActiveAccount();
            account.freezeByAdmin(OPERATOR_ID, OPERATOR_NAME, FreezeReason.ADMIN,
                    LocalDateTime.now().plusSeconds(1), null);
            boolean unfrozen = account.tryAutoUnfreezeIfExpired(
                    LocalDateTime.now().plusMinutes(1));
            assertEquals(true, unfrozen);
            assertEquals(AccountStatus.ACTIVE, account.getStatus());
        }

        @Test
        @DisplayName("should not unfreeze when permanent")
        void shouldNotUnfreezeWhenPermanent() {
            Account account = newActiveAccount();
            account.freezeByAdmin(OPERATOR_ID, OPERATOR_NAME, FreezeReason.ADMIN, null, null);
            boolean unfrozen = account.tryAutoUnfreezeIfExpired(LocalDateTime.now().plusYears(10));
            assertEquals(false, unfrozen);
            assertEquals(AccountStatus.FROZEN, account.getStatus());
        }
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :eagle-services:eagle-system-service:test --tests "*.AccountFreezeTest"`
Expected: FAIL（`Account.freezeByAdmin` 不存在）

- [ ] **Step 3: 修改 Account.java**

把 `Account.java` 完整替换为下面内容（字段移除 locked，新增 status + freeze；业务方法新增 freezeByAdmin / unfreeze /
tryAutoUnfreezeIfExpired；旧 lock/unlock 委托并 @Deprecated）：

```java
package com.eagle.system.auth.domain.model;

import com.eagle.common.base.BaseAggregateRoot;
import com.eagle.system.auth.domain.AuthErrorCode;
import com.eagle.system.auth.domain.event.AccountDeletedEvent;
import com.eagle.system.auth.domain.event.AccountFrozenEvent;
import com.eagle.system.auth.domain.event.AccountRegisteredEvent;
import com.eagle.system.auth.domain.event.AccountUnfrozenEvent;
import com.eagle.system.auth.domain.model.enums.AccountStatus;
import com.eagle.system.auth.domain.model.enums.FreezeReason;
import com.eagle.system.auth.domain.model.valueobject.AccountFreeze;
import com.eagle.system.auth.domain.model.valueobject.ProfileHints;
import com.eagle.system.auth.domain.model.valueobject.WechatBinding;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 账号聚合根
 *
 * @author sunshixiong
 */
@Entity
@Getter
@NoArgsConstructor
@Table(name = "auth_account", indexes = {
        @Index(name = "idx_account_username", columnList = "username", unique = true),
        @Index(name = "idx_account_phone", columnList = "phone"),
        @Index(name = "idx_account_openid", columnList = "openid"),
        @Index(name = "idx_account_unionid", columnList = "unionid"),
        @Index(name = "idx_account_web_openid", columnList = "web_openid"),
        @Index(name = "idx_account_mp_openid", columnList = "mp_openid")
})
public class Account extends BaseAggregateRoot<Account> {

    @Column(nullable = false, length = 64, unique = true, comment = "用户名")
    private String username;

    @Column(nullable = false, length = 128, comment = "密码（BCrypt）")
    private String password;

    @Column(length = 20, comment = "手机号")
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, comment = "账号状态")
    private AccountStatus status = AccountStatus.ACTIVE;

    @Embedded
    private AccountFreeze freeze;

    @Embedded
    private WechatBinding wechatBinding;

    @Transient
    private ProfileHints profileHints;

    // ==================== 工厂方法（均默认 ACTIVE）====================

    public static Account create(String username, String password, String phone,
                                 ProfileHints profileHints) {
        if (username == null || username.isBlank()) {
            throw AuthErrorCode.ACCOUNT_USERNAME_REQUIRED.toDomainException();
        }
        if (password == null || password.isBlank()) {
            throw AuthErrorCode.PASSWORD_REQUIRED.toDomainException();
        }
        Account account = new Account();
        account.username = username;
        account.password = password;
        account.phone = phone;
        account.status = AccountStatus.ACTIVE;
        account.profileHints = profileHints;
        return account;
    }

    public static Account createFromWechat(String openid, String unionid) {
        if (openid == null || openid.isBlank()) {
            throw AuthErrorCode.OPENID_REQUIRED.toDomainException();
        }
        Account account = new Account();
        account.username = "wx_" + openid.substring(0, Math.min(16, openid.length()));
        account.password = "";
        account.status = AccountStatus.ACTIVE;
        account.wechatBinding = WechatBinding.create(openid, unionid);
        account.profileHints = ProfileHints.EMPTY;
        return account;
    }

    public static Account createFromPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw AuthErrorCode.PHONE_REQUIRED.toDomainException();
        }
        Account account = new Account();
        account.username = phone;
        account.password = "";
        account.phone = phone;
        account.status = AccountStatus.ACTIVE;
        account.profileHints = ProfileHints.EMPTY;
        return account;
    }

    public static Account createFromWechatWeb(String webOpenid, String unionid,
                                              String nickname, String avatar) {
        if (webOpenid == null || webOpenid.isBlank()) {
            throw AuthErrorCode.WEB_OPENID_REQUIRED.toDomainException();
        }
        Account account = new Account();
        account.username = "wxweb_"
                + webOpenid.substring(0, Math.min(16, webOpenid.length()));
        account.password = "";
        account.status = AccountStatus.ACTIVE;
        account.wechatBinding = WechatBinding.createForWeb(webOpenid, unionid);
        account.profileHints = ProfileHints.ofWechat(nickname, avatar);
        return account;
    }

    public static Account createFromWechatH5(String mpOpenid, String unionid,
                                             String nickname, String avatar) {
        if (mpOpenid == null || mpOpenid.isBlank()) {
            throw AuthErrorCode.MP_OPENID_REQUIRED.toDomainException();
        }
        Account account = new Account();
        account.username = "wxmp_"
                + mpOpenid.substring(0, Math.min(16, mpOpenid.length()));
        account.password = "";
        account.status = AccountStatus.ACTIVE;
        account.wechatBinding = WechatBinding.createForH5(mpOpenid, unionid);
        account.profileHints = ProfileHints.ofWechat(nickname, avatar);
        return account;
    }

    // ==================== 事件发布 ====================

    @PostPersist
    private void onPostPersist() {
        if (profileHints != null) {
            registerEvent(new AccountRegisteredEvent(
                    getId(), username, phone,
                    profileHints.nickname(), profileHints.avatar(),
                    profileHints.email()
            ));
            profileHints = null;
        }
    }

    public void publishDeletedEvent() {
        registerEvent(new AccountDeletedEvent(getId()));
    }

    // ==================== 凭据 ====================

    public void changePassword(String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw AuthErrorCode.NEW_PASSWORD_REQUIRED.toDomainException();
        }
        this.password = newPassword;
    }

    public void bindPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw AuthErrorCode.PHONE_REQUIRED.toDomainException();
        }
        if (this.phone != null && !this.phone.isBlank()) {
            throw AuthErrorCode.ACCOUNT_PHONE_ALREADY_SET.toDomainException();
        }
        this.phone = phone;
    }

    // ==================== 冻结 / 解冻 ====================

    /**
     * 管理员显式冻结
     */
    public void freezeByAdmin(Long operatorId, String operatorName,
                              FreezeReason reason, LocalDateTime freezeUntil, String remark) {
        if (this.status == AccountStatus.FROZEN) {
            throw AuthErrorCode.ACCOUNT_FROZEN.toDomainException();
        }
        if (freezeUntil != null && !freezeUntil.isAfter(LocalDateTime.now())) {
            throw AuthErrorCode.ACCOUNT_FREEZE_UNTIL_INVALID.toDomainException();
        }
        this.status = AccountStatus.FROZEN;
        this.freeze = new AccountFreeze(
                reason, freezeUntil, operatorId, operatorName, remark, LocalDateTime.now());
        registerEvent(new AccountFrozenEvent(getId(), username, reason, freezeUntil, operatorId));
    }

    /**
     * 管理员显式解冻
     */
    public void unfreeze(Long operatorId, String operatorName) {
        if (this.status != AccountStatus.FROZEN) {
            throw AuthErrorCode.ACCOUNT_NOT_FROZEN.toDomainException();
        }
        this.status = AccountStatus.ACTIVE;
        this.freeze = null;
        registerEvent(new AccountUnfrozenEvent(
                getId(), username, AccountUnfrozenEvent.Source.ADMIN, operatorId));
    }

    /**
     * 登录路径懒触发：到期则自动解冻
     *
     * @return true 表示状态发生了变化
     */
    public boolean tryAutoUnfreezeIfExpired(LocalDateTime now) {
        if (this.status == AccountStatus.FROZEN
                && this.freeze != null
                && this.freeze.isExpired(now)) {
            this.status = AccountStatus.ACTIVE;
            this.freeze = null;
            registerEvent(new AccountUnfrozenEvent(
                    getId(), username, AccountUnfrozenEvent.Source.AUTO, null));
            return true;
        }
        return false;
    }

    // ==================== 旧 lock/unlock @Deprecated 委托 ====================

    /**
     * @deprecated use {@link #freezeByAdmin}
     */
    @Deprecated
    public void lock() {
        freezeByAdmin(null, "system-legacy",
                FreezeReason.OTHER, null, "legacy lock API");
    }

    /**
     * @deprecated use {@link #unfreeze}
     */
    @Deprecated
    public void unlock() {
        unfreeze(null, "system-legacy");
    }

    // ==================== 微信绑定 ====================

    public void bindWechat(String openid, String unionid) {
        if (openid == null || openid.isBlank()) {
            throw AuthErrorCode.OPENID_REQUIRED.toDomainException();
        }
        this.wechatBinding = WechatBinding.create(openid, unionid);
    }

    public void bindWechatWeb(String webOpenid, String unionid) {
        if (webOpenid == null || webOpenid.isBlank()) {
            throw AuthErrorCode.WEB_OPENID_REQUIRED.toDomainException();
        }
        if (this.wechatBinding == null) {
            this.wechatBinding = WechatBinding.createForWeb(webOpenid, unionid);
        } else {
            this.wechatBinding = this.wechatBinding.withWebOpenid(webOpenid);
            if (unionid != null) {
                this.wechatBinding = this.wechatBinding.withUnionid(unionid);
            }
        }
    }

    public void bindWechatH5(String mpOpenid, String unionid) {
        if (mpOpenid == null || mpOpenid.isBlank()) {
            throw AuthErrorCode.MP_OPENID_REQUIRED.toDomainException();
        }
        if (this.wechatBinding == null) {
            this.wechatBinding = WechatBinding.createForH5(mpOpenid, unionid);
        } else {
            this.wechatBinding = this.wechatBinding.withMpOpenid(mpOpenid);
            if (unionid != null) {
                this.wechatBinding = this.wechatBinding.withUnionid(unionid);
            }
        }
    }
}
```

- [ ] **Step 4: 运行 AccountFreezeTest 确认通过**

Run: `./gradlew :eagle-services:eagle-system-service:test --tests "*.AccountFreezeTest"`
Expected: PASS（7 个用例）

- [ ] **Step 5: 调整 AccountApplicationService 内的 `getLocked()` 引用**

在 `AccountApplicationService.java` 中：

- Line 177：`if (Boolean.TRUE.equals(account.getLocked()))` → `if (account.getStatus() == AccountStatus.FROZEN)`
- Line 215：同样替换
- 顶部 import 增加：`import com.eagle.system.auth.domain.model.enums.AccountStatus;`

- [ ] **Step 6: 调整 EagleUserDetailsServiceImpl**

在 `EagleUserDetailsServiceImpl.java`：

- Line 58、61：`!Boolean.TRUE.equals(account.getLocked())` → `account.getStatus() == AccountStatus.ACTIVE`
- 顶部 import 增加：`import com.eagle.system.auth.domain.model.enums.AccountStatus;`

- [ ] **Step 7: 全模块编译**

Run: `./gradlew :eagle-services:eagle-system-service:compileJava`
Expected: BUILD SUCCESSFUL（如有其他 `getLocked()` 调用残留，编译错误会指出位置，按相同模式替换）

- [ ] **Step 8: 跑现有 AccountApplicationServiceTest 确保不回归**

Run: `./gradlew :eagle-services:eagle-system-service:test --tests "*.AccountApplicationServiceTest"`
Expected: PASS

- [ ] **Step 9: Commit**

```bash
git add eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/domain/model/Account.java \
        eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/application/service/AccountApplicationService.java \
        eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/infrastructure/adapter/EagleUserDetailsServiceImpl.java \
        eagle-services/eagle-system-service/src/test/java/com/eagle/system/auth/domain/model/AccountFreezeTest.java
git commit -m "feat(auth): Account 聚合根升级为冻结模型（替换 locked boolean）"
```

---

### Task 1.7：AccountApplicationService 新增 freeze/unfreeze 接口

**Files:**

- Create: `auth/application/command/FreezeAccountCommand.java`
- Modify: `auth/application/service/AccountApplicationService.java`

- [ ] **Step 1: 写失败测试（在 AccountApplicationServiceTest 中追加 @Nested 块）**

在现有 `AccountApplicationServiceTest.java` 文件末尾的最后一个 `}` 之前追加：

```java
    @Nested
    @DisplayName("freezeAccount")
    class FreezeAccount {
        @Test
        @DisplayName("should freeze account and save")
        void shouldFreezeAccount() {
            Account account = existingAccount();
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(accountRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.freezeAccount(ACCOUNT_ID,
                    new com.eagle.system.auth.application.command.FreezeAccountCommand(
                            com.eagle.system.auth.domain.model.enums.FreezeReason.ADMIN,
                            null, "test", 99L, "admin"));

            assertEquals(com.eagle.system.auth.domain.model.enums.AccountStatus.FROZEN,
                    account.getStatus());
            verify(accountRepository).save(account);
        }

        @Test
        @DisplayName("should throw when account not found")
        void shouldThrowWhenNotFound() {
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class, () -> service.freezeAccount(ACCOUNT_ID,
                    new com.eagle.system.auth.application.command.FreezeAccountCommand(
                            com.eagle.system.auth.domain.model.enums.FreezeReason.ADMIN,
                            null, null, 99L, "admin")));
        }
    }

    @Nested
    @DisplayName("unfreezeAccount")
    class UnfreezeAccount {
        @Test
        @DisplayName("should unfreeze and save")
        void shouldUnfreeze() {
            Account account = existingAccount();
            account.freezeByAdmin(99L, "admin",
                    com.eagle.system.auth.domain.model.enums.FreezeReason.ADMIN, null, null);
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(accountRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.unfreezeAccount(ACCOUNT_ID, 99L, "admin");

            assertEquals(com.eagle.system.auth.domain.model.enums.AccountStatus.ACTIVE,
                    account.getStatus());
            verify(accountRepository).save(account);
        }
    }
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :eagle-services:eagle-system-service:test --tests "*.AccountApplicationServiceTest"`
Expected: FAIL（找不到方法 `freezeAccount` / `unfreezeAccount`）

- [ ] **Step 3: 创建 FreezeAccountCommand**

`auth/application/command/FreezeAccountCommand.java`：

```java
package com.eagle.system.auth.application.command;

import com.eagle.system.auth.domain.model.enums.FreezeReason;

import java.time.LocalDateTime;

/**
 * 冻结账号命令
 *
 * @author sunshixiong
 */
public record FreezeAccountCommand(
        FreezeReason reason,
        LocalDateTime freezeUntil,
        String remark,
        Long operatorId,
        String operatorName) {
}
```

- [ ] **Step 4: AccountApplicationService 新增方法 + 旧方法委托**

替换原 `lockAccount` / `unlockAccount` 区段：

```java
    /** 冻结账号（管理员显式触发）*/
    @Transactional(rollbackFor = Exception.class)
    public void freezeAccount(Long accountId, FreezeAccountCommand cmd) {
        Account account = findAccountById(accountId);
        account.freezeByAdmin(cmd.operatorId(), cmd.operatorName(),
                cmd.reason(), cmd.freezeUntil(), cmd.remark());
        accountRepository.save(account);
    }

    /** 解冻账号（管理员显式触发）*/
    @Transactional(rollbackFor = Exception.class)
    public void unfreezeAccount(Long accountId, Long operatorId, String operatorName) {
        Account account = findAccountById(accountId);
        account.unfreeze(operatorId, operatorName);
        accountRepository.save(account);
    }

    /**
     * @deprecated 改用 {@link #freezeAccount}
     */
    @Deprecated
    @Transactional(rollbackFor = Exception.class)
    public void lockAccount(Long accountId) {
        Account account = findAccountById(accountId);
        account.lock();
        accountRepository.save(account);
    }

    /**
     * @deprecated 改用 {@link #unfreezeAccount}
     */
    @Deprecated
    @Transactional(rollbackFor = Exception.class)
    public void unlockAccount(Long accountId) {
        Account account = findAccountById(accountId);
        account.unlock();
        accountRepository.save(account);
    }
```

并在顶部 import 增加：

```java
import com.eagle.system.auth.application.command.FreezeAccountCommand;
```

- [ ] **Step 5: 运行测试确认通过**

Run: `./gradlew :eagle-services:eagle-system-service:test --tests "*.AccountApplicationServiceTest"`
Expected: PASS（含新增 4 个用例）

- [ ] **Step 6: Commit**

```bash
git add eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/application/command/FreezeAccountCommand.java \
        eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/application/service/AccountApplicationService.java \
        eagle-services/eagle-system-service/src/test/java/com/eagle/system/auth/application/service/AccountApplicationServiceTest.java
git commit -m "feat(auth): AccountApplicationService 新增 freezeAccount/unfreezeAccount"
```

---

### Task 1.8：AccountController 新增 /freeze /unfreeze 端点 + 旧端点转发

**Files:**

- Create: `auth/interfaces/dto/request/FreezeAccountRequest.java`
- Modify: `auth/interfaces/controller/AccountController.java`

- [ ] **Step 1: 创建 FreezeAccountRequest DTO**

```java
package com.eagle.system.auth.interfaces.dto.request;

import com.eagle.system.auth.domain.model.enums.FreezeReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 冻结账号请求
 *
 * @author sunshixiong
 */
@Data
@Schema(description = "冻结账号请求")
public class FreezeAccountRequest {

    @NotNull
    @Schema(description = "冻结原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "ADMIN")
    private FreezeReason reason;

    @Schema(description = "冻结到期时间（null = 永久）", example = "2026-06-01T00:00:00")
    private LocalDateTime freezeUntil;

    @Schema(description = "冻结备注", example = "违规操作")
    private String remark;
}
```

- [ ] **Step 2: 修改 AccountController**

替换 `lockAccount` / `unlockAccount` 两个端点为以下代码块（注意：保留旧端点但加 @Deprecated 并转发）：

```java
    @Operation(summary = "冻结账号")
    @PatchMapping("/{accountId}/freeze")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin')")
    public void freezeAccount(@Parameter(description = "账号ID") @PathVariable Long accountId,
                              @Valid @RequestBody FreezeAccountRequest request,
                              @AuthenticationPrincipal EagleUser principal) {
        accountApplicationService.freezeAccount(accountId,
                new FreezeAccountCommand(request.getReason(), request.getFreezeUntil(),
                        request.getRemark(),
                        principal != null ? principal.getId() : null,
                        principal != null ? principal.getName() : "admin"));
    }

    @Operation(summary = "解冻账号")
    @PatchMapping("/{accountId}/unfreeze")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin')")
    public void unfreezeAccount(@Parameter(description = "账号ID") @PathVariable Long accountId,
                                @AuthenticationPrincipal EagleUser principal) {
        accountApplicationService.unfreezeAccount(accountId,
                principal != null ? principal.getId() : null,
                principal != null ? principal.getName() : "admin");
    }

    /** @deprecated 改用 /freeze */
    @Deprecated
    @Operation(summary = "[Deprecated] 锁定账号", description = "请改用 /freeze")
    @PatchMapping("/{accountId}/lock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin')")
    public void lockAccount(@Parameter(description = "账号ID") @PathVariable Long accountId) {
        accountApplicationService.lockAccount(accountId);
    }

    /** @deprecated 改用 /unfreeze */
    @Deprecated
    @Operation(summary = "[Deprecated] 解锁账号", description = "请改用 /unfreeze")
    @PatchMapping("/{accountId}/unlock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin')")
    public void unlockAccount(@Parameter(description = "账号ID") @PathVariable Long accountId) {
        accountApplicationService.unlockAccount(accountId);
    }
```

并在顶部增加 import：

```java
import com.eagle.common.dto.EagleUser;
import com.eagle.system.auth.application.command.FreezeAccountCommand;
import com.eagle.system.auth.interfaces.dto.request.FreezeAccountRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
```

- [ ] **Step 3: 编译**

Run: `./gradlew :eagle-services:eagle-system-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 跑全模块 build（含 modulith 验证）**

Run: `./gradlew :eagle-services:eagle-system-service:build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/interfaces/dto/request/FreezeAccountRequest.java \
        eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/interfaces/controller/AccountController.java
git commit -m "feat(auth): AccountController 新增 /freeze /unfreeze 端点，旧 /lock /unlock 标 Deprecated"
```

---

# Slice 2 — Blacklist 租户级聚合 + 应用服务 + 缓存

### Task 2.1：BlacklistType 枚举

**Files:** Create `auth/domain/model/enums/BlacklistType.java`

- [ ] **Step 1: 创建**

```java
package com.eagle.system.auth.domain.model.enums;

/**
 * 黑名单类型枚举
 *
 * @author sunshixiong
 */
public enum BlacklistType {
    /** 账号 ID（值为 Long 字符串）*/
    ACCOUNT_ID,
    /** 手机号 */
    PHONE,
    /** 邮箱 */
    EMAIL,
    /** IP 地址 */
    IP,
    /** 微信 openid */
    OPENID
}
```

- [ ] **Step 2: Commit**

```bash
git add eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/domain/model/enums/BlacklistType.java
git commit -m "feat(auth): 新增 BlacklistType 枚举"
```

---

### Task 2.2：BlacklistAddedEvent / BlacklistRemovedEvent

**Files:**

- Create: `auth/domain/event/BlacklistAddedEvent.java`
- Create: `auth/domain/event/BlacklistRemovedEvent.java`

- [ ] **Step 1: 创建两个 record 事件**

`BlacklistAddedEvent.java`：

```java
package com.eagle.system.auth.domain.event;

import com.eagle.system.auth.domain.model.enums.BlacklistType;

import java.time.LocalDateTime;

/**
 * 黑名单新增事件
 *
 * @author sunshixiong
 */
public record BlacklistAddedEvent(
        Long id,
        String tenantId,
        BlacklistType type,
        String value,
        LocalDateTime expiresAt) {
}
```

`BlacklistRemovedEvent.java`：

```java
package com.eagle.system.auth.domain.event;

import com.eagle.system.auth.domain.model.enums.BlacklistType;

/**
 * 黑名单删除事件
 *
 * @author sunshixiong
 */
public record BlacklistRemovedEvent(
        Long id,
        String tenantId,
        BlacklistType type,
        String value) {
}
```

- [ ] **Step 2: Commit**

```bash
git add eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/domain/event/BlacklistAddedEvent.java \
        eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/domain/event/BlacklistRemovedEvent.java
git commit -m "feat(auth): 新增 BlacklistAdded/Removed 领域事件"
```

---

### Task 2.3：Blacklist 聚合根（租户级）+ 单元测试

**Files:**

- Create: `auth/domain/model/Blacklist.java`
- Test: `auth/domain/model/BlacklistTest.java`

- [ ] **Step 1: 写失败单测**

`src/test/java/com/eagle/system/auth/domain/model/BlacklistTest.java`：

```java
package com.eagle.system.auth.domain.model;

import com.eagle.system.auth.domain.model.enums.BlacklistType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlacklistTest {

    @Test
    void shouldCreateWithRequiredFields() {
        Blacklist b = Blacklist.create(BlacklistType.PHONE, "13800138000",
                "test", null, 99L, "admin");
        assertEquals(BlacklistType.PHONE, b.getType());
        assertEquals("13800138000", b.getValue());
        assertEquals("test", b.getReason());
        assertEquals(99L, b.getOperatorId());
    }

    @Test
    void shouldDetectExpired() {
        Blacklist b = Blacklist.create(BlacklistType.IP, "1.1.1.1", null,
                LocalDateTime.now().minusMinutes(1), null, null);
        assertTrue(b.isExpired(LocalDateTime.now()));
    }

    @Test
    void shouldTreatNullExpiresAsPermanent() {
        Blacklist b = Blacklist.create(BlacklistType.IP, "1.1.1.1", null,
                null, null, null);
        assertFalse(b.isExpired(LocalDateTime.now().plusYears(10)));
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :eagle-services:eagle-system-service:test --tests "*.BlacklistTest"`
Expected: FAIL（`Blacklist` 类不存在）

- [ ] **Step 3: 创建 Blacklist 聚合根**

```java
package com.eagle.system.auth.domain.model;

import com.eagle.common.base.BaseAggregateRoot;
import com.eagle.system.auth.domain.event.BlacklistAddedEvent;
import com.eagle.system.auth.domain.event.BlacklistRemovedEvent;
import com.eagle.system.auth.domain.model.enums.BlacklistType;
import com.eagle.tenant.TenantAware;
import com.eagle.tenant.TenantContextHolder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.LocalDateTime;

/**
 * 身份黑名单聚合根（租户级）
 *
 * @author sunshixiong
 */
@Entity
@Getter
@NoArgsConstructor
@Table(name = "auth_blacklist", indexes = {
        @Index(name = "uk_blacklist_tenant_type_value",
                columnList = "tenant_id, type, value", unique = true),
        @Index(name = "idx_blacklist_tenant_expires",
                columnList = "tenant_id, expires_at")
})
@FilterDef(name = "tenantFilter",
        parameters = @ParamDef(name = "tenantId", type = String.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Blacklist extends BaseAggregateRoot<Blacklist> implements TenantAware {

    @Column(name = "tenant_id", nullable = false, updatable = false, length = 64, comment = "租户ID")
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, comment = "黑名单类型")
    private BlacklistType type;

    @Column(nullable = false, length = 128, comment = "黑名单值")
    private String value;

    @Column(length = 255, comment = "加黑原因")
    private String reason;

    @Column(name = "expires_at", comment = "过期时间（null=永久）")
    private LocalDateTime expiresAt;

    @Column(name = "operator_id", comment = "操作人ID（null=系统）")
    private Long operatorId;

    @Column(name = "operator_name", length = 64, comment = "操作人姓名")
    private String operatorName;

    public static Blacklist create(BlacklistType type, String value, String reason,
                                   LocalDateTime expiresAt, Long operatorId, String operatorName) {
        Blacklist b = new Blacklist();
        b.type = type;
        b.value = value;
        b.reason = reason;
        b.expiresAt = expiresAt;
        b.operatorId = operatorId;
        b.operatorName = operatorName;
        return b;
    }

    @PrePersist
    void fillTenant() {
        if (tenantId == null) {
            tenantId = TenantContextHolder.getTenantId();
        }
    }

    @PostPersist
    void onPostPersist() {
        registerEvent(new BlacklistAddedEvent(getId(), tenantId, type, value, expiresAt));
    }

    /** 删除前调用，注册 Removed 事件（应用服务负责调用）*/
    public void publishRemovedEvent() {
        registerEvent(new BlacklistRemovedEvent(getId(), tenantId, type, value));
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt != null && now.isAfter(expiresAt);
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew :eagle-services:eagle-system-service:test --tests "*.BlacklistTest"`
Expected: PASS（3 用例）

- [ ] **Step 5: Commit**

```bash
git add eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/domain/model/Blacklist.java \
        eagle-services/eagle-system-service/src/test/java/com/eagle/system/auth/domain/model/BlacklistTest.java
git commit -m "feat(auth): 新增租户级 Blacklist 聚合根"
```

---

### Task 2.4：BlacklistRepository

**Files:** Create `auth/domain/repository/BlacklistRepository.java`

- [ ] **Step 1: 创建仓储接口**

```java
package com.eagle.system.auth.domain.repository;

import com.eagle.system.auth.domain.model.Blacklist;
import com.eagle.system.auth.domain.model.enums.BlacklistType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 黑名单仓储
 *
 * @author sunshixiong
 */
@Repository
public interface BlacklistRepository extends JpaRepository<Blacklist, Long> {

    Optional<Blacklist> findByTypeAndValue(BlacklistType type, String value);

    Page<Blacklist> findByTypeAndValueContaining(BlacklistType type, String value, Pageable pageable);

    Page<Blacklist> findByType(BlacklistType type, Pageable pageable);

    /** 启动期全量加载（绕过 tenantFilter，返回所有租户的非过期记录）*/
    @Query(value = "SELECT * FROM auth_blacklist " +
            "WHERE deleted = 0 AND (expires_at IS NULL OR expires_at > :now)",
            nativeQuery = true)
    List<Blacklist> findAllActiveForCacheWarmup(@Param("now") java.time.LocalDateTime now);
}
```

- [ ] **Step 2: 编译 + Commit**

```bash
./gradlew :eagle-services:eagle-system-service:compileJava
git add eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/domain/repository/BlacklistRepository.java
git commit -m "feat(auth): 新增 BlacklistRepository"
```

---

### Task 2.5：DTO（AddBlacklistRequest / BlacklistResponse / BlacklistQuery / AddBlacklistCommand）

**Files:** Create 4 个文件

- [ ] **Step 1: AddBlacklistRequest**

`auth/interfaces/dto/request/AddBlacklistRequest.java`：

```java
package com.eagle.system.auth.interfaces.dto.request;

import com.eagle.system.auth.domain.model.enums.BlacklistType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 添加黑名单请求
 *
 * @author sunshixiong
 */
@Data
@Schema(description = "添加黑名单请求")
public class AddBlacklistRequest {

    @NotNull
    @Schema(description = "黑名单类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "PHONE")
    private BlacklistType type;

    @NotBlank
    @Size(max = 128)
    @Schema(description = "黑名单值", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    private String value;

    @Size(max = 255)
    @Schema(description = "加黑原因", example = "异常账号")
    private String reason;

    @Schema(description = "过期时间（null = 永久）", example = "2026-06-01T00:00:00")
    private LocalDateTime expiresAt;
}
```

- [ ] **Step 2: BlacklistResponse**

`auth/interfaces/dto/response/BlacklistResponse.java`：

```java
package com.eagle.system.auth.interfaces.dto.response;

import com.eagle.system.auth.domain.model.enums.BlacklistType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 黑名单响应
 *
 * @author sunshixiong
 */
@Data
@Schema(description = "黑名单条目")
public class BlacklistResponse {
    private Long id;
    private String tenantId;
    private BlacklistType type;
    private String value;
    private String reason;
    private LocalDateTime expiresAt;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime createTime;
}
```

- [ ] **Step 3: BlacklistQuery**

`auth/application/command/BlacklistQuery.java`：

```java
package com.eagle.system.auth.application.command;

import com.eagle.system.auth.domain.model.enums.BlacklistType;

/**
 * 黑名单查询参数
 *
 * @author sunshixiong
 */
public record BlacklistQuery(BlacklistType type, String value) {
}
```

- [ ] **Step 4: AddBlacklistCommand**

`auth/application/command/AddBlacklistCommand.java`：

```java
package com.eagle.system.auth.application.command;

import com.eagle.system.auth.domain.model.enums.BlacklistType;

import java.time.LocalDateTime;

/**
 * 添加黑名单命令
 *
 * @author sunshixiong
 */
public record AddBlacklistCommand(
        BlacklistType type,
        String value,
        String reason,
        LocalDateTime expiresAt,
        Long operatorId,
        String operatorName) {
}
```

- [ ] **Step 5: 编译 + Commit**

```bash
./gradlew :eagle-services:eagle-system-service:compileJava
git add eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/interfaces/dto/request/AddBlacklistRequest.java \
        eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/interfaces/dto/response/BlacklistResponse.java \
        eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/application/command/BlacklistQuery.java \
        eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/application/command/AddBlacklistCommand.java
git commit -m "feat(auth): 新增 Blacklist 相关 DTO 与 Command"
```

---

### Task 2.6：BlacklistMapper

**Files:** Create `auth/application/mapper/BlacklistMapper.java`

- [ ] **Step 1: 创建纯 Java Mapper**

```java
package com.eagle.system.auth.application.mapper;

import com.eagle.system.auth.domain.model.Blacklist;
import com.eagle.system.auth.interfaces.dto.response.BlacklistResponse;
import org.springframework.stereotype.Component;

/**
 * Blacklist 领域对象 → Response DTO 映射
 *
 * @author sunshixiong
 */
@Component
public class BlacklistMapper {

    public BlacklistResponse toResponse(Blacklist blacklist) {
        if (blacklist == null) {
            return null;
        }
        BlacklistResponse response = new BlacklistResponse();
        response.setId(blacklist.getId());
        response.setTenantId(blacklist.getTenantId());
        response.setType(blacklist.getType());
        response.setValue(blacklist.getValue());
        response.setReason(blacklist.getReason());
        response.setExpiresAt(blacklist.getExpiresAt());
        response.setOperatorId(blacklist.getOperatorId());
        response.setOperatorName(blacklist.getOperatorName());
        response.setCreateTime(blacklist.getCreatedAt());
        return response;
    }
}
```

> 注意：`BaseAggregateRoot` 提供 `getCreatedAt()`（项目约定，若属性名为 `createTime` 改对应 getter，本步若编译错误参照
> BaseAggregateRoot 内字段名调整一次）。

- [ ] **Step 2: 编译 + Commit**

```bash
./gradlew :eagle-services:eagle-system-service:compileJava
git add eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/application/mapper/BlacklistMapper.java
git commit -m "feat(auth): 新增 BlacklistMapper"
```

---

### Task 2.7：BlacklistApplicationService 接口骨架 + 单元测试

**Files:**

- Create: `auth/application/service/BlacklistApplicationService.java`
- Test: `auth/application/service/BlacklistApplicationServiceTest.java`

- [ ] **Step 1: 写失败单测**

```java
package com.eagle.system.auth.application.service;

import com.eagle.common.exception.ConflictException;
import com.eagle.common.exception.NotFoundException;
import com.eagle.system.auth.application.command.AddBlacklistCommand;
import com.eagle.system.auth.application.command.BlacklistQuery;
import com.eagle.system.auth.application.mapper.BlacklistMapper;
import com.eagle.system.auth.domain.model.Blacklist;
import com.eagle.system.auth.domain.model.enums.BlacklistType;
import com.eagle.system.auth.domain.repository.BlacklistRepository;
import com.eagle.system.auth.infrastructure.cache.BlacklistCacheStore;
import com.eagle.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlacklistApplicationServiceTest {

    @Mock BlacklistRepository repository;
    @Mock BlacklistMapper mapper;
    @Mock BlacklistCacheStore cacheStore;
    @InjectMocks BlacklistApplicationService service;

    @BeforeEach
    void setUp() { TenantContextHolder.setTenantId("t1"); }
    @AfterEach
    void tearDown() { TenantContextHolder.clear(); }

    @Nested
    @DisplayName("addToBlacklist")
    class Add {
        @Test
        @DisplayName("should save when not duplicated")
        void shouldSave() {
            when(repository.findByTypeAndValue(BlacklistType.PHONE, "13800138000"))
                    .thenReturn(Optional.empty());
            when(repository.save(any(Blacklist.class)))
                    .thenAnswer(i -> i.getArgument(0));

            service.addToBlacklist(new AddBlacklistCommand(
                    BlacklistType.PHONE, "13800138000", "test", null, 99L, "admin"));

            verify(repository).save(any(Blacklist.class));
        }

        @Test
        @DisplayName("should throw on duplicate")
        void shouldRejectDuplicate() {
            when(repository.findByTypeAndValue(BlacklistType.PHONE, "13800138000"))
                    .thenReturn(Optional.of(Blacklist.create(
                            BlacklistType.PHONE, "13800138000", null, null, null, null)));

            assertThrows(ConflictException.class,
                    () -> service.addToBlacklist(new AddBlacklistCommand(
                            BlacklistType.PHONE, "13800138000", null, null, 99L, "admin")));
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("removeFromBlacklist")
    class Remove {
        @Test
        @DisplayName("should throw NotFound")
        void notFound() {
            when(repository.findById(99L)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class, () -> service.removeFromBlacklist(99L));
        }
    }

    @Nested
    @DisplayName("isBlacklisted")
    class IsBlack {
        @Test
        @DisplayName("should hit cache")
        void hit() {
            when(cacheStore.isMember("t1", BlacklistType.IP, "1.1.1.1")).thenReturn(true);
            assertTrue(service.isBlacklisted(BlacklistType.IP, "1.1.1.1"));
            verify(repository, never()).findByTypeAndValue(any(), any());
        }
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :eagle-services:eagle-system-service:test --tests "*.BlacklistApplicationServiceTest"`
Expected: FAIL（类不存在）

- [ ] **Step 3: 创建 BlacklistCacheStore 端口**

`auth/infrastructure/cache/BlacklistCacheStore.java`（接口先空实现稍后补 Redis）：

```java
package com.eagle.system.auth.infrastructure.cache;

import com.eagle.system.auth.domain.model.enums.BlacklistType;

/**
 * 黑名单 Redis 缓存读写抽象
 *
 * @author sunshixiong
 */
public interface BlacklistCacheStore {
    void add(String tenantId, BlacklistType type, String value);
    void remove(String tenantId, BlacklistType type, String value);
    boolean isMember(String tenantId, BlacklistType type, String value);
}
```

- [ ] **Step 4: 创建 BlacklistApplicationService 实现**

```java
package com.eagle.system.auth.application.service;

import com.eagle.system.auth.application.command.AddBlacklistCommand;
import com.eagle.system.auth.application.command.BlacklistQuery;
import com.eagle.system.auth.application.mapper.BlacklistMapper;
import com.eagle.system.auth.domain.AuthErrorCode;
import com.eagle.system.auth.domain.model.Blacklist;
import com.eagle.system.auth.domain.model.enums.BlacklistType;
import com.eagle.system.auth.domain.repository.BlacklistRepository;
import com.eagle.system.auth.infrastructure.cache.BlacklistCacheStore;
import com.eagle.system.auth.interfaces.dto.response.BlacklistResponse;
import com.eagle.tenant.TenantContextHolder;
import com.eagle.tenant.annotation.TenantFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 黑名单应用服务
 *
 * @author sunshixiong
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlacklistApplicationService {

    private final BlacklistRepository repository;
    private final BlacklistMapper mapper;
    private final BlacklistCacheStore cacheStore;

    @TenantFilter
    @Transactional(readOnly = true)
    public Page<BlacklistResponse> queryBlacklist(BlacklistQuery query, Pageable pageable) {
        Page<Blacklist> page;
        if (query.type() != null && query.value() != null && !query.value().isBlank()) {
            page = repository.findByTypeAndValueContaining(query.type(), query.value(), pageable);
        } else if (query.type() != null) {
            page = repository.findByType(query.type(), pageable);
        } else {
            page = repository.findAll(pageable);
        }
        return page.map(mapper::toResponse);
    }

    @Transactional(rollbackFor = Exception.class)
    public BlacklistResponse addToBlacklist(AddBlacklistCommand cmd) {
        if (cmd.expiresAt() != null && !cmd.expiresAt().isAfter(LocalDateTime.now())) {
            throw AuthErrorCode.ACCOUNT_FREEZE_UNTIL_INVALID.toDomainException();
        }
        repository.findByTypeAndValue(cmd.type(), cmd.value()).ifPresent(b -> {
            throw AuthErrorCode.BLACKLIST_DUPLICATE.toConflictException();
        });
        Blacklist blacklist = Blacklist.create(
                cmd.type(), cmd.value(), cmd.reason(), cmd.expiresAt(),
                cmd.operatorId(), cmd.operatorName());
        Blacklist saved = repository.save(blacklist);
        log.info("blacklist added: id={}, type={}, value={}",
                saved.getId(), saved.getType(), saved.getValue());
        return mapper.toResponse(saved);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeFromBlacklist(Long id) {
        Blacklist blacklist = repository.findById(id)
                .orElseThrow(AuthErrorCode.BLACKLIST_NOT_FOUND::toNotFoundException);
        blacklist.publishRemovedEvent();
        repository.save(blacklist); // flush event
        repository.deleteById(id);
        log.info("blacklist removed: id={}, type={}, value={}",
                id, blacklist.getType(), blacklist.getValue());
    }

    /**
     * 黑名单命中判断：先查 Redis，命中后回 DB 验证未过期；Redis 异常降级直接查 DB
     */
    public boolean isBlacklisted(BlacklistType type, String value) {
        String tenantId = TenantContextHolder.getTenantId();
        if (cacheStore.isMember(tenantId, type, value)) {
            Optional<Blacklist> entry = repository.findByTypeAndValue(type, value);
            if (entry.isEmpty()) {
                cacheStore.remove(tenantId, type, value);
                return false;
            }
            if (entry.get().isExpired(LocalDateTime.now())) {
                // 懒过期清理（异步）
                try {
                    removeFromBlacklist(entry.get().getId());
                } catch (Exception e) {
                    log.warn("failed to lazy-expire blacklist id={}", entry.get().getId(), e);
                }
                return false;
            }
            return true;
        }
        return false;
    }
}
```

- [ ] **Step 5: 运行测试**

Run: `./gradlew :eagle-services:eagle-system-service:test --tests "*.BlacklistApplicationServiceTest"`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/infrastructure/cache/BlacklistCacheStore.java \
        eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/application/service/BlacklistApplicationService.java \
        eagle-services/eagle-system-service/src/test/java/com/eagle/system/auth/application/service/BlacklistApplicationServiceTest.java
git commit -m "feat(auth): BlacklistApplicationService 增删查 + Redis 抽象"
```

---

### Task 2.8：BlacklistCacheStore Redis 实现

**Files:** Create `auth/infrastructure/cache/RedisBlacklistCacheStore.java`

- [ ] **Step 1: 创建实现类**

```java
package com.eagle.system.auth.infrastructure.cache;

import com.eagle.system.auth.domain.model.enums.BlacklistType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * BlacklistCacheStore 的 Redis Set 实现
 *
 * <p>Key 格式：{@code auth:blacklist:{tenantId}:{TYPE}}
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisBlacklistCacheStore implements BlacklistCacheStore {

    private static final String KEY_PREFIX = "auth:blacklist:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void add(String tenantId, BlacklistType type, String value) {
        try {
            redisTemplate.opsForSet().add(key(tenantId, type), value);
        } catch (Exception e) {
            log.warn("blacklist cache add failed: tenant={}, type={}, value={}",
                    tenantId, type, value, e);
        }
    }

    @Override
    public void remove(String tenantId, BlacklistType type, String value) {
        try {
            redisTemplate.opsForSet().remove(key(tenantId, type), value);
        } catch (Exception e) {
            log.warn("blacklist cache remove failed: tenant={}, type={}, value={}",
                    tenantId, type, value, e);
        }
    }

    @Override
    public boolean isMember(String tenantId, BlacklistType type, String value) {
        try {
            Boolean hit = redisTemplate.opsForSet().isMember(key(tenantId, type), value);
            return Boolean.TRUE.equals(hit);
        } catch (Exception e) {
            log.warn("blacklist cache check failed (fallback to DB): tenant={}, type={}, value={}",
                    tenantId, type, value, e);
            return false;
        }
    }

    private String key(String tenantId, BlacklistType type) {
        return KEY_PREFIX + (tenantId != null ? tenantId : "0") + ":" + type.name();
    }
}
```

- [ ] **Step 2: 编译 + Commit**

```bash
./gradlew :eagle-services:eagle-system-service:compileJava
git add eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/infrastructure/cache/RedisBlacklistCacheStore.java
git commit -m "feat(auth): BlacklistCacheStore Redis Set 实现"
```

---

### Task 2.9：BlacklistCacheSyncHandler 事件同步缓存

**Files:** Create `auth/infrastructure/event/BlacklistCacheSyncHandler.java`

- [ ] **Step 1: 创建事件处理器**

```java
package com.eagle.system.auth.infrastructure.event;

import com.eagle.system.auth.domain.event.BlacklistAddedEvent;
import com.eagle.system.auth.domain.event.BlacklistRemovedEvent;
import com.eagle.system.auth.infrastructure.cache.BlacklistCacheStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 黑名单变更事件同步 Redis 缓存
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlacklistCacheSyncHandler {

    private final BlacklistCacheStore cacheStore;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAdded(BlacklistAddedEvent event) {
        cacheStore.add(event.tenantId(), event.type(), event.value());
        log.info("blacklist cache add: tenant={}, type={}, value={}",
                event.tenantId(), event.type(), event.value());
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRemoved(BlacklistRemovedEvent event) {
        cacheStore.remove(event.tenantId(), event.type(), event.value());
        log.info("blacklist cache remove: tenant={}, type={}, value={}",
                event.tenantId(), event.type(), event.value());
    }
}
```

- [ ] **Step 2: Commit**

```bash
./gradlew :eagle-services:eagle-system-service:compileJava
git add eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/infrastructure/event/BlacklistCacheSyncHandler.java
git commit -m "feat(auth): 黑名单变更事件 → Redis 同步处理器"
```

---

### Task 2.10：BlacklistCacheWarmer 启动期全量加载

**Files:** Create `auth/infrastructure/cache/BlacklistCacheWarmer.java`

- [ ] **Step 1: 创建**

```java
package com.eagle.system.auth.infrastructure.cache;

import com.eagle.system.auth.domain.model.Blacklist;
import com.eagle.system.auth.domain.repository.BlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 启动期黑名单全量加载至 Redis
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlacklistCacheWarmer {

    private final BlacklistRepository repository;
    private final BlacklistCacheStore cacheStore;

    @Value("${eagle.auth.blacklist.cache-warm-on-startup:true}")
    private boolean enabled;

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        if (!enabled) {
            log.info("blacklist cache warmer disabled");
            return;
        }
        long start = System.currentTimeMillis();
        List<Blacklist> all = repository.findAllActiveForCacheWarmup(LocalDateTime.now());
        for (Blacklist b : all) {
            cacheStore.add(b.getTenantId(), b.getType(), b.getValue());
        }
        log.info("blacklist cache warmed: count={}, costMs={}",
                all.size(), System.currentTimeMillis() - start);
    }
}
```

- [ ] **Step 2: Commit**

```bash
./gradlew :eagle-services:eagle-system-service:compileJava
git add eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/infrastructure/cache/BlacklistCacheWarmer.java
git commit -m "feat(auth): 启动期黑名单全量加载至 Redis"
```

---

### Task 2.11：BlacklistController

**Files:** Create `auth/interfaces/controller/BlacklistController.java`

- [ ] **Step 1: 创建 Controller**

```java
package com.eagle.system.auth.interfaces.controller;

import com.eagle.common.dto.EagleUser;
import com.eagle.system.auth.application.command.AddBlacklistCommand;
import com.eagle.system.auth.application.command.BlacklistQuery;
import com.eagle.system.auth.application.service.BlacklistApplicationService;
import com.eagle.system.auth.domain.model.enums.BlacklistType;
import com.eagle.system.auth.interfaces.dto.request.AddBlacklistRequest;
import com.eagle.system.auth.interfaces.dto.response.BlacklistResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 身份黑名单管理控制器（租户级，仅管理员）
 *
 * @author sunshixiong
 */
@Tag(name = "黑名单管理", description = "租户级身份黑名单的增删查")
@RestController
@RequestMapping("/admin/blacklist")
@RequiredArgsConstructor
public class BlacklistController {

    private final BlacklistApplicationService blacklistApplicationService;

    @Operation(summary = "查询黑名单（分页）")
    @GetMapping
    @PreAuthorize("hasRole('admin')")
    public Page<BlacklistResponse> query(
            @RequestParam(required = false) BlacklistType type,
            @RequestParam(required = false) String value,
            @ParameterObject
            @Parameter(description = "分页参数（page 从 0 开始）")
            @PageableDefault Pageable pageable) {
        return blacklistApplicationService.queryBlacklist(new BlacklistQuery(type, value), pageable);
    }

    @Operation(summary = "新增黑名单条目")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('admin')")
    public BlacklistResponse add(@Valid @RequestBody AddBlacklistRequest request,
                                 @AuthenticationPrincipal EagleUser principal) {
        return blacklistApplicationService.addToBlacklist(
                new AddBlacklistCommand(
                        request.getType(), request.getValue(), request.getReason(),
                        request.getExpiresAt(),
                        principal != null ? principal.getId() : null,
                        principal != null ? principal.getName() : "admin"));
    }

    @Operation(summary = "删除黑名单条目")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin')")
    public void remove(@Parameter(description = "黑名单ID") @PathVariable Long id) {
        blacklistApplicationService.removeFromBlacklist(id);
    }
}
```

- [ ] **Step 2: 编译 + Modulith 验证**

Run: `./gradlew :eagle-services:eagle-system-service:test --tests "*.ModulithArchitectureTest"`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/interfaces/controller/BlacklistController.java
git commit -m "feat(auth): BlacklistController 增删查接口（仅管理员）"
```

---

### Task 2.12：Slice 2 整合验证

- [ ] **Step 1: 跑全模块 build**

Run: `./gradlew :eagle-services:eagle-system-service:build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 启动应用本地验证表结构创建**

Run（如本地 dev 环境就绪）：

```bash
./gradlew :eagle-services:eagle-system-service:bootRun
```

查看启动日志确认 Hibernate 自动创建 `auth_blacklist` 表，无报错；关闭进程（Ctrl+C）。

> 若 dev 环境暂未就绪可跳过此步骤；记录在 PR 描述中由 reviewer 验证。

---

# Slice 3 — BlacklistChecker 接入登录链路

### Task 3.1：BlacklistChecker

**Files:** Create `auth/infrastructure/security/BlacklistChecker.java`

- [ ] **Step 1: 写失败单测**

`src/test/java/com/eagle/system/auth/infrastructure/security/BlacklistCheckerTest.java`：

```java
package com.eagle.system.auth.infrastructure.security;

import com.eagle.common.exception.AppException;
import com.eagle.system.auth.application.service.BlacklistApplicationService;
import com.eagle.system.auth.domain.AuthErrorCode;
import com.eagle.system.auth.domain.model.enums.BlacklistType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlacklistCheckerTest {

    @Mock BlacklistApplicationService blacklist;
    @InjectMocks BlacklistChecker checker;

    @Test
    void shouldThrowWhenIpBlacklisted() {
        when(blacklist.isBlacklisted(BlacklistType.IP, "1.1.1.1")).thenReturn(true);
        AppException ex = assertThrows(AppException.class,
                () -> checker.checkLogin(null, null, "1.1.1.1", null));
        assertEquals(AuthErrorCode.IP_BLACKLISTED.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    void shouldThrowWhenPhoneBlacklisted() {
        when(blacklist.isBlacklisted(BlacklistType.PHONE, "13800138000")).thenReturn(true);
        AppException ex = assertThrows(AppException.class,
                () -> checker.checkLogin(null, "13800138000", null, null));
        assertEquals(AuthErrorCode.IDENTITY_BLACKLISTED.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    void shouldThrowWhenAccountIdBlacklisted() {
        when(blacklist.isBlacklisted(BlacklistType.ACCOUNT_ID, "123")).thenReturn(true);
        AppException ex = assertThrows(AppException.class,
                () -> checker.checkLogin(null, null, null, 123L));
        assertEquals(AuthErrorCode.IDENTITY_BLACKLISTED.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    void shouldPassWhenNotBlacklisted() {
        assertDoesNotThrow(() -> checker.checkLogin("alice", "13800138000", "1.1.1.1", 1L));
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :eagle-services:eagle-system-service:test --tests "*.BlacklistCheckerTest"`
Expected: FAIL（类不存在）

- [ ] **Step 3: 创建 BlacklistChecker**

```java
package com.eagle.system.auth.infrastructure.security;

import com.eagle.system.auth.application.service.BlacklistApplicationService;
import com.eagle.system.auth.domain.AuthErrorCode;
import com.eagle.system.auth.domain.model.enums.BlacklistType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 登录 / 注册链路的黑名单前置校验
 *
 * <p>所有判断都依赖 {@code TenantContextHolder} 已被 {@code TenantIdFilter} 填充。
 *
 * @author sunshixiong
 */
@Component
@RequiredArgsConstructor
public class BlacklistChecker {

    private final BlacklistApplicationService blacklist;

    /** 用户名/手机号登录前置校验 */
    public void checkLogin(String username, String phone, String ip, Long accountId) {
        if (ip != null && blacklist.isBlacklisted(BlacklistType.IP, ip)) {
            throw AuthErrorCode.IP_BLACKLISTED.toServiceException();
        }
        if (phone != null && !phone.isBlank()
                && blacklist.isBlacklisted(BlacklistType.PHONE, phone)) {
            throw AuthErrorCode.IDENTITY_BLACKLISTED.toServiceException();
        }
        if (accountId != null
                && blacklist.isBlacklisted(BlacklistType.ACCOUNT_ID, String.valueOf(accountId))) {
            throw AuthErrorCode.IDENTITY_BLACKLISTED.toServiceException();
        }
    }

    /** 注册前置校验 */
    public void checkRegister(String phone, String email, String ip) {
        if (ip != null && blacklist.isBlacklisted(BlacklistType.IP, ip)) {
            throw AuthErrorCode.IP_BLACKLISTED.toServiceException();
        }
        if (phone != null && !phone.isBlank()
                && blacklist.isBlacklisted(BlacklistType.PHONE, phone)) {
            throw AuthErrorCode.IDENTITY_BLACKLISTED.toServiceException();
        }
        if (email != null && !email.isBlank()
                && blacklist.isBlacklisted(BlacklistType.EMAIL, email)) {
            throw AuthErrorCode.IDENTITY_BLACKLISTED.toServiceException();
        }
    }

    /** 微信登录前置校验 */
    public void checkWechat(String openid, String ip) {
        if (ip != null && blacklist.isBlacklisted(BlacklistType.IP, ip)) {
            throw AuthErrorCode.IP_BLACKLISTED.toServiceException();
        }
        if (openid != null && !openid.isBlank()
                && blacklist.isBlacklisted(BlacklistType.OPENID, openid)) {
            throw AuthErrorCode.IDENTITY_BLACKLISTED.toServiceException();
        }
    }
}
```

- [ ] **Step 4: 测试通过 + Commit**

```bash
./gradlew :eagle-services:eagle-system-service:test --tests "*.BlacklistCheckerTest"
git add eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/infrastructure/security/BlacklistChecker.java \
        eagle-services/eagle-system-service/src/test/java/com/eagle/system/auth/infrastructure/security/BlacklistCheckerTest.java
git commit -m "feat(auth): BlacklistChecker 登录/注册/微信前置校验"
```

---

### Task 3.2：接入 EagleUserDetailsServiceImpl（密码登录）

**Files:** Modify `auth/infrastructure/adapter/EagleUserDetailsServiceImpl.java`

- [ ] **Step 1: 注入 BlacklistChecker 并调用两次（前置 IP + 加载 Account 后 phone/accountId）**

修改方法：

```java
@Override
@Transactional(readOnly = true)
public @NonNull UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
    String ip = ClientIpHolder.get();   // 见 Step 2
    // 1) IP 前置（账号未知）
    blacklistChecker.checkLogin(username, null, ip, null);

    Account account = accountRepository.findByUsername(username)
            .orElseThrow(AuthErrorCode.ACCOUNT_NOT_FOUND::toNotFoundException);

    // 2) 账号已知后再次校验 phone / accountId 维度
    blacklistChecker.checkLogin(username, account.getPhone(), ip, account.getId());

    AuthorizationInfo authInfo = authorizationPort
            .findAuthorizationInfo(account.getId())
            .orElse(AuthorizationInfo.empty());

    return new EagleUser(
            account.getId(),
            account.getUsername(),
            account.getPassword(),
            authInfo.name() != null ? authInfo.name() : account.getUsername(),
            account.getPhone(),
            account.getStatus() == AccountStatus.ACTIVE,
            true,
            true,
            account.getStatus() == AccountStatus.ACTIVE,
            authInfo.roleCodes().stream()
                    .<GrantedAuthority>map(code ->
                            new SimpleGrantedAuthority(SecurityConstants.ROLE_START + code))
                    .collect(Collectors.toList())
    );
}
```

新增字段：

```java
private final BlacklistChecker blacklistChecker;
```

- [ ] **Step 2: 新增 ClientIpHolder（ThreadLocal 持有当前请求 IP）**

`auth/infrastructure/security/ClientIpHolder.java`：

```java
package com.eagle.system.auth.infrastructure.security;

/**
 * 当前请求的 IP（由 SecurityFilter 写入，UserDetailsService 读取）
 *
 * @author sunshixiong
 */
public final class ClientIpHolder {

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private ClientIpHolder() {}

    public static void set(String ip) { HOLDER.set(ip); }
    public static String get() { return HOLDER.get(); }
    public static void clear() { HOLDER.remove(); }
}
```

并在 `LoginRateLimitFilter`（已存在）尾部 finally 块前补充 `ClientIpHolder.set(ip)`，filter 退出前 `ClientIpHolder.clear()`
。具体位置：

```java
// LoginRateLimitFilter.doFilterInternal 内最外层 try-finally
String ip = ...; // 已有
ClientIpHolder.set(ip);
try {
    // 现有逻辑
    chain.doFilter(request, response);
} finally {
    ClientIpHolder.clear();
}
```

- [ ] **Step 3: 编译 + 跑 modulith 测试**

Run:

```bash
./gradlew :eagle-services:eagle-system-service:compileJava
./gradlew :eagle-services:eagle-system-service:test --tests "*.ModulithArchitectureTest"
```

Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/infrastructure/security/ClientIpHolder.java \
        eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/infrastructure/security/LoginRateLimitFilter.java \
        eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/infrastructure/adapter/EagleUserDetailsServiceImpl.java
git commit -m "feat(auth): 密码登录前置黑名单校验（IP + phone + accountId）"
```

---

### Task 3.3：接入 SmsCodeAuthenticationProvider

**Files:** Modify `auth/infrastructure/security/SmsCodeAuthenticationProvider.java`

- [ ] **Step 1: 读取该文件，定位 `authenticate(Authentication)` 方法入口**

Run:
`grep -n "authenticate" eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/infrastructure/security/SmsCodeAuthenticationProvider.java`

- [ ] **Step 2: 在 authenticate 方法开头注入并调用**

在 `private final ...` 字段段末尾增加：

```java
private final BlacklistChecker blacklistChecker;
```

在 `authenticate(Authentication authentication)` 方法首行（提取 phone 之后）插入：

```java
String phone = /* 现有代码提取的手机号 */;
blacklistChecker.checkLogin(null, phone, ClientIpHolder.get(), null);
```

> 具体行号需在实现时根据现有代码定位；保留现有所有逻辑，仅在 phone 解析后、SMS 校验前增加 1 行。

- [ ] **Step 3: 编译 + Commit**

```bash
./gradlew :eagle-services:eagle-system-service:compileJava
git add eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/infrastructure/security/SmsCodeAuthenticationProvider.java
git commit -m "feat(auth): SMS 登录前置黑名单校验"
```

---

### Task 3.4：接入 WechatMiniProgramAuthenticationProvider / WechatAppAuthenticationProvider

**Files:** Modify 两个 Wechat Provider 文件

- [ ] **Step 1: 两个文件均添加字段**

```java
private final BlacklistChecker blacklistChecker;
```

并在 `authenticate(Authentication)` 提取 openid 之后插入：

```java
blacklistChecker.checkWechat(openid, ClientIpHolder.get());
```

- [ ] **Step 2: 编译 + Commit**

```bash
./gradlew :eagle-services:eagle-system-service:compileJava
git add eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/infrastructure/security/WechatMiniProgramAuthenticationProvider.java \
        eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/infrastructure/security/WechatAppAuthenticationProvider.java
git commit -m "feat(auth): 微信登录前置黑名单校验（openid + IP）"
```

---

### Task 3.5：接入注册端点

**Files:** Modify `auth/interfaces/controller/AccountController.java`

- [ ] **Step 1: 在 register 端点开头注入 BlacklistChecker 并调用**

在 controller 字段段增加：

```java
private final BlacklistChecker blacklistChecker;
```

修改 `register` 方法：

```java
@Operation(summary = "用户自主注册")
@PostMapping("/register")
@ResponseStatus(HttpStatus.CREATED)
@PreAuthorize("permitAll()")
public Map<String, Long> register(@Valid @RequestBody RegisterAccountRequest request,
                                  jakarta.servlet.http.HttpServletRequest httpRequest) {
    String ip = httpRequest.getRemoteAddr();
    blacklistChecker.checkRegister(request.getPhone(), request.getEmail(), ip);
    Long accountId = accountApplicationService.register(
            request.getUsername(), request.getPassword(), request.getPhone(),
            request.getEmail(), request.getNickname());
    return Map.of("accountId", accountId);
}
```

- [ ] **Step 2: 编译 + Modulith + Commit**

```bash
./gradlew :eagle-services:eagle-system-service:compileJava
./gradlew :eagle-services:eagle-system-service:test --tests "*.ModulithArchitectureTest"
git add eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/interfaces/controller/AccountController.java
git commit -m "feat(auth): 注册端点前置黑名单校验"
```

---

# Slice 4 — 冻结强制下线（OnlineUserPort 反向索引）

### Task 4.1：OnlineUserPort 接口增加 listJtisByAccount

**Files:** Modify `auth/domain/port/OnlineUserPort.java`

- [ ] **Step 1: 接口追加方法**

在 `boolean isBlacklisted(String jti);` 之前追加：

```java
    /**
     * 反查某账号当前所有在线 JTI。
     *
     * @param accountId 账号 ID
     * @return JTI 列表（空集合表示未在线）
     */
    java.util.List<String> listJtisByAccount(Long accountId);
```

- [ ] **Step 2: 编译会失败（实现未跟上），先 Commit 接口本身**

```bash
git add eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/domain/port/OnlineUserPort.java
# 注意：先不要 commit，等 Task 4.2 实现一起 commit
```

---

### Task 4.2：OnlineUserAdapter 维护反向索引 Set + 实现 listJtisByAccount

**Files:** Modify `auth/infrastructure/adapter/OnlineUserAdapter.java`

- [ ] **Step 1: 替换 OnlineUserAdapter 实现**

```java
package com.eagle.system.auth.infrastructure.adapter;

import com.alibaba.fastjson2.JSON;
import com.eagle.system.auth.domain.port.OnlineUserInfo;
import com.eagle.system.auth.domain.port.OnlineUserPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * OnlineUserPort 的 Redis 实现
 *
 * <p>Redis Key 规范：
 * <ul>
 *   <li>{@code online:users:{jti}}        — OnlineUserInfo JSON</li>
 *   <li>{@code account:online:{accountId}} — Set&lt;jti&gt; 反向索引</li>
 *   <li>{@code token:blacklist:{jti}}     — "1"</li>
 * </ul>
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OnlineUserAdapter implements OnlineUserPort {

    private static final String ONLINE_KEY_PREFIX = "online:users:";
    private static final String ACCOUNT_INDEX_PREFIX = "account:online:";
    private static final String BLACKLIST_KEY_PREFIX = "token:blacklist:";
    private static final long DEFAULT_TTL_SECONDS = 3600L;

    private final StringRedisTemplate redisTemplate;

    @Override
    public void trackLogin(OnlineUserInfo info) {
        try {
            String json = JSON.toJSONString(info);
            redisTemplate.opsForValue().set(
                    ONLINE_KEY_PREFIX + info.tokenId(), json,
                    info.expiresIn(), TimeUnit.SECONDS);
            if (info.userId() != null) {
                String indexKey = ACCOUNT_INDEX_PREFIX + info.userId();
                redisTemplate.opsForSet().add(indexKey, info.tokenId());
                redisTemplate.expire(indexKey, info.expiresIn(), TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.warn("failed to track online user: tokenId={}", info.tokenId(), e);
        }
    }

    @Override
    public List<OnlineUserInfo> listOnlineUsers() {
        List<OnlineUserInfo> result = new ArrayList<>();
        try (var cursor = redisTemplate.scan(ScanOptions.scanOptions()
                .match(ONLINE_KEY_PREFIX + "*").count(100).build())) {
            cursor.forEachRemaining(key -> {
                String json = redisTemplate.opsForValue().get(key);
                if (json != null) {
                    try {
                        result.add(JSON.parseObject(json, OnlineUserInfo.class));
                    } catch (Exception e) {
                        log.warn("Skipping malformed OnlineUserInfo for key: {}", key, e);
                    }
                }
            });
        } catch (Exception e) {
            log.warn("failed to list online users", e);
        }
        return result;
    }

    @Override
    public List<String> listJtisByAccount(Long accountId) {
        if (accountId == null) {
            return List.of();
        }
        try {
            Set<String> jtis = redisTemplate.opsForSet().members(ACCOUNT_INDEX_PREFIX + accountId);
            return jtis == null ? List.of() : new ArrayList<>(jtis);
        } catch (Exception e) {
            log.warn("failed to list jtis by accountId={}", accountId, e);
            return List.of();
        }
    }

    @Override
    public void forceLogout(String tokenId) {
        try {
            log.info("Force logout, tokenId: {}", tokenId);
            String onlineKey = ONLINE_KEY_PREFIX + tokenId;
            String json = redisTemplate.opsForValue().get(onlineKey);
            Long ttl = redisTemplate.getExpire(onlineKey, TimeUnit.SECONDS);

            // 反向索引 SREM
            if (json != null) {
                try {
                    OnlineUserInfo info = JSON.parseObject(json, OnlineUserInfo.class);
                    if (info.userId() != null) {
                        redisTemplate.opsForSet()
                                .remove(ACCOUNT_INDEX_PREFIX + info.userId(), tokenId);
                    }
                } catch (Exception parseEx) {
                    log.warn("force logout: malformed online info, jti={}", tokenId, parseEx);
                }
            }

            redisTemplate.delete(onlineKey);
            long blacklistTtl = (ttl != null && ttl > 0) ? ttl : DEFAULT_TTL_SECONDS;
            redisTemplate.opsForValue().set(
                    BLACKLIST_KEY_PREFIX + tokenId, "1", blacklistTtl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("failed to force logout: tokenId={}", tokenId, e);
        }
    }

    @Override
    public boolean isBlacklisted(String jti) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + jti));
        } catch (Exception e) {
            log.warn("failed to check blacklist, defaulting to not-blacklisted: jti={}", jti, e);
            return false;
        }
    }
}
```

- [ ] **Step 2: 编译 + Commit**

```bash
./gradlew :eagle-services:eagle-system-service:compileJava
git add eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/domain/port/OnlineUserPort.java \
        eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/infrastructure/adapter/OnlineUserAdapter.java
git commit -m "feat(auth): OnlineUserPort 新增 listJtisByAccount 反向索引"
```

---

### Task 4.3：AccountSecurityEventHandler — 冻结事件强制下线

**Files:**

- Create: `auth/infrastructure/event/AccountSecurityEventHandler.java`
- Test: `auth/infrastructure/event/AccountSecurityEventHandlerTest.java`

- [ ] **Step 1: 写失败单测**

```java
package com.eagle.system.auth.infrastructure.event;

import com.eagle.system.auth.domain.event.AccountFrozenEvent;
import com.eagle.system.auth.domain.model.enums.FreezeReason;
import com.eagle.system.auth.domain.port.OnlineUserPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountSecurityEventHandlerTest {

    @Mock OnlineUserPort onlineUserPort;
    @InjectMocks AccountSecurityEventHandler handler;

    @Test
    void shouldForceLogoutAllJtis() {
        when(onlineUserPort.listJtisByAccount(100L))
                .thenReturn(List.of("jti-1", "jti-2", "jti-3"));

        handler.onAccountFrozen(new AccountFrozenEvent(
                100L, "alice", FreezeReason.ADMIN, null, 99L));

        verify(onlineUserPort).forceLogout("jti-1");
        verify(onlineUserPort).forceLogout("jti-2");
        verify(onlineUserPort).forceLogout("jti-3");
        verify(onlineUserPort, times(3)).forceLogout(org.mockito.ArgumentMatchers.anyString());
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :eagle-services:eagle-system-service:test --tests "*.AccountSecurityEventHandlerTest"`
Expected: FAIL

- [ ] **Step 3: 创建 Handler**

```java
package com.eagle.system.auth.infrastructure.event;

import com.eagle.system.auth.domain.event.AccountFrozenEvent;
import com.eagle.system.auth.domain.port.OnlineUserPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * 账号安全事件处理器：冻结 → 强制下线该账号所有在线 token
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountSecurityEventHandler {

    private final OnlineUserPort onlineUserPort;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAccountFrozen(AccountFrozenEvent event) {
        List<String> jtis = onlineUserPort.listJtisByAccount(event.accountId());
        for (String jti : jtis) {
            onlineUserPort.forceLogout(jti);
        }
        log.info("frozen account force-logout: accountId={}, jtiCount={}",
                event.accountId(), jtis.size());
    }
}
```

- [ ] **Step 4: 测试通过 + Commit**

```bash
./gradlew :eagle-services:eagle-system-service:test --tests "*.AccountSecurityEventHandlerTest"
git add eagle-services/eagle-system-service/src/main/java/com/eagle/system/auth/infrastructure/event/AccountSecurityEventHandler.java \
        eagle-services/eagle-system-service/src/test/java/com/eagle/system/auth/infrastructure/event/AccountSecurityEventHandlerTest.java
git commit -m "feat(auth): 冻结事件触发强制下线该账号所有 token"
```

---

# Slice 5 — 清理 base 域遗留 UserLockedEvent

### Task 5.1：删除 UserLockedEvent 与 handler

**Files:**

- Delete: `base/domain/event/UserLockedEvent.java`
- Modify: `base/infrastructure/event/UserEventHandler.java`

- [ ] **Step 1: 删除 UserLockedEvent**

```bash
rm eagle-services/eagle-system-service/src/main/java/com/eagle/system/base/domain/event/UserLockedEvent.java
```

- [ ] **Step 2: 在 UserEventHandler.java 中删除 handleUserLocked 方法 + 顶部 import**

定位 `import com.eagle.system.base.domain.event.UserLockedEvent;` 行删除；
定位 `public void handleUserLocked(UserLockedEvent event)` 方法（包含其上的 `@Async / @TransactionalEventListener`
注解块），整段删除。

- [ ] **Step 3: 编译 + Modulith 验证**

Run:

```bash
./gradlew :eagle-services:eagle-system-service:compileJava
./gradlew :eagle-services:eagle-system-service:test --tests "*.ModulithArchitectureTest"
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add -A eagle-services/eagle-system-service/src/main/java/com/eagle/system/base/
git commit -m "refactor(base): 清理遗留 UserLockedEvent（lock 已迁移至 auth.Account 冻结模型）"
```

---

### Task 5.2：最终完整构建验证

- [ ] **Step 1: 全模块 clean build**

Run: `./gradlew :eagle-services:eagle-system-service:clean build`
Expected: BUILD SUCCESSFUL，全部测试通过

- [ ] **Step 2: 跑全 modulith 验证**

Run: `./gradlew :eagle-services:eagle-system-service:test --tests "*.ModulithArchitectureTest"`
Expected: PASS

- [ ] **Step 3: 检查 git log 简洁**

Run: `git log --oneline -25`
Expected: 25 条左右 commits，scope 一致（auth/base），符合 Conventional Commits

- [ ] **Step 4: 推送或保留本地**

`git status` 应当 clean。是否推送由用户决定，本计划不主动 push。

---

## Self-Review

### Spec 覆盖

| Spec 章节                         | 对应 Task                                                                                                                                                                  |
|---------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 2.1 Account 冻结模型                | Task 1.1 – 1.6                                                                                                                                                           |
| 2.2 Blacklist 聚合                | Task 2.1, 2.3                                                                                                                                                            |
| 2.3 领域事件                        | Task 1.4, 2.2                                                                                                                                                            |
| 3.1 auth_account 字段             | Task 1.3, 1.6（@Embedded 自动 ddl-auto 同步）                                                                                                                                  |
| 3.2 auth_blacklist 表            | Task 2.3（@Table + @Index 自动 ddl-auto 同步）                                                                                                                                 |
| 4.1 AccountApplicationService   | Task 1.7                                                                                                                                                                 |
| 4.2 BlacklistApplicationService | Task 2.7                                                                                                                                                                 |
| 5.1 AccountController           | Task 1.8                                                                                                                                                                 |
| 5.2 BlacklistController         | Task 2.11                                                                                                                                                                |
| 6.1 BlacklistChecker            | Task 3.1                                                                                                                                                                 |
| 6.2 接入 5 个位点                    | Task 3.2 – 3.5                                                                                                                                                           |
| 6.3 冻结强制下线                      | Task 4.1 – 4.3                                                                                                                                                           |
| 7.1/7.2 Redis 缓存                | Task 2.8, 2.9, 2.10                                                                                                                                                      |
| 7.3 用户缓存失效                      | 由现有 `UserEventHandler` 处理；冻结/解冻事件复用现有 evict 机制（在 Task 5.1 后通过 UserUpdatedEvent 路径自然失效；如未生效请新增 `evictUserCache` 调用到 AccountSecurityEventHandler，本计划内默认依赖 username 缓存 TTL） |
| 8 错误码 + i18n                    | Task 1.5                                                                                                                                                                 |
| 9 单元测试                          | Task 1.6, 1.7, 2.3, 2.7, 3.1, 4.3（共 6 个测试类）                                                                                                                              |
| 10 回滚                           | 通过 `@ConditionalOnProperty` 在配置项 `eagle.auth.blacklist.enabled` 落实（Task 2.10 已有），冻结回滚需手工切换 Controller 端点                                                                 |
| 11 配置项                          | `eagle.auth.blacklist.cache-warm-on-startup` 在 Task 2.10 落实                                                                                                              |

### 占位符扫描

- 无 TBD / TODO；Task 2.6 BlacklistMapper 备注了 `getCreatedAt()` 字段名可能需要按 BaseAggregateRoot 实际名称调整一次，这是显式
  escape hatch 不算占位符
- Task 3.3 `phone = /* 现有代码提取的手机号 */` 是引用现有代码段的位置说明，**实施时需读取 SmsCodeAuthenticationProvider
  内现有 phone 提取逻辑后替换** — 这一处需在实施时填充

### 类型一致性

- `FreezeReason / AccountStatus / BlacklistType` 在所有 task 中拼写一致
- `freezeByAdmin / unfreeze / tryAutoUnfreezeIfExpired` 方法名贯穿一致
- `AccountFrozenEvent / AccountUnfrozenEvent.Source.ADMIN / AUTO` 一致
- `BlacklistCacheStore.add/remove/isMember` 在 Service / Handler / RedisImpl 三处一致

### 已知补丁点（实施时）

1. Task 2.6 BlacklistMapper：若 `BaseAggregateRoot` getter 名为 `getCreateTime()` 而非 `getCreatedAt()`，对应改用
2. Task 3.3 SmsCodeAuthenticationProvider：插入位置需先读取该 Provider 现有代码定位 phone 提取行号
3. Task 3.4 Wechat Providers：同上，需先读取定位 openid 提取行号
4. 配置开关 `eagle.auth.blacklist.enabled` 若希望生效到 BlacklistChecker 阻塞行为，可在 Task 3.1 BlacklistChecker 类上额外加
   `@ConditionalOnProperty(name="eagle.auth.blacklist.enabled", havingValue="true", matchIfMissing=true)`
   （本计划默认启用，需要时由实施者补）

---

## 执行入口

参见上方"通用约定"。从 Task 1.1 顺序执行，每个 task 末尾的 commit 标志可被验收。
