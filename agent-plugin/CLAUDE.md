# Eagle Cloud 开发规范（Plugin 注入）

> 本文件由 `eagle-cloud` Claude Code Plugin 自动注入业务项目。
> 业务项目自身的 `CLAUDE.md` 仍然生效，本文件作为补充指引。
>
> Plugin 通过 Git Marketplace 分发，支持 **Gitee / GitHub / GitLab / Gitea**。
> 接入方式见 `README.md` 与 `DEPLOYMENT.md`。

## 项目栈定位

业务项目依赖 `eagle-cloud` 基础架子（BOM + 19 个 starter），遵循以下技术栈与约定：

- **Java 25** / Gradle 8.x（Groovy DSL）
- **Spring Boot 4.0.6** / Spring Cloud 2025.1.1 / Spring Cloud Alibaba 2025.1.0.0
- **Spring Modulith 2.0.5** — 模块化单体边界静态验证
- **DDD + 六边形架构**（领域层稳定，infrastructure 可拆分微服务）
- **Hibernate 7.2.6** / MySQL / PostgreSQL / Druid
- **Spring Security + OAuth2 Resource Server**（业务服务，授权服务器是独立的 eagle-auth-service）

## PR 前必跑（速查）

```bash
./gradlew clean build
./gradlew :path:to:module:test --tests "*.ModulithArchitectureTest"   # 涉及模块化代码
./gradlew :path:to:module:test
```

或一键：`/check-arch`。

## 开发规范（按场景查阅）

后端项目查 `rules/`；前端项目查 `rules-frontend/`。

### 后端：`rules/`（Spring Boot 4 + DDD + Modulith）

| 文件                          | 适用场景                                                                 |
|-----------------------------|----------------------------------------------------------------------|
| `rules/00-core.md`          | **必看**：中文回答、禁 `@Value`、Lombok 分角色规则、DDD 命名、测试范围、依赖与 Git              |
| `rules/01-java25.md`        | **必看**：record / sealed / 模式匹配 / Gatherers / 虚拟线程 / ScopedValue    |
| `rules/02-architecture.md`  | DDD 分层、Port/Adapter、Modulith 边界、领域事件与集成事件契约、Saga                     |
| `rules/03-api-error.md`     | RESTful、`@PreAuthorize` 用法、异常体系、ErrorCode 号段、i18n、OpenAPI             |
| `rules/04-data.md`          | JPA 实体、禁物理外键、索引唯一性、事务与并发、线程池、Flyway 迁移                               |
| `rules/05-security.md`      | OAuth2/JWT 取当前用户、脱敏、多租户与数据权限、审计日志、日志规范                               |
| `rules/06-boot4.md`   | **必看**：Jackson 3 分包、`@AutoConfiguration`、`RestClient`、Security 7 DSL |
| `rules/07-checklist.md`     | **必看**：高频陷阱速查（Eagle 特有 API）+ 存量违例台账 + PR 前自检清单                      |
| `rules/08-quality.md`       | **必看**：规模红线、贫血模型、**优先用现成能力（不重复造轮子）**、抽象最小化、复用归属、各层厚度、AI 特有坏味道      |

缓存、消息队列、分布式事务、定时任务、对象存储、韧性等主题**不设常驻规则文件**，规范随对应 starter skill
（`eagle-redis` / `eagle-amqp` / `eagle-scheduler` / `eagle-oss-minio` / `eagle-resilience`）按需自动加载。

### 前端：`rules-frontend/`（React Web / React Native / Taro 多端）

业务结构三端统一（FSD-lite），差异集中在一份对照文件里，不再按平台分目录：

| 文件                                  | 适用场景                                                        |
|-------------------------------------|-------------------------------------------------------------|
| `rules-frontend/00-overview.md`     | 入口：平台判断 + 架构定位 + TL;DR 10 条                                |
| `rules-frontend/01-architecture.md` | FSD-lite 分层、角色矩阵、依赖方向、slice 边界与 public API、Barrel 平台策略      |
| `rules-frontend/02-conventions.md`  | 文件/函数命名、slice 词根、import 顺序、DTO / ViewModel / Props 类型三层     |
| `rules-frontend/03-state-data.md`   | React Query / Zustand / local state 边界、错误与 401、主题、副作用、i18n |
| `rules-frontend/04-platforms.md`    | **三端差异对照**：目录 / 路由 / 样式 / 别名 / 新增业务清单（Web、RN、Taro 一表看全）    |
| `rules-frontend/05-quality.md`      | 红线、反例速查 16 条、测试、性能预算、依赖校验、扩展信号                             |

**平台轨选择速查**：

- 项目根有 `vite.config.ts` / `webpack.config.js` 但**不是** Taro → Web 轨
- 项目依赖含 `expo` 或 `@react-navigation` → React Native 轨
- 项目依赖含 `@tarojs/taro` → Taro 轨

三条轨都先读 `01`–`03`（通用），再查 `04-platforms.md` 里对应平台的列。

## Starter 使用（按需 skill 加载）

**19 个 starter** 各有独立 skill（另有 eagle-feature-flow 手写 skill），AI 在编码时按场景自动加载。列表见 `skills/`：

| Skill                      | 何时触发                                      |
|----------------------------|-------------------------------------------|
| `eagle-common`             | DDD 基类、异常、领域事件、分布式锁接口                     |
| `eagle-data-jpa`           | JPA Auditing + Hibernate 配置               |
| `eagle-data-r2dbc`         | 响应式 R2DBC 持久化、BaseR2dbcAggregateRoot      |
| `eagle-sharding`           | 分库分表、ShardingSphere YAML 配置               |
| `eagle-redis`              | 缓存 / 锁 / 限流 / 布隆                          |
| `eagle-amqp`               | RabbitMQ 事件发布 / 消费 / 死信 / 消息幂等            |
| `eagle-id-generator`       | 雪花 / TSID / NanoId / 业务单号                 |
| `eagle-idempotency`        | 接口幂等                                      |
| `eagle-resource-server`    | OAuth2 资源服务器                              |
| `eagle-restclient`         | Servlet 服务 HTTP Service + 自动透传（阻塞 RestClient）|
| `eagle-webclient`          | WebFlux 服务 HTTP Service + 自动透传（响应式 WebClient）|
| `eagle-tracing`            | 链路追踪                                      |
| `eagle-openapi`            | SpringDoc 3                               |
| `eagle-oss-minio`          | 对象存储                                      |
| `eagle-scheduler`          | XXL-JOB                                   |
| `eagle-websocket`          | WS / SSE / 离线消息                           |
| `eagle-resilience`         | 熔断器 / 重试 / 超时 / `@RateLimit`，Fallback     |
| `eagle-encrypt`            | 字段级加密，@Convert 注解                         |
| `eagle-audit-log`          | 操作审计日志，@AuditLog                          |

**已移除的 9 个 starter**（skill 已同步删除，不要再引用）：
`tenant`（多租户）、`rocketmq`（→ `amqp`）、`dynamic-datasource`（读写分离）、`elasticsearch`、
`excel`、`notification`（短信/邮件）、`seata`（分布式事务）、`sentinel`（限流熔断 → `resilience`）、`ai`。

对应能力的替代方案见 `rules/07-checklist.md` 陷阱 5 / 11 / 23 与 `rules/05-security.md`。

## 项目级 Commands

| 命令                | 作用                                 |
|-------------------|------------------------------------|
| `/eagle-flow`     | **启动 6 阶段端到端流程**(仅手动触发,不自动激活)     |
| `/check-arch`     | Modulith 架构验证 + 模块测试 + 全量构建一键检查    |
| `/new-module`     | 按 DDD 模板创建新业务模块                    |
| `/new-aggregate`  | 创建聚合根全栈骨架                          |
| `/new-starter`    | 按 Spring Boot 4 模板创建新 starter      |
| `/add-error-code` | 在 ErrorCode 枚举追加常量并同步 i18n 三语翻译    |
| `/verify-rules`   | 校验规则断言与代码实况一致，防规则腐烂          |
| `/new-adr`        | 新建架构决策记录（ADR），记录规则背后的"为什么"  |

## 端到端开发流程(eagle-feature-flow skill)

主干用 **Superpowers 6 阶段**,在 **规划** 与 **写代码** 阶段嵌入式调用本 plugin 的 rules / commands / starter skills:

| 阶段 | 名称         | 主干调用                                         | agent-plugin 注入                                                              |
|----|------------|----------------------------------------------|------------------------------------------------------------------------------|
| 1  | Brainstorm | `superpowers:brainstorming`                  | (无,聚焦需求澄清)                                                                   |
| 2  | Plan       | `superpowers:writing-plans`                  | ★ 必读相关 `rules/*` + 在 plan 中预定要触发的 commands(`/new-module` 等)                  |
| 3  | TDD        | `superpowers:test-driven-development`        | ★ 加载相关 starter skills(eagle-common / eagle-amqp 等) + 触发 plan 中的 commands |
| 4  | Verify     | `superpowers:verification-before-completion` | ★ 强制 `/check-arch`                                                           |
| 5  | Review     | `superpowers:requesting-code-review`         | ★ 对照 `rules/07-checklist.md`(高频陷阱 + 自检清单)                                 |
| 6  | Finish     | `superpowers:finishing-a-development-branch` | 按 `rules/00-core.md` 整理 commit + PR 描述                                        |

**设计哲学**:Superpowers 提供工程纪律(brainstorm → plan → TDD → verify → review → finish),
本 plugin 提供 Eagle 平台的"约束"(rules)和"工具箱"(commands + per-starter skills),后者在主流程的关键节点被嵌入式调用。

详见 `skills/eagle-feature-flow/SKILL.md`。**仅手动触发**:使用 `/eagle-flow [可选功能描述]`,
或在对话中显式说"按 eagle flow 走" / "启动 eagle flow"。普通需求描述(如"做一个新功能 / 加一个模块 / 重构 X")
不会自动进入 flow,按常规方式处理即可。

## 重要约定（高频陷阱）

完整的 22 条高频陷阱速查表见 **`rules/07-checklist.md`**（单一维护点，勿在此处重复）。
写代码前务必扫一遍——都是 Eagle 特有 API 与命名，凭直觉写必然出错，例如：

- 审计字段是 `createBy / createTime`，**不是** `createdBy / createdAt`
- `CacheProtectionUtil.getWithMutex(...)` 是 **4 参数**，最后一个是 `Class<T>`
- `DistributedLock.tryLock(...)` 收 **`long` 秒**，不是 `Duration`
- Jackson 核心类在 `tools.jackson.*`，注解仍在 `com.fasterxml.jackson.annotation.*`（详见 `rules/06-boot4.md`）
- 自定义 `SecurityFilterChain` 必须显式接 `EagleJwtAuthenticationConverter`，否则 `hasRole` 静默全废
- **不存在** `eagle.xxx.enabled` 总开关，starter 引入即生效
