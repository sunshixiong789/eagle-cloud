# 手机号为主账号体系 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans（本计划由主会话直接执行，不派 subagent）。Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 第三方登录（淘宝/Apple/微信四渠道）统一挂靠手机号主账号：首登 `binding_required` + `social_bind`，存量影子账号绑手机自动归并。

**Architecture:** 复用 SAS 自定义 grant 骨架新增 `social_bind`；BindTicket 走 StringRedisTemplate + tools.jackson；归并在 `AccountApplicationService.bindPhone` 事务内完成，AFTER_COMMIT 踢下线。

**Tech Stack:** Spring Boot 4 / Spring Authorization Server 7 / Spring Data JPA / Redis / Mockito（测试零基础设施依赖）

**Spec:** `docs/superpowers/specs/2026-07-16-phone-primary-account-design.md`

## Global Constraints

- 配置一律 @ConfigurationProperties，禁止 @Value（rules 02/19）。
- JSON 一律 tools.jackson `ObjectMapper`（Boot 4 + Jackson 3），禁止 com.fasterxml。
- 测试纯 Mockito，不依赖 H2/Redis/Nacos。
- 不写 Flyway；索引变更依赖 ddl-auto=update（unique 化需手动 DROP 旧索引，列入交付说明）。
- 错误码接 `AuthErrorCode` 现有序列（11059 起）。

---

### Task 1: 错误码 + BindTicket 领域端口与 Redis 适配器

**Files:**
- Modify: `domain/AuthErrorCode.java`（+4 码）
- Create: `domain/port/BindTicket.java`（record）
- Create: `domain/port/BindTicketStore.java`（port 接口）
- Create: `infrastructure/adapter/RedisBindTicketStore.java`
- Test: `infrastructure/adapter/RedisBindTicketStoreTest.java`

**Interfaces (Produces):**
```java
enum SocialProvider { TAOBAO, APPLE, WECHAT }
enum WechatChannel { MINI_PROGRAM, APP, PC, H5 }
record BindTicket(SocialProvider provider, String identifier,
                  WechatChannel wechatChannel, String unionid,
                  String nickname, String avatar,
                  String appleEmail, String appleFullName,
                  String appleRefreshTokenCiphertext) { /* 工厂: ofTaobao/ofApple/ofWechat */ }
interface BindTicketStore {
    String save(BindTicket ticket);          // 返回高熵 ticketId，TTL 10min
    Optional<BindTicket> consume(String id); // GETDEL 语义，一次性
}
```
错误码：`SOCIAL_BIND_TICKET_INVALID(11059)`、`APPLE_ALREADY_BOUND(11060)`、
`WECHAT_ALREADY_BOUND(11061)`、`SOCIAL_IDENTITY_ALREADY_BOUND(11062)`。

- [x] 测试先行（mock StringRedisTemplate，参照 OnlineUserAdapterTest）→ 实现 → 通过

### Task 2: Account 域方法补齐（bindApple + 微信/淘宝冲突语义）

**Files:**
- Modify: `domain/model/Account.java`
- Test: `AccountBindingTest.java`（新建，域层纯单测）

**Interfaces (Produces):**
```java
void bindApple(String subject, String encryptedRefreshToken)
// 已绑相同 subject：幂等 + 轮换密文；已绑不同 subject：APPLE_ALREADY_BOUND
// bindWechat/bindWechatWeb/bindWechatH5：本渠道已绑不同 openid → WECHAT_ALREADY_BOUND（现状是静默覆盖，收紧）
// bindTaobao 维持现有语义（幂等 / TAOBAO_ALREADY_BOUND）
```

- [x] 测试先行 → 实现 → 通过

### Task 3: 唯一索引收紧

**Files:**
- Modify: `Account.java` @Table indexes：`openid`/`unionid`/`web_openid`/`mp_openid`/`taobao_open_uid` 全部 `unique = true`

- [x] 修改 + 编译通过（交付说明注明存量库需 DROP 旧普通索引）

### Task 4: 微信统一查找（unionid 优先，四渠道一致）

**Files:**
- Modify: `application/service/WechatWebUserService.java`
- Test: `WechatWebUserServiceTest.java`（补用例）

**Interfaces (Produces):**
```java
/** 查找并按需补绑本渠道 openid；不创建账号。 */
Optional<Account> findWechatAccount(WechatChannel channel, String openid, String unionid)
// 顺序：本渠道 openid → unionid（命中补绑本渠道 openid 后保存）→ empty
// findOrCreateWechatWebAccount 内部改为复用该查找，创建分支保留（web/H5 现状流程）
```

- [x] 测试先行 → 实现 → 通过

### Task 5: SocialBindingRequiredException + token 端点 errorResponseHandler

**Files:**
- Create: `infrastructure/security/SocialBindingRequiredException.java`
- Create: `infrastructure/security/BindingRequiredErrorResponseHandler.java`
- Modify: `infrastructure/config/OAuth2AuthorizationServerSecurityConfig.java`（tokenEndpoint.errorResponseHandler）
- Test: `BindingRequiredErrorResponseHandlerTest.java`

**Interfaces (Produces):**
```java
class SocialBindingRequiredException extends OAuth2AuthenticationException {
    // error=binding_required; 字段：String bindTicket, SocialProvider provider
}
// handler：命中 binding_required 输出 {"error","bind_ticket","provider"}；
// 其余异常委托 SAS 默认 OAuth2ErrorAuthenticationFailureHandler
```

- [x] 测试先行 → 实现 → 通过

### Task 6: 四个 Provider 未命中改抛 binding_required

**Files:**
- Modify: `TaobaoAppAuthenticationProvider` / `AppleAppAuthenticationProvider` /
  `WechatAppAuthenticationProvider` / `WechatMiniProgramAuthenticationProvider`
- Test: 对应四个既有 ProviderTest 补/改用例

命中绑定（微信含 unionid 归并）→ 直登不变；未命中 → `BindTicketStore.save` + 抛
`SocialBindingRequiredException`。`findOrCreateByTaobao/Apple/WechatOpenid` 的"创建"分支移除
（方法删除或收窄为纯查找，调用点同步）。

- [x] 测试先行 → 实现 → 通过

### Task 7: social_bind grant 三件套 + 应用服务

**Files:**
- Create: `SocialBindAuthenticationToken.java` / `SocialBindAuthenticationConverter.java` /
  `SocialBindAuthenticationProvider.java`
- Create: `application/service/SocialBindApplicationService.java`
- Modify: `AccountApplicationService.java`（`findOrCreateByPhone(String phone, ProfileHints hints)` 重载）
- Modify: `OAuth2AuthorizationServerSecurityConfig.registerCustomGrants`、`application.yml` app 客户端 grant 列表 + `social_bind`
- Test: `SocialBindAuthenticationConverterTest` / `SocialBindApplicationServiceTest` / `SocialBindGrantSupportTest`

**Interfaces (Produces):**
```java
// grant_type=social_bind，参数 bind_ticket / phone / code
Account SocialBindApplicationService.bind(String ticketId, String phone, String code)
// 1 consume ticket(无效→SOCIAL_BIND_TICKET_INVALID) 2 verifyCode 3 BlacklistChecker(按 provider)
// 4 并发兜底:身份已被绑→SOCIAL_IDENTITY_ALREADY_BOUND 5 findOrCreateByPhone(带 hints)
// 6 冻结前置检查 7 按 provider/channel 挂接 8 save(唯一索引冲突翻译)
```

- [x] 测试先行 → 实现 → 通过

### Task 8: bindPhone 影子账号自动归并

**Files:**
- Modify: `AccountApplicationService.bindPhone` → 返回 `BindPhoneResult(boolean merged)`
- Modify: `Account.java`（`isShadowAccount()`、`transferBindingsFrom`/清空绑定辅助）
- Modify: `AccountController.bindPhone` → 200 + `{"merged": bool}`
- Test: `AccountApplicationServiceTest` 补归并用例

归并（单事务）：影子判定（DISABLED_PASSWORD 且无手机号）→ 主账号同 provider 异身份冲突检查
（XXX_ALREADY_BOUND）→ 影子被冻结拒绝 → **先 delete 影子 + flush，再迁移绑定到主账号 save**
（Hibernate flush 顺序陷阱）→ `publishDeletedEvent()` 级联删 system User（不走
AccountDeletionApplicationService，避免 Apple revoke）→ afterCommit 同步器
`OnlineUserPort.listJtisByAccount + forceLogout` 踢影子下线。

- [x] 测试先行 → 实现 → 通过

### Task 9: 全量验证 + 收尾

- [x] `./gradlew :eagle-services:eagle-auth-service:test` 全绿
- [x] spec 与实现的偏差回写 spec（错误码命名等）
- [x] feature 分支提交（不 push）

## Self-Review 结论

- Spec §4.1→Task1、§4.2→Task4/6、§4.3→Task7、§5→Task8、§6→Task2/3、§8→各 Task 测试步骤；无遗漏。
- wx_phone_code / web 硬约束为 spec 非目标，不设任务。
- 类型一致性：BindTicket/SocialProvider/WechatChannel 在 Task1 定义，Task4/6/7 引用同名。
