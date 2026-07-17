# 手机号为主账号体系：第三方登录统一挂靠手机号

- 日期：2026-07-16
- 作者：sunshixiong（与 Claude Code 协作 brainstorm）
- 状态：已实现并通过全量单测（feature/phone-primary-account 分支）
- 影响范围：`eagle-auth-service` 的 Account 聚合 / 四个自定义 grant Provider / AccountApplicationService / 错误码；客户端登录流程（App 端及未来小程序端）

## 1. 背景与目标

现状：每种第三方登录（淘宝 / Apple / 微信）首次登录都通过 `findOrCreateByXxx`
自动创建一个"影子账号"（`apple_xxx` / `tb_xxx` / `wx_xxx`）。同一个人先用手机号登录、
再用 Apple 登录，就产生两个 Account、两个 system 域 User。之后影子账号补绑手机号时
撞 `idx_account_phone` 唯一约束，报 409「该手机号已绑定其他账号」（线上日志已复现：
`apple_fabac2d1…` 账号 16 绑定已有主的手机号被拒）。

目标模型：**手机号是人的唯一身份锚点**。一个手机号 = 一个 Account = 一个 system 域 User；
淘宝 / Apple / 微信只是「登录方式」，全部挂在同一个手机号主账号下，怎么登录都进同一个账号。

`auth_account` 表结构已支持一个账号同时持有微信 + 淘宝 + Apple 绑定与手机号，
**本设计不改表结构**，只改流程与索引。

## 2. 设计原则

- 第三方身份 → 账号必须一对一，数据库唯一索引兜底（与 `idx_account_phone` 同样的 TOCTOU 防线）。
- 复用现有机制：自定义 grant 骨架（`AbstractCustomGrantAuthenticationProvider`）、
  短信验证码（`SmsService`）、账号删除级联（`AccountDeletedEvent`）、
  强制下线（`OnlineUserPort.forceLogout`）。
- YAGNI：不拆独立绑定表；嵌入式 VO 结构已满足，未来新 provider = 新 VO + 唯一索引。
- 范围：**四个自定义 grant 全部纳入** binding_required 流程——`taobao_app`、`apple_app`、
  `wechat_app`、`wechat_miniprogram`（微信小程序登录同样以手机号统一）。
  网页扫码 / 公众号 H5 是浏览器 session 流程，不走 token 端点，已有
  `/login/bind-phone` 引导页（`WechatWebLoginController`），本期通过「绑手机时自动归并」
  达成同样的最终一致（见 §5），不重构其 session 流程。
- 微信多渠道同主体：同一微信号在小程序 / App / PC / H5 的 openid 不同但 unionid 相同，
  已通过手机号验证的微信主体在其他微信渠道登录时**不重复验手机号**（unionid 归并直登）。

## 3. 方案总览

```
┌─ 新流量 ──────────────────────────────────────────────────────────┐
│ 第三方授权成功                                                      │
│   ├─ 身份已绑定某账号 → 直登（现状不变，老用户无感）                    │
│   └─ 身份未绑定 → 不再自动建账号：                                    │
│        发 bind_ticket（Redis，TTL 10min）                           │
│        → token 端点返回 error=binding_required + bind_ticket        │
│        → 客户端弹「绑定手机号」页                                     │
│        → grant_type=social_bind (ticket+phone+code)                │
│        → findOrCreateByPhone → 挂接第三方绑定 → 签发正式 token        │
└──────────────────────────────────────────────────────────────────┘
┌─ 存量影子账号 ─────────────────────────────────────────────────────┐
│ 影子账号 bindPhone 撞已有主账号 → 自动归并：                          │
│   绑定迁移到主账号 → 删除影子账号（级联删 system User）→ 踢下线         │
│   → 客户端重新登录，之后进的就是主账号                                │
└──────────────────────────────────────────────────────────────────┘
```

## 4. 新流量：binding_required + social_bind grant

### 4.1 BindTicket

Redis 存储的一次性凭证，TTL 10 分钟，key 为高熵随机串（UUID 级别）：

| 字段 | 说明 |
|---|---|
| provider | TAOBAO / APPLE / WECHAT |
| identifier | openUid / apple subject / 微信本渠道 openid |
| wechatChannel | MINI_PROGRAM / APP / PC / H5（provider=WECHAT 时必填，决定挂接到哪个 openid 字段） |
| unionid | 微信附带（可空） |
| nickname / avatar | 微信附带的 profile hints（可空，仅新建主账号时使用） |
| appleEmail / appleFullName / appleRefreshTokenCiphertext | Apple 附带（可空） |

第三方凭证在 Provider 内已完成验签 / 换取，ticket 里只存**验签后的结果**，
`social_bind` 阶段不再回调第三方。消费即删（`GETDEL` 语义），防重放。

### 4.2 Provider 改造（taobao_app / apple_app / wechat_app / wechat_miniprogram）

`authenticateGrant` 未命中绑定时不再 `findOrCreateByXxx`，改为：

1. 生成 BindTicket 写 Redis；
2. 抛 `SocialBindingRequiredException extends OAuth2AuthenticationException`
   （`error=binding_required`），异常携带 ticket 与 provider。

**微信渠道统一查找顺序**（四渠道一致，收敛现状差异——小程序目前只查 openid，
无 unionid 优先逻辑；App/PC/H5 走 `WechatWebUserService` 已有 unionid 优先）：

1. 本渠道 openid（`openid` / `web_openid` / `mp_openid`）命中 → 直登；
2. unionid 命中 → 同一微信主体已在别的渠道完成过手机号验证：
   把本渠道 openid 补绑到该账号后直登，**不重复验手机号**；
3. 都未命中 → `binding_required`。

落地方式：把 `WechatWebUserService` 的 unionid 优先查找逻辑抽为四渠道共用
（`wechat_miniprogram` 的 `findOrCreateByWechatOpenid` 并入），
「查找/补绑」与「创建」拆开——查找归 provider，创建只发生在 `social_bind`。

token 端点注册自定义 `AuthenticationFailureHandler`：识别 `binding_required` 时输出

```json
{ "error": "binding_required", "bind_ticket": "…", "provider": "APPLE" }
```

其余错误走原有默认输出，不影响现存错误契约。

### 4.3 social_bind grant

新增 `SocialBindAuthenticationToken / Converter / Provider`（沿用现有自定义 grant 三件套模式），
grant_type = `social_bind`，参数 `bind_ticket`、`phone`、`code`：

1. 取出并删除 ticket（不存在 / 过期 → `SOCIAL_BIND_TICKET_INVALID`，客户端重走第三方授权）；
2. 校验短信验证码（`SmsService.verifyCode`）；
3. 黑名单检查（复用 `BlacklistChecker`，按 provider 分派）；
4. `findOrCreateByPhone(phone)` 得到主账号；
5. 并发兜底：此刻第三方身份若已被其他账号绑定（双端同时提交）→ 409；
6. 主账号冻结前置检查：挂接是持久化副作用，必须在绑定**之前**拒绝
   （骨架的 FROZEN 检查发生在 `authenticateGrant` 返回之后，只能兜底签发环节）；
7. 主账号挂接：淘宝 `bindTaobao`、Apple `bindApple`、微信按 `wechatChannel` 分派
   `bindWechat / bindWechatWeb / bindWechatH5`（含 unionid）。同身份幂等，
   同 provider 异身份 → 409；
8. 走 `AbstractCustomGrantAuthenticationProvider` 骨架签发 token。

Apple 附带信息处理：`bindApple` 挂接 subject + refresh token 密文；
email / fullName / 微信昵称头像作为 ProfileHints 仅在 `findOrCreateByPhone`
**新建**账号时有意义——新建路径改为允许携带 hints
（现有 `createFromPhone` 传 `ProfileHints.EMPTY` 的调用点不变）。

**小程序一键手机号（预留扩展，本期不实现）**：微信小程序支持 `getPhoneNumber`
组件一键授权微信绑定的手机号（服务端以 code 调 `phonenumber.getPhoneNumber` API 换号），
免发短信。`social_bind` 的参数设计预留第二种凭证形态：`bind_ticket + wx_phone_code`
替代 `bind_ticket + phone + code`。小程序真正接入时再实现该分支，接口形态不变。

## 5. 存量影子账号：bindPhone 冲突自动归并

**影子账号判定**：`password == DISABLED_PASSWORD && phone == null`（仅第三方直登产生的账号）。

`AccountApplicationService.bindPhone` 中，手机号已属账号 A 且当前账号 B ≠ A 时：

- B 是影子账号 → 归并（下述）；
- B 不是影子账号（有密码或有手机号的实账号）→ 维持现状 409 `PHONE_ALREADY_BOUND`。
  实账号之间不做自动合并（数据归属复杂，非目标）。

**归并流程**（单事务）：

1. 冲突检查：A 已绑**同 provider 的不同身份** → 409 `ACCOUNT_MERGE_CONFLICT`（新错误码），不静默覆盖；
2. B 的全部第三方绑定迁移到 A（`wechatBinding` / `taobaoBinding` / `appleBinding` 整体搬移；
   Apple refresh token 密文随绑定迁移，**不得调用 Apple revoke**——身份仍在使用）；
3. 删除 B 并**显式 flush 后**再保存 A：Hibernate 默认 flush 顺序是 update 先于 delete，
   若 A 的绑定列更新先落库而 B 还持有相同值，会误撞唯一索引。删除走
   `publishDeletedEvent()` → `AccountDeletedEvent` → system 域级联删除 B 的 User
   （注意**不走** `AccountDeletionApplicationService`，避免其 Apple revoke 逻辑）；
4. 唯一索引兜底：save A 时若并发撞绑定唯一索引 → 翻译为 409（推广
   `savePhoneWithUniquenessGuard` 的模式到绑定列）。

**事务提交后**（`@TransactionalEventListener(AFTER_COMMIT)` 或应用服务显式编排）：

5. `OnlineUserPort.listJtisByAccount(B.id)` → 逐个 `forceLogout`，B 的存量 token 全部失效；
6. bindPhone 接口返回归并结果 `{ merged: true }`，客户端提示后重新登录
   （手机号 / 第三方任意方式重登都落在主账号 A）。

## 6. 域模型与索引

- 补 `Account.bindApple(String subject, String encryptedRefreshToken)`：
  已绑相同 subject 幂等（并轮换 refresh token 密文）；已绑不同 subject → 409。
  与 `bindTaobao` 冲突语义对齐；`bindWechat*` 已有，维持。
- 唯一索引补齐（第三方身份 → 账号一对一的数据库兜底）：

| 列 | 现状 | 目标 |
|---|---|---|
| apple_subject | unique | 不变 |
| taobao_open_uid | 普通索引 | **unique** |
| openid / web_openid / mp_openid | 普通索引 | **unique**（PostgreSQL 唯一索引默认 NULLS DISTINCT，多行 NULL 不冲突，不影响未绑定账号） |
| unionid | 普通索引 | **unique**——unionid 现在承担跨渠道归并直登的查找职责，同一 unionid 出现在两个账号会使 `findByWechatBindingUnionid` 结果不确定，必须一对一 |

  开发期依赖 `ddl-auto=update`（项目约定不写 Flyway）；**注意 `update` 不会把既有普通索引
  改成 unique**，开发 / 测试库需手动 `DROP INDEX` 旧索引后由 JPA 重建，spec 落地时在计划中列为独立步骤。
- 新错误码（`AuthErrorCode`，实现落地）：`SOCIAL_BIND_TICKET_INVALID(11059)`、
  `APPLE_ALREADY_BOUND(11060)`、`WECHAT_ALREADY_BOUND(11061)`、
  `SOCIAL_IDENTITY_ALREADY_BOUND(11062)`（并发兜底通用翻译）。
  归并冲突不单设 `ACCOUNT_MERGE_CONFLICT`——「主账号已绑同 provider 异身份」
  复用 `XXX_ALREADY_BOUND` 系列，语义一致。

## 7. 错误处理与边界

| 场景 | 行为 |
|---|---|
| ticket 过期 / 重放 | `SOCIAL_BIND_TICKET_INVALID`，客户端重走第三方授权 |
| 验证码错误 | `SMS_CODE_INVALID`（现状） |
| 手机号主账号被冻结 | grant 骨架统一拒绝 `account_frozen`（现状） |
| 影子账号被冻结 | 拒绝归并（冻结是风控手段，`ACCOUNT_FROZEN`） |
| 第三方身份 / IP 在黑名单 | `BlacklistChecker` 拒绝（现状，social_bind 补同款检查） |
| 双端并发 social_bind / bindPhone | 唯一索引兜底 → 409 |

## 8. 测试策略

遵循项目测试哲学：纯 Mockito 单测，不依赖 H2 / Redis / Nacos。

- Provider 分支：已绑定直登不变；未绑定抛 `binding_required` 且 ticket 已写入（mock TicketStore）。
- 微信查找顺序：本渠道 openid 命中直登；unionid 命中补绑本渠道 openid 直登（不重复验手机号）；
  四渠道（小程序 / App / PC / H5）行为一致。
- social_bind：ticket 无效 / 验证码错 / 黑名单 / 冻结 / 幂等重绑 / 同 provider 异身份冲突 /
  正常挂接（微信按 channel 写入对应 openid 字段 + unionid）。
- 归并：影子判定（DISABLED_PASSWORD + 无手机号）；绑定迁移完整性（含 Apple 密文）；
  A 同 provider 冲突拒绝；B 冻结拒绝；删除事件发布；AFTER_COMMIT 踢下线调用。
- Account 域：`bindApple` 幂等 / 冲突语义。

## 9. 客户端改动（唯一外部依赖）

1. token 端点错误响应新增 `binding_required` 分支 → 跳绑手机页 → 调 `social_bind`；
2. `bindPhone` 响应新增 `merged` 字段 → 为 true 时提示并引导重新登录。

服务端兼容性：已绑定用户全链路无感；`bindPhone` 对非影子账号行为不变；
现存错误响应契约不变（failure handler 只拦截 `binding_required`）。

## 10. 实现偏差记录（2026-07-16 实现完成后回写）

- `social_bind` 的 token 端点错误契约：ticket 失效 → `error=invalid_bind_ticket`
  （客户端重走第三方授权）；验证码错误 / 冻结 / 黑名单 / 冲突 → `error=invalid_grant`
  （`error_description` 携带业务文案）。
- 微信绑定兼容性检查比 spec 更严：除同渠道异 openid 外，**unionid 不同主体**也拒绝
  （防止两个不同微信号挂到同一账号）。
- `bindPhone` 接口从 204 无响应体改为 200 `{"merged": bool}`。
- 微信查找顺序统一为「本渠道 openid → unionid」（原 Web 流程为 unionid 优先，
  行为差异仅出现在脏数据场景，唯一索引兜底后不可达）。

## 11. 非目标

- 不拆 `auth_account_binding` 独立绑定表；
- 不重构网页扫码 / 公众号 H5 的浏览器 session 流程：其已有 `/login/bind-phone`
  引导页，配合 §5 的归并逻辑达成最终一致。「未绑手机的 web session 禁止完成
  OIDC 授权」的硬约束（受限 authority）留待二期评估；
- 小程序 `getPhoneNumber` 一键绑定（`wx_phone_code` 凭证分支）留待小程序接入时实现；
- 不做实账号之间的合并；
- 不提供第三方解绑接口（现状没有，本期不加）。
