# Eagle Cloud Plugin 团队使用指南

> 面向**业务项目开发者**(plugin 使用方)的日常上手文档。**同时适用 Claude Code 与 Codex CLI**——下文 `/plugin` 命令为
> Claude 会话内语法，Codex 用 `codex plugin …` shell 命令，行为等价。
> 如果你是 plugin 维护者(改 rules / 加 starter skill),请看 `README.md` 与 `DEPLOYMENT.md`。

## 目录

1. [5 分钟快速开始](#5-分钟快速开始)
2. [核心概念](#核心概念)
3. [三种使用层次](#三种使用层次)
4. [典型场景走读](#典型场景走读)
5. [触发短语速查](#触发短语速查)
6. [Slash Commands 速查](#slash-commands-速查)
7. [Starter Skills 索引](#starter-skills-索引)
8. [PR 前自检](#pr-前自检)
9. [常见问题 FAQ](#常见问题-faq)
10. [故障排查](#故障排查)

---

## 5 分钟快速开始

### 第 1 步:安装 plugin(强制)

**Claude Code**（会话内）：

```
/plugin marketplace add https://github.com/sunshixiong789/eagle-cloud.git
/plugin install eagle-cloud@eagle-cloud
```

**Codex CLI**（shell）：

```bash
codex plugin marketplace add https://github.com/sunshixiong789/eagle-cloud.git
codex plugin install eagle-cloud@eagle-cloud
```

> 第一条命令每台机器只需运行一次（全局生效）。
> 第二条命令每个项目运行一次。
> 安装后重启会话即可生效。

### 第 2 步:安装 Superpowers(推荐,工程纪律)

**Claude Code**：

```
/plugin marketplace add https://github.com/obra/superpowers.git
/plugin install superpowers@superpowers
```

**Codex CLI**：

```bash
codex plugin marketplace add obra/superpowers
codex plugin install superpowers@superpowers
```

### 第 3 步:验证安装

启动 Claude Code 或 Codex CLI,在会话中输入:

```
/check-arch
```

应识别为 Eagle plugin 的命令(若提示"未知命令",见[故障排查](#故障排查))。

### 第 4 步:试用一次完整流程

`eagle-feature-flow` 改为**仅手动触发**,只能由 slash command 或显式短语启动。直接对 Claude 说:

```
/eagle-flow 用户积分功能
```

或:

```
按 eagle flow 走,做一个用户积分功能
```

模型会启动 `eagle-feature-flow` → 按 6 阶段走完(Brainstorm → Plan → TDD → Verify → Review → Finish)。

> 仅说"我要给系统加一个用户积分功能"**不会**进入 6 阶段流程,模型会按常规方式处理;
> 如果希望走完整流程,**必须**显式触发。

**就这样,你就能用了。**

---

## 核心概念

| 概念                           | 是什么                                                                                                             | 何时用            |
|------------------------------|-----------------------------------------------------------------------------------------------------------------|----------------|
| **eagle-cloud plugin**       | 本仓库提供的 **agent plugin（同时支持 Claude Code + Codex CLI）**,含 30 份规则(rules)、6 个项目命令(commands)、23 个 starter / 编排 skill | 写 Eagle 平台代码时  |
| **Superpowers plugin**       | 第三方 plugin,提供工程纪律 skill(brainstorming / writing-plans / TDD / verification / code-review / finishing-branch)    | 做非 trivial 变更时 |
| **eagle-feature-flow skill** | 本 plugin 的"编排型 skill",把 Superpowers 的 6 阶段 + Eagle 的 rules/commands/starter skills 串起来                          | 启动新功能、新模块、重构时  |

**它们的关系**:

```
你显式触发(/eagle-flow 或 "按 eagle flow 走")
    ↓
eagle-feature-flow 激活(本 plugin) ← 仅手动触发,不识别普通需求短语
    ↓
按 6 阶段调用 Superpowers,每个阶段嵌入 Eagle 知识
    ├─ Phase 1 Brainstorm → superpowers:brainstorming
    ├─ Phase 2 Plan       → superpowers:writing-plans   + 读 rules/* + 决策 commands
    ├─ Phase 3 TDD        → superpowers:tdd             + 加载 starter skills + 触发 commands
    ├─ Phase 4 Verify     → superpowers:verification    + /check-arch
    ├─ Phase 5 Review     → superpowers:code-review     + rules/07-checklist.md
    └─ Phase 6 Finish     → superpowers:finishing-branch
```

---

## 三种使用层次

按团队对一致性的要求,从轻到重选一种:

### 层次 1:Plugin 自动注入(最轻量)

**只装 eagle-cloud plugin**,不装 Superpowers,不主动触发任何流程。

模型在以下情况自动起作用:

| 情况            | 模型自动做                                |
|---------------|--------------------------------------|
| 启动会话          | 读 plugin 的 CLAUDE.md → 知道 Eagle 平台规范 |
| 写 AMQP 代码    | 自动激活 `eagle-amqp` skill,使用正确 API   |
| 写缓存 / 锁代码     | 自动激活 `eagle-redis` skill             |
| 用户问"怎么做幂等"    | 自动激活 `eagle-idempotency` skill       |

**适合**:个人项目、小修小补、不要求严格流程。

**例子**:

```
> 帮我在 OrderService 加一个订单创建后跨服务通知

[模型]
1. 读 CLAUDE.md → 知道用 DDD + 模块化单体
2. 涉及跨服务集成事件发布 → 自动加载 eagle-amqp skill
3. 用 DomainEventPublisher.publish() API 实现 → 写测试 → 写实现
```

但模型 **不会** 主动跑 `/check-arch`、不会主动对照 review-checklist。这些得你手动要求。

---

### 层次 2:手动组合 commands + skills(中等)

**装 plugin + Superpowers**,**手动**调 superpowers skill 和 Eagle commands,自由组合。

**典型 session**:

```
> 用 superpowers brainstorming 帮我澄清"会员等级体系"需求
[模型走完 brainstorming]

> 现在写 plan,记得读 .claude/rules/02-architecture.md 和 03-data.md
[模型用 superpowers writing-plans + 读规则]

> 创建 membership 模块
> /new-module membership
[Eagle command 生成骨架]

> 在 membership 模块新增 MembershipLevel 聚合根
> /new-aggregate membership MembershipLevel
[Eagle command 生成聚合根全栈]

> 实现升级逻辑,用 TDD
[模型走完 superpowers:tdd]

> /check-arch
[Eagle command 跑架构验证]

> 对照 .claude/rules/07-checklist.md 帮我自评一遍
[模型对照清单]
```

**适合**:有经验的开发者、知道何时用哪个 skill / command。

---

### 层次 3:eagle-feature-flow 编排(最强约束,推荐团队)

**装 plugin + Superpowers**,通过 `/eagle-flow` 或显式短语**手动**启动 6 阶段固定流程
(不再因为"我要做新功能"等普通描述自动激活)。

**典型 session**:

```
> /eagle-flow 用户积分系统
  # 或: 按 eagle flow 走,做一个用户积分系统

[Eagle Flow] 启动 6 阶段流程

[Eagle Flow] Phase 1/6: Brainstorm
  调用 superpowers:brainstorming
  → 澄清:积分获取/消耗规则、过期、跨租户、与会员等级关系
  → 用户确认 ✅

[Eagle Flow] Phase 2/6: Plan
  调用 superpowers:writing-plans
  必读 rules: 03-architecture, 04-modulith, 06-database, 15-messaging, 17-tenant
  Plan 决策的 commands:
    - /new-module points
    - /new-aggregate points PointAccount
    - /new-aggregate points PointTransaction
    - /add-error-code PointErrorCode INSUFFICIENT_BALANCE
  → 用户 approve ✅

[Eagle Flow] Phase 3/6: TDD
  调用 superpowers:test-driven-development
  加载 skills: eagle-common, eagle-data-jpa, eagle-amqp, eagle-tenant
  执行 commands: /new-module points → /new-aggregate ×2 → /add-error-code
  逐 step 红→绿→重构
  → 测试全绿 ✅

[Eagle Flow] Phase 4/6: Verify
  调用 superpowers:verification-before-completion
  /check-arch ✅
  手动 API 验证 ✅

[Eagle Flow] Phase 5/6: Review
  调用 superpowers:requesting-code-review
  对照 rules/07-checklist.md
  → 发现 3 处 logging 不规范,修复

[Eagle Flow] Phase 6/6: Finish
  调用 superpowers:finishing-a-development-branch
  整理 commits, 写 PR 描述, push

[Eagle Flow] 完成 ✅
```

**适合**:多人团队、需要 PR 自动达标、新人快速上手。

---

## 典型场景走读

### 场景 A:做新功能(用 L3)

```
> /eagle-flow 在订单系统加一个发票申请功能,用户支付后可以申请开发票
```

(或先说需求再追加"按 eagle flow 走"。注意:仅描述需求不会自动激活 flow,必须显式触发。)

模型按 6 阶段走。Phase 2 会读:

- `../.agents/rules/02-architecture.md`(发票算独立聚合根还是订单子实体?)
- `../.agents/rules/03-api-error.md`(URL 设计)
- `../.agents/rules/05-security.md`(开票涉及敏感信息脱敏)

Phase 3 会触发:`/new-aggregate order Invoice` 或 `/new-module invoice`(取决于 Phase 2 决策)。

---

### 场景 B:重构(用 L3)

```
> /eagle-flow 把 OrderService 里的支付逻辑抽出来,做成支付适配器
```

(显式触发后)Phase 1 澄清:是否改外部行为?是否引入新 starter?

Phase 2 plan 重点:

- 读 `../.agents/rules/02-architecture.md` 的 Port/Adapter 章节
- 决定 Port 接口放在 `payment::port`,加 `@NamedInterface`

Phase 3 跳过 commands(纯重构),直接 TDD:先确保测试覆盖现有行为 → 重构期间测试保持绿。

---

### 场景 C:Bug 修复(用 L1 即可)

```
> Order.markPaid() 在并发场景下偶尔报 OptimisticLockingFailureException
```

L1 模式即可:模型读 CLAUDE.md → 知道 Eagle 用乐观锁(`@Version`)→ 加载 `eagle-common` skill → 给出"应用层重试"或"改悲观锁"
两种方案。

不需要 6 阶段流程。

---

### 场景 D:小修小补(零流程)

```
> 把 OrderController 的日志级别从 info 改成 debug
```

直接改即可。模型最多读 `../.agents/rules/05-security.md` 校对一下。

---

## 触发短语速查

**`eagle-feature-flow` 仅手动触发,不自动激活**。普通需求描述(如"做一个新功能 / 加一个模块 / 重构 X")
**不会**进入 6 阶段流程,按常规方式处理即可。

**仅以下显式入口可启动 flow**:

| 方式                  | 例子                                                       | 适合                                |
|---------------------|----------------------------------------------------------|-----------------------------------|
| **Slash command**   | `/eagle-flow 用户积分系统` / `/eagle-flow`(空参)                 | 任何人,推荐统一入口                        |
| **自然语言显式短语**        | `按 eagle flow 走` / `启动 eagle flow` / `走 eagle flow`     | 对话流中自然延续                          |
| **跨 session 恢复**    | `/eagle-flow continue` / `继续 eagle flow`                 | 上次未走完想接着走                         |

**绝不会自动激活的场景**(必须自己显式触发,否则按常规方式处理):

| 短语                                         | 处理方式                       |
|--------------------------------------------|----------------------------|
| "新功能 / 加一个 X / 加一个模块"                      | 常规开发请求;想走 flow → 加一句"按 eagle flow 走" |
| "新增聚合根 / 新增实体"                             | 直接 `/new-aggregate` 或常规实现  |
| "重构 X / 抽取 / 拆分"                           | 常规重构;想走 flow → 显式触发        |
| "build feature / refactor / add aggregate" | 同上,英文不再作为自动触发短语            |
| "改一下日志 / 调整配置 / 改个名字"                      | 直接改,与 flow 无关               |
| "为什么 X 报错"                                 | Bug 调试,与 flow 无关            |
| "解释一下 OrderService"                        | 代码理解,与 flow 无关              |

---

## Slash Commands 速查

本 plugin 提供的命令(全部在 `agent-plugin/commands/`):

| 命令                | 参数                               | 作用                                                                     |
|-------------------|----------------------------------|------------------------------------------------------------------------|
| `/eagle-flow`     | `[功能描述,可选]`                      | **启动 6 阶段端到端流程**(仅手动触发;等价短语:"按 eagle flow 走")                          |
| `/check-arch`     | `[模块路径,可选]`                      | Modulith 架构验证 + 模块测试 + 全量构建                                            |
| `/new-module`     | `<module-name>`                  | 按 DDD 模板创建新业务模块                                                        |
| `/new-aggregate`  | `<module> <aggregate-name>`      | 创建聚合根 + Repository + ErrorCode + ApplicationService + Controller + DTO |
| `/new-starter`    | `<starter-name>`                 | 按 Spring Boot 4 模板创建新 starter                                          |
| `/add-error-code` | `<enum-class> <CODE> <i18n-key>` | 在 ErrorCode 枚举追加常量 + i18n 三语翻译                                         |

**强烈建议**:在团队会议上每个命令演示一次,新人才会用。

---

## Starter Skills 索引

写代码涉及哪个 starter,对应 skill 自动激活(也可手动 invoke):

| 写到这类代码                       | 自动加载的 skill                |
|------------------------------|----------------------------|
| 聚合根 / 异常 / 事件 / `EagleUser`  | `eagle-common`             |
| JPA 实体 / 审计字段 / 索引           | `eagle-data-jpa`           |
| 多数据源主从分离                     | `eagle-dynamic-datasource` |
| Elasticsearch 检索             | `eagle-elasticsearch`      |
| Redis 缓存 / 分布式锁 / 限流 / 布隆    | `eagle-redis`              |
| RabbitMQ 发布 / 消费 / 死信          | `eagle-amqp`               |
| ID 生成(雪花/TSID/订单号)           | `eagle-id-generator`       |
| 接口幂等                         | `eagle-idempotency`        |
| 多租户隔离                        | `eagle-tenant`             |
| OAuth2 资源服务器(JWT 校验)         | `eagle-resource-server`    |
| RestClient 远程调用(自动透传)        | `eagle-feign-client`       |
| 链路追踪(Brave/Zipkin)           | `eagle-tracing`            |
| Swagger / OpenAPI 文档         | `eagle-openapi`            |
| MinIO 对象存储                   | `eagle-oss-minio`          |
| 短信 / 邮件 / 站内信                | `eagle-notification`       |
| XXL-JOB 定时任务                 | `eagle-scheduler`          |
| Seata 分布式事务                  | `eagle-seata`              |
| Sentinel 限流                  | `eagle-sentinel`           |
| WebSocket / SSE              | `eagle-websocket`          |

写 Eagle 代码前看一眼这张表,**手动 invoke 对应 skill 比凭记忆写更可靠**。

---

## PR 前自检

不论用哪种层次,**PR 前必须**:

```bash
# 1. 架构验证(必须 3/3 全绿)
/check-arch

# 2. 全量构建
./gradlew clean build

# 3. 涉及 UI 的特性 → 启动 dev server 在浏览器手动验证

# 4. 涉及 DB 变更 → 本地跑过 Flyway migration

# 5. 对照 06-checklist.md 自检 16 大类
```

L3 流程的 Phase 4 / Phase 5 已自动包含以上;L1 / L2 必须手动。

---

## 常见问题 FAQ

**Q1:Plugin 必装吗?Superpowers 必装吗?**

- Plugin **必装**(否则 AI 不知道 Eagle 规范、22 个 starter API)
- Superpowers **强烈推荐**(否则没有 TDD / verification / code-review 工程纪律,L3 也无法启动)

---

**Q2:不想被 6 阶段流程"绑架",能不能跳过?**

可以。三种方式:

1. 用层次 1 / 层次 2,**不触发** flow
2. 触发后明确说"跳过 Phase N"(skill 允许用户主动跳过)
3. 直接说"不要走 eagle flow,我自己来"

但**不推荐**对新人 / 团队成员关闭 — 自由的代价是 PR 反复打回。

---

**Q3:写代码时 starter skill 没自动激活怎么办?**

可能原因:

1. 描述不够明确 → 提及具体 starter 名(如"用 eagle-amqp 发个消息")
2. 模型选了别的 skill → 显式说"加载 eagle-amqp skill"
3. Plugin 没正确装 → 见[故障排查](#故障排查)

---

**Q4:Plugin 升级会破坏我的项目吗?**

Plugin 团队遵循向后兼容原则，breaking change 会提前在 `CHANGELOG.md` 说明。每次重启 Claude Code
会话会自动拉取最新版本。如需暂停升级，可临时卸载旧版并安装指定版本（需提前打 Tag 或记录 commit SHA，联系 Plugin 维护者）。

详见 `CHANGELOG.md`。

---

**Q5:能在 plugin 之外加自己项目的规则吗?**

可以,而且推荐。业务项目自己的 `CLAUDE.md`、`.claude/rules/`、`.claude/commands/` **都会生效**,与 plugin 内容并列。Plugin
提供"基线",项目可以扩展。

---

**Q6:eagle-feature-flow 的 6 阶段顺序能改吗?**

不建议。顺序是 rigid skill,基于工程实践经验设计:

- 跳 Brainstorm 直接 Plan → 需求没澄清,plan 是空中楼阁
- 跳 Plan 直接 TDD → 没有路线图,TDD 写飞了
- 跳 Verify 直接 Review → 自己都没跑通,凭什么让人 review
- 跳 Review 直接 Finish → PR 反复打回

如果某阶段对场景不适用,可以跳过(如纯重构跳 Phase 3 的 scaffold);但**顺序不变**。

---

## 故障排查

### `/check-arch` 提示"未知命令"

**Claude Code 排查**:

```
# 1. 检查 plugin 是否已安装
/plugin list
# 应能看到 eagle-cloud

# 2. 如未安装
/plugin marketplace add https://github.com/sunshixiong789/eagle-cloud.git
/plugin install eagle-cloud@eagle-cloud

# 3. 重启 Claude Code 会话
```

**Codex CLI 排查**:

```bash
# 1. 检查 plugin
codex plugin list
# 或会话内 /plugins 浏览

# 2. 如未安装
codex plugin marketplace add https://github.com/sunshixiong789/eagle-cloud.git
codex plugin install eagle-cloud@eagle-cloud

# 3. 重启 Codex CLI
```

---

### Starter skill 不自动激活

**排查**:

1. 在会话中说:"列出当前激活的 plugins"
2. 应能看到 `eagle-cloud`(Plugin 列表) 和具体 skill 名
3. 如果没有,卸载并重装:

```
# Claude Code
/plugin remove eagle-cloud
/plugin install eagle-cloud@eagle-cloud

# Codex CLI
codex plugin remove eagle-cloud
codex plugin install eagle-cloud@eagle-cloud
```

---

### eagle-feature-flow 不触发

**先确认**:flow 已改为**仅手动触发**,普通需求描述("做一个新功能 / 加一个模块 / 重构 X")不会自动激活,
这是预期行为。如果你确实想启动 flow:

| 原因                 | 处理                                                          |
|--------------------|-------------------------------------------------------------|
| 只描述了需求,没显式触发       | 使用 `/eagle-flow` 或追加"按 eagle flow 走" / "启动 eagle flow"      |
| Superpowers 没装     | flow 依赖 superpowers 的 skill,必须装                             |
| 显式短语后仍未进入 flow     | 显式说"用 eagle-feature-flow skill 启动 6 阶段流程"                   |

---

### Plugin 与项目自定义规则冲突

业务项目自身的指令文件优先级 **高于** plugin 注入内容：

| 工具          | 业务项目优先                         | plugin 注入                                        |
|-------------|--------------------------------|--------------------------------------------------|
| Claude Code | `CLAUDE.md` + `.claude/rules/` | `agent-plugin/CLAUDE.md` + `../.agents/rules/` |
| Codex CLI   | `AGENTS.md` + `.codex/rules/`  | `agent-plugin/AGENTS.md` + `../.agents/rules/` |

如果发现规则打架:

1. 业务项目里改自己的规则即可(plugin 内容不会被覆盖,会**叠加**)
2. 不希望 plugin 某条规则生效 → 在业务项目 CLAUDE.md / AGENTS.md 显式声明"忽略 plugin/rules/XX-yy.md"

---

## 反馈与贡献

发现规则错漏 / starter API 描述不准 / 触发不灵敏:

1. **小问题**(错别字 / 补充示例)→ 在 `eagle-cloud` 仓库 `agent-plugin/` 直接 PR
2. **架构变更**(新增 rule、改变 flow 顺序、加新阶段)→ 先在 `#eagle-arch` 群讨论,再 PR
3. **新增 starter**:在 `eagle-starter/` 加新 starter → 在 `agent-plugin/skills/` 加对应 SKILL.md → 跑
   `./agent-plugin/sync.sh`

详见 `README.md` "维护流程"章节。

---

## 一句话总结

> **新项目接入,L3 配套 hook;日常使用,直接说话**。
>
> 想做新功能 → "我要做 X" → flow 自动启动 → 6 阶段跑完 → PR ready。
>
> 不想被流程绑架 → 用 L1 / L2,但 PR 前手动 `/check-arch`,别忘对照 `../.agents/rules/07-checklist.md`。
