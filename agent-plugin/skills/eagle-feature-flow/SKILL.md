---
name: eagle-feature-flow
description: Use when starting any non-trivial feature, refactor, or cross-module change in eagle-cloud projects — orchestrates Superpowers (brainstorming/plan/TDD/verification/code-review/finishing-branch) as the main backbone, and pulls in agent-plugin's Eagle knowledge (rules/* + slash commands like /new-module, /new-aggregate, /new-starter, /add-error-code, /check-arch + per-starter skills) at the planning and coding phases. Triggers on phrases like "新功能"/"加一个模块"/"新增聚合根"/"重构 X"/"build feature"/"refactor"/"add aggregate".
---

# eagle-feature-flow — Superpowers 主干 + Eagle plugin 知识库

> **设计哲学**:Superpowers 提供工程纪律(brainstorm → plan → TDD → verify → review → finish);
> agent-plugin 提供 Eagle 平台的"约束"(rules/*)和"工具箱"(commands + per-starter skills),
> 在 **规划阶段** 与 **写代码阶段** 被 superpowers 主流程"嵌入式调用",**不**替代主干。

## 何时使用

**适用场景(满足任一即触发)**:

- 用户表达"新做一个功能 / 加一个模块 / 新增聚合根 / 重构 X"
- 跨多模块 / 多聚合根 / 多服务的非 trivial 变更
- 涉及新增 starter、新增 ErrorCode、新增领域事件契约
- PR 前希望确保走完完整规范流程

**不适用场景**:

- 单文件改一行 typo / 注释 / 文档微调
- 已经在某阶段中,被中断后回来续接
- 临时探索 / 验证 / 调试,不打算合并到主干

## 执行约束(rigid skill,不可跳步)

1. **顺序固定**:阶段编号即执行顺序;失败必须修复后再进入下一阶段
2. **每阶段显式宣告**:进入新阶段时输出 `[Eagle Flow] Phase N/6: <name>` 一行
3. **TodoWrite 跟踪**:flow 启动时立刻创建 6 项 todo,完成一项 mark 一项
4. **agent-plugin 知识库注入**是**强制**的,不是建议 —— Phase 2 必须读 rules,Phase 3 必须加载 starter skill

## 6 阶段流程

```
Phase 1  Brainstorm    ← superpowers:brainstorming
Phase 2  Plan          ← superpowers:writing-plans          ★ 注入 Eagle rules + 决策 commands
Phase 3  TDD           ← superpowers:test-driven-development ★ 注入 Eagle starter skills + 触发 commands
Phase 4  Verify        ← superpowers:verification-before-completion ★ 强制 /check-arch
Phase 5  Review        ← superpowers:requesting-code-review  ★ 对照 rules/25-review-checklist.md
Phase 6  Finish        ← superpowers:finishing-a-development-branch
```

---

### Phase 1/6:Brainstorm — 澄清需求

**主干**:`superpowers:brainstorming`

**目的**:把模糊需求收敛到"做什么 / 不做什么 / 验收标准"

**通过条件**:

- 用户痛点 / 业务目标已明确
- 功能边界(in scope / out of scope)
- 验收标准(可测试)
- 主要不确定性 / 假设

**产出**:口头摘要,不落盘(下一阶段会落入 plan 文件)

**agent-plugin 介入**:无。本阶段聚焦需求,不引入实现细节约束。

---

### Phase 2/6:Plan — 写实现计划(★ 注入 Eagle 规范)

**主干**:`superpowers:writing-plans`

**目的**:把澄清后的需求落成"可执行、可校验"的步骤化 plan,**plan 必须以 Eagle 规范为约束写成**

**强制动作**:

**A. 必读以下 rules**(写 plan 前先读,把约束体现在 plan 里):

| 涉及           | 必读规则                                                |
|--------------|-----------------------------------------------------|
| 新模块 / 跨模块    | `rules/03-architecture.md` + `rules/04-modulith.md` |
| 新聚合根 / 实体    | `rules/03-architecture.md` + `rules/06-database.md` |
| 新接口          | `rules/05-api.md` + `rules/18-openapi.md`           |
| 新错误码         | `rules/07-exception.md` + `rules/20-i18n.md`        |
| 新事件 / MQ 消费  | `rules/15-messaging.md` + `rules/08-concurrency.md` |
| 新缓存 / 锁      | `rules/14-cache.md`                                 |
| 多租户 / 数据权限   | `rules/17-tenant-permission.md`                     |
| 涉及金额 / 分布式事务 | `rules/16-transaction-distributed.md`               |
| 新 starter    | `rules/10-starter.md`                               |
| 新定时任务        | `rules/27-scheduling.md`                            |
| DB 变更        | `rules/28-migration.md`                             |

不确定要读哪些 → 读 `rules/25-review-checklist.md`(汇总索引)。

**B. 在 plan 中预先决定要触发哪些 Eagle commands**:

| 需求         | Command                               | 何时用                       |
|------------|---------------------------------------|---------------------------|
| 新增业务模块     | `/new-module {name}`                  | 引入新有界上下文                  |
| 新增聚合根      | `/new-aggregate {module} {name}`      | 已有模块内加新聚合根全栈骨架            |
| 新增 starter | `/new-starter {name}`                 | 抽取通用能力(谨慎,需架构组同意)         |
| 新增错误码      | `/add-error-code {enum} {code} {key}` | 业务需要新 ErrorCode + i18n 三语 |

**C. plan 文件格式**(写在 `.superpowers/plans/{slug}.md` 或 PR 描述草稿):

```markdown
# Plan: {feature-name}

## Goal
<一句话说明目标>

## Eagle Rules Applied(本次依据的规范)
- rules/03-architecture.md(跨域 Port/Adapter)
- rules/15-messaging.md(MQ 幂等)
- ...

## Steps
1. [ ] /new-module points         (依据 rules/04-modulith.md)
2. [ ] /new-aggregate points PointAccount  (依据 rules/06-database.md)
3. [ ] /add-error-code PointErrorCode INSUFFICIENT_BALANCE error.point.insufficient_balance
4. [ ] 实现 PointAccount.debit() 状态机 + UT
5. [ ] 实现 RocketMQ 消费者 + Inbox 表 + UT(rules/15-messaging.md 的支付级强一致)
6. [ ] /check-arch
7. [ ] 集成测试 / 手工验证
```

**通过条件**:plan 落盘 + 用户**显式 approve**(口头"OK / 继续 / 同意");Eagle rules 索引已写入 plan 头部。

---

### Phase 3/6:TDD — 测试驱动实现(★ 注入 Eagle starter skills)

**主干**:`superpowers:test-driven-development`

**目的**:用测试驱动每一步实现,达到 80%+ 覆盖,代码符合 Eagle 规范

**强制动作**:

**A. 按 plan 步骤逐项执行,每步内部走 红 → 绿 → 重构 循环**

**B. 触发 plan 中预定的 Eagle commands**(Phase 2 已规划好,这里执行):

```
1. /new-module points          ← 生成模块骨架
2. /new-aggregate points PointAccount   ← 生成聚合根全栈
3. /add-error-code PointErrorCode ...   ← 加错误码 + i18n
```

**C. 按需加载 Eagle starter skills**(模型在写涉及对应 starter 的代码时自动激活,但本 flow 必须显式确认加载):

| 写到这类代码                       | 必须加载 skill                 |
|------------------------------|----------------------------|
| 聚合根 / 异常 / 事件 / `EagleUser`  | `eagle-common`             |
| JPA 实体 / 审计字段 / 索引           | `eagle-data-jpa`           |
| Redis 缓存 / 分布式锁 / 限流 / 布隆    | `eagle-redis`              |
| RocketMQ 发布 / 消费 / 事务消息 / 死信 | `eagle-rocketmq`           |
| ID 生成(雪花/TSID/订单号)           | `eagle-id-generator`       |
| 接口幂等                         | `eagle-idempotency`        |
| 多租户隔离                        | `eagle-tenant`             |
| 行级数据权限                       | `eagle-row-security`       |
| OAuth2 资源服务器                 | `eagle-resource-server`    |
| Feign 远程调用                   | `eagle-feign-client`       |
| MinIO 对象存储                   | `eagle-oss-minio`          |
| 短信 / 邮件 / 站内信                | `eagle-notification`       |
| 支付宝 / 微信支付                   | `eagle-payment`            |
| XXL-JOB 定时任务                 | `eagle-scheduler`          |
| Seata 分布式事务                  | `eagle-seata`              |
| Sentinel 限流                  | `eagle-sentinel`           |
| WebSocket / SSE              | `eagle-websocket`          |
| 链路追踪                         | `eagle-tracing`            |
| Swagger / OpenAPI            | `eagle-openapi`            |
| MyBatis-Plus(可选,与 JPA 二选一)   | `eagle-mybatis`            |
| 多数据源                         | `eagle-dynamic-datasource` |
| Elasticsearch                | `eagle-elasticsearch`      |

每个 skill 内含 starter 的 API、配置、典型用法、陷阱。**写涉及该 starter 的代码前必须先 invoke 对应 skill**,而不是凭记忆写。

**D. 测试规范**(`rules/09-testing.md`):

- JUnit 5 + Mockito + AAA 结构
- `@Nested` + `@DisplayName` 分组
- 覆盖正常路径 / 边界 / 异常路径
- 不连真实 DB / 网络 / 文件
- 命名 `should{行为}When{前提}`

**红线**:**禁止**先写实现再补测试。发现已有未覆盖的实现,先停下补测试再继续。

**通过条件**:plan 中的 steps 全部 `[x]`;模块单元测试全绿。

---

### Phase 4/6:Verify — 验证(★ 强制 `/check-arch`)

**主干**:`superpowers:verification-before-completion`

**目的**:用命令证明"声称完成的工作真的完成",不是看代码

**强制动作**:

1. **`/check-arch`**(agent-plugin 提供) — Modulith 静态验证 + 模块测试 + 全量构建,3/3 全绿
    - Modulith 违规 → 按 `rules/04-modulith.md` 加 `@NamedInterface` / 改 `allowedDependencies` / 重构 Port-Adapter
    - 编译失败 → 调用 `everything-claude-code:java-build-resolver` agent 修
    - 测试回归 → 回到 Phase 3 修测试和实现
2. **`./gradlew clean build`** 通过(`/check-arch` 内部已包含)
3. **涉及 UI** 的特性已手工启动 dev server 在浏览器验证(单元测试不能替代 UI 验证)
4. **涉及 DB** 的变更已在本地数据库跑过 Flyway migration,确认能 up 也能(若有)down
5. **关键路径有日志埋点**(对照 `rules/13-logging.md` 的"核心操作必须埋点"清单)

**通过条件**:所有验证命令绿;手动验证场景有截图或描述。

---

### Phase 5/6:Review — 自评审(★ 对照 25-review-checklist)

**主干**:`superpowers:requesting-code-review`

**目的**:在请求他人 review 前,自己先按 Eagle PR checklist 完整对照

**强制动作**:

1. 打开 `agent-plugin/rules/25-review-checklist.md`,逐项对照(命名 / 架构 / API / 数据库 / 异常 / 日志 / 并发 / 测试 /
   Starter / Feign / 安全 / 缓存 / 消息 / 多租户 / 配置 / 性能,共 16 大类)
2. 发现的问题 → 修复(回到 Phase 3 局部迭代)或显式记入 PR 描述的"已知风险与跟进项"
3. 可选:调用 `everything-claude-code:java-reviewer` agent 做第二轮自动评审

**通过条件**:checklist 16 大类全部对齐;评审发现项已修复或归档。

---

### Phase 6/6:Finish — 收尾

**主干**:`superpowers:finishing-a-development-branch`

**目的**:决定如何收尾这条开发分支(直接合并 / 拆分 PR / 暂存继续 / ...)并执行

**动作**:

- 整理 commit(按 `rules/22-git.md` 的 Conventional Commits 与原子提交规范)
- 写 PR 描述(模板见 `rules/25-review-checklist.md` 末尾)
- push + open PR + 分配 reviewer

**最后输出**:flow 总结报告

- 涉及模块 / 聚合根 / starter
- 新增测试数量、覆盖率变化
- 新增 / 变更的公共契约(API / 事件 / Port)
- 后续跟进项

---

## 触发示例

### 示例 1:用户说"我要做一个用户积分系统"

```
[Eagle Flow] 启动 6 阶段流程

Phase 1/6: Brainstorm
  → 调用 superpowers:brainstorming
  ...(澄清:积分获取/消耗规则、过期、跨租户、与会员等级关系)

Phase 2/6: Plan
  → 调用 superpowers:writing-plans
  → 必读 rules: 03-architecture, 04-modulith, 06-database, 15-messaging, 17-tenant
  → plan 包含: /new-module points, /new-aggregate ×2, /add-error-code, RocketMQ Inbox 模式
  → 用户 approve ✅

Phase 3/6: TDD
  → 调用 superpowers:test-driven-development
  → 加载 skills: eagle-common, eagle-data-jpa, eagle-rocketmq, eagle-tenant
  → 触发 commands: /new-module points → /new-aggregate ×2 → /add-error-code
  → 逐 step 红→绿→重构

Phase 4/6: Verify
  → /check-arch ✅
  → 手动 API 验证 ✅

Phase 5/6: Review
  → 对照 rules/25-review-checklist.md
  → 发现 3 处 logging 不规范,修复

Phase 6/6: Finish
  → 调用 superpowers:finishing-a-development-branch
  → 整理 commits, 写 PR 描述, push

[Eagle Flow] 完成 ✅
```

### 示例 2:用户说"重构 OrderService,提取支付适配器"

```
Phase 1: Brainstorm(确认重构范围 / 不改外部行为)
Phase 2: Plan
  → 必读 rules: 03-architecture(Port/Adapter), 11-feign(若适配器是 Feign), 25-checklist
  → plan 不需要 /new-module(纯重构),可能需要在 payment::port 加 @NamedInterface
Phase 3: TDD
  → 加载 skills: eagle-common, eagle-feign-client(若适配器走 Feign)
  → 先确保现有测试覆盖了所重构的行为,再做重构,期间测试必须保持绿
Phase 4: /check-arch
Phase 5: rules/25-review-checklist.md 自检
Phase 6: 收尾
```

---

## agent-plugin 知识库总览(供 Phase 2 / Phase 3 查阅)

| 资源类型               | 路径                                          | 用途                   |
|--------------------|---------------------------------------------|----------------------|
| **规范文档(必读)**       | `agent-plugin/rules/01-30*.md`              | Phase 2 写 plan 的约束输入 |
| **PR 自检清单**        | `agent-plugin/rules/25-review-checklist.md` | Phase 5 评审依据         |
| **Slash Commands** | `agent-plugin/commands/*.md`                | Phase 3 触发的脚手架命令     |
| **Starter Skills** | `agent-plugin/skills/eagle-*/SKILL.md`      | Phase 3 写代码时按需加载     |

主索引在 `agent-plugin/CLAUDE.md`。

---

## 与 Superpowers 的边界(避免概念混淆)

| Superpowers 提供                | agent-plugin 提供                             |
|-------------------------------|---------------------------------------------|
| 工程纪律(brainstorm/plan/TDD/...) | Eagle 平台领域知识(rules + commands + starter 用法) |
| 通用、不绑定项目                      | 项目专属、绑定 eagle-cloud 技术栈                     |
| 主干流程的 6 个阶段                   | 在 Phase 2 / 3 / 4 / 5 被 superpowers 主流程调用   |

**两者不冲突,是"通用工程纪律 × 平台专属约束"的组合。**

---

## 失败与中断

- **任意阶段失败**:停在当前阶段,不进入下一阶段。修复后从失败阶段继续,**不重启 flow**。
- **用户中断**:在当前阶段暂停,记录下次从该阶段恢复;TodoWrite 状态保留。
- **跨 session 恢复**:下次会话用户说"继续 eagle flow",读取 TodoWrite 中未完成的最低编号阶段恢复。

## 红线(必须避免)

| 反例                                                     | 为什么错                                                                                 |
|--------------------------------------------------------|--------------------------------------------------------------------------------------|
| 跳过 Phase 1 直接写 plan                                    | 需求没澄清,plan 是空中楼阁                                                                     |
| Phase 2 写 plan 不读 rules                                | plan 与 Eagle 规范脱节,Phase 5 review 时一片红                                                |
| Phase 3 写 RocketMQ 消费者却不 invoke `eagle-rocketmq` skill | 凭记忆写,API 错(常见:用了 `@RocketMQMessageListener` 注解,而 Eagle 用 `AbstractRocketMqListener`) |
| Phase 3 先写实现再补测试                                       | 违反 TDD,覆盖率注水                                                                         |
| Phase 4 跳过 `/check-arch`                               | Modulith 违规进 PR,合并后才发现                                                               |
| Phase 4 用"我看了一遍代码"代替跑命令                                | "已完成"必须跑命令证明                                                                         |
| Phase 5 不看 25-review-checklist                         | 评审标准不一致,PR 反复打回                                                                      |

## 配套 hook(可选,锁死关键节点)

如果团队希望关键阶段强制执行,在 `.claude/settings.json` 加 hook:

```json
{
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Bash",
        "matcherPattern": "git\\s+commit",
        "command": "./gradlew :check-arch --quiet || (echo '⚠️ /check-arch 未通过,Phase 4 必须先绿' && exit 1)"
      }
    ]
  }
}
```

(把 `exit 1` 改为 `exit 0` 即降级为警告而非阻断。)
