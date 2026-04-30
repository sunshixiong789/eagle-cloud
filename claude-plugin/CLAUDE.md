# Eagle Cloud 开发规范（Plugin 注入）

> 本文件由 `eagle-cloud` Claude Code Plugin 自动注入业务项目。
> 业务项目自身的 `CLAUDE.md` 仍然生效，本文件作为补充指引。
>
> Plugin 通过 Git Marketplace 分发，支持 **Gitee / GitHub / GitLab / Gitea**。
> 接入方式见 `README.md` 与 `DEPLOYMENT.md`。

## 项目栈定位

业务项目依赖 `eagle-cloud` 基础架子（BOM + 22 个 starter），遵循以下技术栈与约定：

- **Java 25** / Gradle 8.x（Groovy DSL）
- **Spring Boot 4.0.3** / Spring Cloud 2025.1.1 / Spring Cloud Alibaba 2025.1.0.0
- **Spring Modulith 2.0.5** — 模块化单体边界静态验证
- **DDD + 六边形架构**（领域层稳定，infrastructure 可拆分微服务）
- **Hibernate 7.2.6** / MySQL / PostgreSQL / Druid
- **Spring Security + OAuth2 Resource Server**（业务服务，授权服务器在 eagle-system-server）

## PR 前必跑（速查）

```bash
./gradlew clean build
./gradlew :path:to:module:test --tests "*.ModulithArchitectureTest"   # 涉及模块化代码
./gradlew :path:to:module:test
```

或一键：`/check-arch`。

## 开发规范（按场景查阅 rules/）

| 文件                                    | 适用场景                                          |
|---------------------------------------|-----------------------------------------------|
| `rules/01-naming.md`                  | 命名约定（DDD 组件、ErrorCode、Modulith）               |
| `rules/02-code-style.md`              | Google Java Style + Lombok + `@NullMarked`    |
| `rules/03-architecture.md`            | DDD 分层、Port/Adapter、聚合根创建型事件                  |
| `rules/04-modulith.md`                | `@ApplicationModule` / `@NamedInterface` 边界治理 |
| `rules/05-api.md`                     | RESTful URL、`@PreAuthorize`、CORS、响应格式         |
| `rules/06-database.md`                | JPA 实体、跨聚合 ID 引用、索引、CQRS 投影                   |
| `rules/07-exception.md`               | AppException 体系、ErrorCode 工厂方法                |
| `rules/08-concurrency.md`             | 事务、领域事件 `@Async + AFTER_COMMIT`、缓存失效          |
| `rules/09-testing.md`                 | JUnit 5 + Mockito、AAA、命名、覆盖要求                 |
| `rules/10-starter.md`                 | `@AutoConfiguration` + Properties + imports   |
| `rules/11-feign.md`                   | FeignClient 位置、错误处理、`@SpringQueryMap`         |
| `rules/12-security.md`                | OAuth2 / JWT、密码、敏感字段脱敏、审计                     |
| `rules/13-logging.md`                 | SLF4J 占位符、MDC、核心操作埋点                          |
| `rules/14-cache.md`                   | Redis+Caffeine、Key 命名、击穿/穿透/雪崩防护              |
| `rules/15-messaging.md`               | RocketMQ Topic、幂等、死信、事务消息                     |
| `rules/16-transaction-distributed.md` | Seata AT/TCC、本地消息表                            |
| `rules/17-tenant-permission.md`       | 多租户隔离、行级数据权限                                  |
| `rules/18-openapi.md`                 | SpringDoc 注解、版本、错误码文档化                        |
| `rules/19-config.md`                  | Properties、Nacos、profile、Jasypt 加密            |
| `rules/20-i18n.md`                    | messages 组织、key 规则                            |
| `rules/22-git.md`                     | 分支模型、Conventional Commits                     |
| `rules/23-performance.md`             | N+1、慢 SQL、连接池、Async 池                         |
| `rules/24-deployment.md`              | Dockerfile、K8s、健康检查、优雅停机                      |
| `rules/25-review-checklist.md`        | **PR 前完整自检清单**                                |
| `rules/26-file-storage.md`            | MinIO Bucket、Key 设计、上传校验                      |
| `rules/27-scheduling.md`              | XXL-JOB 路由、分片、幂等                              |
| `rules/28-migration.md`               | Flyway 命名、不可变、回滚                              |
| `rules/30-dependency.md`              | Gradle 范围、BOM、CVE                             |

## Starter 使用（按需 skill 加载）

22 个 starter 各有独立 skill，AI 在编码时会按场景自动加载相关 skill。手动列表见 `skills/` 目录：

| Skill                      | 何时触发                        |
|----------------------------|-----------------------------|
| `eagle-common`             | DDD 基类、异常、领域事件、分布式锁接口       |
| `eagle-data-jpa`           | JPA Auditing + Hibernate 配置 |
| `eagle-mybatis`            | MyBatis-Plus 增强             |
| `eagle-dynamic-datasource` | 主从读写分离                      |
| `eagle-elasticsearch`      | ES 检索 / 聚合 / 高亮             |
| `eagle-redis`              | 缓存 / 锁 / 限流 / 布隆            |
| `eagle-rocketmq`           | 事件发布 / 事务消息 / 死信            |
| `eagle-id-generator`       | 雪花 / TSID / NanoId / 业务单号   |
| `eagle-idempotency`        | 接口幂等                        |
| `eagle-tenant`             | 多租户上下文                      |
| `eagle-row-security`       | 行级数据权限                      |
| `eagle-resource-server`    | OAuth2 资源服务器                |
| `eagle-feign-client`       | Feign + 自动透传                |
| `eagle-tracing`            | 链路追踪                        |
| `eagle-openapi`            | SpringDoc 3                 |
| `eagle-oss-minio`          | 对象存储                        |
| `eagle-notification`       | 短信 / 邮件 / 站内信               |
| `eagle-payment`            | 支付宝 / 微信支付                  |
| `eagle-scheduler`          | XXL-JOB                     |
| `eagle-seata`              | 分布式事务                       |
| `eagle-sentinel`           | 限流 / 熔断                     |
| `eagle-websocket`          | WS / SSE / 离线消息             |

## 项目级 Commands

| 命令                | 作用                                                     |
|-------------------|--------------------------------------------------------|
| `/eagle-flow`     | **启动 6 阶段端到端流程**(自然语言"做一个新功能"等价触发)               |
| `/check-arch`     | Modulith 架构验证 + 模块测试 + 全量构建一键检查                        |
| `/new-module`     | 按 DDD 模板创建新业务模块                                        |
| `/new-aggregate`  | 创建聚合根全栈骨架                                              |
| `/new-starter`    | 按 Spring Boot 4 模板创建新 starter                          |
| `/add-error-code` | 在 ErrorCode 枚举追加常量并同步 i18n 三语翻译                        |

## 端到端开发流程(eagle-feature-flow skill)

主干用 **Superpowers 6 阶段**,在 **规划** 与 **写代码** 阶段嵌入式调用本 plugin 的 rules / commands / starter skills:

| 阶段 | 名称         | 主干调用                                          | claude-plugin 注入                                                            |
|----|------------|-----------------------------------------------|---------------------------------------------------------------------------|
| 1  | Brainstorm | `superpowers:brainstorming`                   | (无,聚焦需求澄清)                                                                |
| 2  | Plan       | `superpowers:writing-plans`                   | ★ 必读相关 `rules/*` + 在 plan 中预定要触发的 commands(`/new-module` 等)               |
| 3  | TDD        | `superpowers:test-driven-development`         | ★ 加载相关 starter skills(eagle-common / eagle-rocketmq 等) + 触发 plan 中的 commands |
| 4  | Verify     | `superpowers:verification-before-completion`  | ★ 强制 `/check-arch`                                                        |
| 5  | Review     | `superpowers:requesting-code-review`          | ★ 对照 `rules/25-review-checklist.md`(16 大类自检)                              |
| 6  | Finish     | `superpowers:finishing-a-development-branch`  | 按 `rules/22-git.md` 整理 commit + PR 描述                                     |

**设计哲学**:Superpowers 提供工程纪律(brainstorm → plan → TDD → verify → review → finish),
本 plugin 提供 Eagle 平台的"约束"(rules)和"工具箱"(commands + per-starter skills),后者在主流程的关键节点被嵌入式调用。

详见 `skills/eagle-feature-flow/SKILL.md`。手动触发:在对话中说"按 eagle flow 走"或描述一个非 trivial 的功能需求即可。

## 重要约定（高频陷阱）

1. **审计字段名**：`createBy / updateBy / createTime / updateTime`（**不是** `createdBy/updatedBy/createdAt/updatedAt`）
2. **AsyncConfig Bean 名**：`taskExecutor`（**不是** `eagleTaskExecutor`）
3. **TenantContextHolder API**：`getTenantId() / setTenantId() / clear()`（**不是** `getCurrentTenantId`）
4. **RocketMQ 消费者**：继承 `AbstractRocketMqListener<T>` + 实现 `getTopic()/getEventClass()/handle(T event)`，**不用**
   `@RocketMQMessageListener` 注解。**构造器必须显式声明并调用 `super(rocketMqProperties)`**（基类已改为构造器注入），
   因此**子类不能再用 `@RequiredArgsConstructor`**——Lombok 生成的构造器不会调用带参 super。
   `AbstractDlqListener` 同此约束
5. **DistributedLock API**：`tryLock(key, long waitSec, long leaseSec, Supplier)`，参数是 **`long` 秒**不是 `Duration`
6. **CacheProtectionUtil**：`getWithMutex(key, ttl, loader, type)` — **4 参数**含返回类型 `Class<T>`
7. **DataScope 枚举**：`ALL / SELF / DEPT / DEPT_AND_CHILD / CUSTOM`（**不是** `DEPT_ONLY/SELF_ONLY`）
8. **多个 starter 默认 disabled**：`eagle.tenant.enabled`、`eagle.datasource.enabled` 默认 `false`
9. **生产环境必须改的默认值**：`eagle.storage.type` 默认 `local`、`eagle.tracing.sampling-probability` 默认 `1.0`
