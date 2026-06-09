# AGENTS.md

本文件是 Codex 在本仓库工作时的主入口规则。Claude Code 专用说明见 `CLAUDE.md`；Eagle 平台的细则集中放在
`agent-plugin/rules/`，开发时必须按场景读取并遵守。

## 项目概述

Eagle Cloud 是一个基于 DDD + 六边形架构 + Spring Modulith 模块化单体构建的 Spring Boot 平台。系统设计为微服务拆分就绪：领域层通过
Port 接口隔离，未来拆分时优先替换 `infrastructure/` 层实现。

## 技术栈

- Java 25 / Gradle 8.x（Groovy DSL）
- Spring Boot 4.0.6 / Spring Cloud 2025.1.1 / Spring Cloud Alibaba 2025.1.0.0
- Spring Modulith 2.0.5
- Hibernate 7.2.6 / JPA / MySQL / PostgreSQL / Druid
- Spring Security + OAuth2 Authorization Server / Resource Server
- Lombok / SpringDoc OpenAPI 3.0.2
- Redisson / RocketMQ / XXL-JOB / Seata / MinIO

## 项目结构与模块组织

`settings.gradle` 定义主要模块：

- `eagle-bom`：依赖版本对齐。
- `eagle-services/`：服务应用。
- `eagle-starter/`：可复用 Spring Boot Starter。
- `eagle-doc/`：项目文档。
- `agent-plugin/`：Claude Code 插件、rules、commands 和 starter skills。
- `codex-plugin/`：Codex 插件源码。

各模块遵循标准目录：`src/main/java`、`src/main/resources`、`src/test/java`。Spring Boot 自动配置入口位于
`src/main/resources/META-INF/spring/`。

## 构建、测试与开发命令

当前仓库未提交 Gradle Wrapper，Codex 执行命令时使用本机 `gradle`，不要改用 `./gradlew`。

```bash
gradle build
gradle test
gradle :eagle-starter:eagle-websocket-starter:test
gradle :eagle-starter:eagle-rocketmq-starter:build
gradle dependencyUpdates
```

开发单个 starter 或服务时，优先运行受影响模块的任务获取快速反馈。跨模块或公共契约变更后运行 `gradle build`。
`dependencyUpdates` 来自 Ben Manes Versions 插件，用于检查依赖升级。

## DDD 与 Modulith 约定

业务模块内部遵循 `interfaces / application / domain / infrastructure` 四层，依赖方向为
`interfaces -> application -> domain <- infrastructure`。跨模块协作优先使用 Port、领域事件和 `@NamedInterface`
暴露的稳定接口，避免直接穿透其他模块内部实现。

`eagle-services/eagle-system-service` 中 `com.eagle.system` 按有界上下文划分模块，典型模块包括 `auth`、`base`、`config`、
`common`。涉及模块边界、Named Interface 或跨模块依赖时，先读取 `agent-plugin/rules/04-modulith.md`。

## 关键基类与基础设施

- `BaseAggregateRoot<T>`：聚合根，包含 ID、审计字段、`@Version` 乐观锁和 `registerEvent()` 事件能力。
- `BaseEntity`：子实体，包含审计字段和乐观锁，不承载领域事件能力。
- `BaseEvent`：领域事件，包含 time-ordered UUID `eventId` 和 `occurredOn`。
- `ErrorCode`：各领域错误码枚举实现该接口，并通过工厂方法创建 `AppException`。

## 编码风格与命名约定

包名保持在 `com.eagle.<area>` 下。类名使用清晰职责后缀，例如 `*AutoConfiguration`、`*Properties`、`*Service`、`*Repository`、
`*Gateway`、`*Request`、`*Result`。

Starter 模块应通过 `@AutoConfiguration` 和 properties 类暴露配置，并在
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册自动配置。保持现有 Java 4 空格缩进和简洁的
SLF4J 日志风格。

## 测试指南

测试基于 JUnit Platform 和 `spring-boot-starter-test`，Mockito 已在 Gradle 中配置为 JVM Agent。测试放在对应模块的
`src/test/java`，包结构与生产代码保持一致。测试类命名为 `*Test`。

Starter 工具类优先写聚焦单元测试，自动配置逻辑补充 Spring Context 测试。提交前至少运行受影响模块的 `test`；跨模块修改需运行
`gradle build`。

## 必须遵守的规则索引

修改代码、测试、Gradle、文档或发布配置时，先识别本次任务涉及的领域，只读取相关规则文件并遵守。不要把 `agent-plugin/rules/`
当作装饰性文档。

| 文件                                                 | 适用场景                                                         |
|----------------------------------------------------|--------------------------------------------------------------|
| `agent-plugin/rules/01-naming.md`                  | 命名约定：Java、DDD 组件、ErrorCode、Modulith。                         |
| `agent-plugin/rules/02-code-style.md`              | Java 风格、Lombok、`@NullMarked`、格式要求。                           |
| `agent-plugin/rules/03-architecture.md`            | DDD 分层、Port / Adapter、聚合根、领域事件。                              |
| `agent-plugin/rules/04-modulith.md`                | Spring Modulith、`@ApplicationModule`、`@NamedInterface`、模块边界。 |
| `agent-plugin/rules/05-api.md`                     | REST API、URL、鉴权、CORS、响应格式。                                   |
| `agent-plugin/rules/06-database.md`                | JPA 实体、跨聚合 ID 引用、索引、CQRS 投影。                                 |
| `agent-plugin/rules/07-exception.md`               | `AppException`、`ErrorCode`、异常映射。                             |
| `agent-plugin/rules/08-concurrency.md`             | 事务、领域事件、`@Async + AFTER_COMMIT`、缓存失效。                        |
| `agent-plugin/rules/09-testing.md`                 | JUnit 5、Mockito、测试命名、覆盖要求。                                   |
| `agent-plugin/rules/10-starter.md`                 | Spring Boot Starter、自动配置、Properties、imports。                 |
| `agent-plugin/rules/11-feign.md`                   | HTTP Service 客户端、错误处理、分页参数。                                  |
| `agent-plugin/rules/12-security.md`                | OAuth2、JWT、密码、敏感数据脱敏、审计。                                     |
| `agent-plugin/rules/13-logging.md`                 | SLF4J、MDC、异常日志、敏感字段脱敏。                                       |
| `agent-plugin/rules/14-cache.md`                   | Redis + Caffeine、Key 命名、TTL、缓存保护。                            |
| `agent-plugin/rules/15-messaging.md`               | RocketMQ Topic、幂等、死信、事务消息。                                   |
| `agent-plugin/rules/16-transaction-distributed.md` | Seata、本地消息表、分布式事务取舍。                                         |
| `agent-plugin/rules/17-tenant-permission.md`       | 多租户隔离、行级数据权限、跨租户操作。                                          |
| `agent-plugin/rules/18-openapi.md`                 | SpringDoc 注解、版本、错误码文档化。                                      |
| `agent-plugin/rules/19-config.md`                  | Properties、Nacos、profile、Jasypt 加密。                          |
| `agent-plugin/rules/20-i18n.md`                    | messages 组织、key 规则、Locale 解析。                                |
| `agent-plugin/rules/22-git.md`                     | 分支模型、Conventional Commits、PR、Tag。                            |
| `agent-plugin/rules/23-performance.md`             | N+1、慢 SQL、连接池、Async 池、JVM。                                   |
| `agent-plugin/rules/24-deployment.md`              | Dockerfile、Kubernetes、健康检查、优雅停机。                             |
| `agent-plugin/rules/25-review-checklist.md`        | PR 前完整自检清单。                                                  |
| `agent-plugin/rules/26-file-storage.md`            | MinIO Bucket、Object Key、上传校验、签名 URL。                         |
| `agent-plugin/rules/27-scheduling.md`              | XXL-JOB、路由、分片、幂等、超时。                                         |
| `agent-plugin/rules/28-migration.md`               | Flyway 命名、不可变迁移、回滚策略。                                        |
| `agent-plugin/rules/30-dependency.md`              | Gradle 依赖范围、BOM、版本升级、CVE。                                    |

## Starter Skill 参考

`agent-plugin/skills/` 中维护了各 starter 的专项说明。涉及具体 starter 时，除读取 rules 外，还应读取对应 skill，例如：

- RocketMQ：`agent-plugin/skills/eagle-rocketmq/SKILL.md`
- Redis：`agent-plugin/skills/eagle-redis/SKILL.md`
- JPA：`agent-plugin/skills/eagle-data-jpa/SKILL.md`
- 多租户：`agent-plugin/skills/eagle-tenant/SKILL.md`
- OAuth2 资源服务器：`agent-plugin/skills/eagle-resource-server/SKILL.md`
- OpenAPI：`agent-plugin/skills/eagle-openapi/SKILL.md`
- WebSocket：`agent-plugin/skills/eagle-websocket/SKILL.md`

## 项目级命令参考

Claude Code slash command 存放在 `agent-plugin/commands/` 和 `.claude/commands/`。Codex 不会自动执行 slash
command，但实现同类任务时应读取对应命令文档作为脚手架规范。

- `/check-arch`：Modulith 架构验证、模块测试、全量构建检查。
- `/new-module`：按 DDD 模板创建新业务模块。
- `/new-aggregate`：创建聚合根全栈骨架。
- `/new-starter`：按 Spring Boot 4 模板创建新 starter。
- `/add-error-code`：追加 ErrorCode 并同步 i18n。

## 提交与 Pull Request 规范

Git 历史使用带 scope 的 Conventional Commits，例如 `feat(auth): add Account aggregate root`、
`docs(rocketmq): add messaging specification`、`chore(config): disable plugin`。每个提交聚焦一个变更，scope 优先使用模块名或领域名。

PR 应包含简明摘要、受影响模块、关联 issue 或背景说明，以及实际执行过的验证命令。修改用户可见行为或公开接口时，补充截图、请求示例或文档链接。

## 安全与配置提示

不要提交密钥。Nexus 发布配置从 Gradle properties 或环境变量读取，可参考 `gradle.properties.example`。凭据、令牌、本地端点不要写入源码或测试夹具。

## Codex 工作要求

- 开始修改前先查看相关文件和规则，不凭记忆套模板。
- 使用 `rg` / `rg --files` 搜索文件和文本。
- 保持改动聚焦，避免无关重构和格式化噪音。
- 不覆盖用户已有改动，不执行破坏性 Git 命令。
- 完成前运行能证明变更有效的验证命令；若无法运行，说明原因。

<claude-mem-context>
# Memory Context

# [eagle-cloud] recent context, 2026-06-09 11:11am GMT+8

Legend: 🎯session 🔴bugfix 🟣feature 🔄refactor ✅change 🔵discovery ⚖️decision 🚨security_alert 🔐security_note
Format: ID TIME TYPE TITLE
Fetch details: get_observations([IDs]) | Search: mem-search skill

Stats: 50 obs (14,438t read) | 546,296t work | 97% savings

### May 18, 2026
1479 11:48a 🟣 Created GatewayCorsProperties.java: @ConfigurationProperties for eagle.gateway.cors.*
1481 11:50a 🟣 GatewayCorsConfig.java Creation Cleared: Only GatewayCorsProperties Javadoc References CorsWebFilter
1482 " 🔴 Created GatewayCorsConfig.java: Single CorsWebFilter @Bean Fixes Duplicate CORS Header Bug
1483 " 🔵 grep @Bean.*CorsWebFilter Returns Empty Despite GatewayCorsConfig Existing — Pattern Requires Same-Line Match
1499 " ⚖️ 禁用开发环境 Flyway，改用 JPA ddl-auto=update
S453 修复 eagle-system-service 测试 Flyway 校验失败问题，30 个测试全部通过 (May 18 at 2:03 PM)
1500 2:04p 🔵 eagle-system-service Flyway 配置现状：基线和 dev profile 均已关闭
1501 2:05p 🔵 System Service 测试配置根因：@ActiveProfiles("local") + 测试 resources/application.yml 激活 local profile
1502 " 🔵 eagle-system-service build.gradle：flyway-mysql 单独声明 + spring-modulith-starter-test 在测试依赖中
1503 " 🔵 Flyway 报错根因确认：应用基线 application.yml 已修复为 enabled=false，仅 prod profile 显式启用
1504 " 🔴 测试环境 Flyway 双重防御：test/resources/application.yml 加 enabled=false + autoconfigure.exclude
1505 2:06p 🔵 dev profile 仅在 application.yml 基线注释中出现，无外部强制激活源
1506 2:07p 🔴 application-dev.yml 同步添加 FlywayAutoConfiguration 双重防御
1507 2:08p 🔴 eagle-system-service 测试全部通过，Flyway 报错彻底消除
S454 修复 eagle-system-service Flyway 测试报错 + 开始处理 hnsls SMS .env 配置同步 (May 18 at 2:08 PM)
1508 " ✅ SMS 提供商 hnsls 未在 .env 中配置，需同步代码侧改动到环境变量文件
S455 同步 hnsls SMS .env 配置：修复 SMS_SIGN_NAME 键名不匹配导致短信未发送问题 (May 18 at 2:09 PM)
1509 2:15p 🔵 eagle-services/.env 已包含 hnsls SMS 完整凭证，但 .env.example 缺少 SIGN_NAME 字段
1511 " 🔵 hnsls SMS 配置全貌：application.yml 与 docker-compose.yml 对齐但 .env 键名不匹配
1512 " 🔵 .env 加载链：spring-dotenv 从 eagle-services/.env 注入环境变量到 Spring Boot
1510 2:16p 🔵 SMS 配置根因：.env 使用 HNSLS_SMS_SIGN_NAME 但 application.yml 绑定 ${SMS_SIGN_NAME:}，键名不匹配
1517 2:25p 🔴 eagle-services/.env SMS 配置重构：添加 SMS_SIGN_NAME=省心，移除冗余提供商特有键名
S456 同步 hnsls SMS .env 配置完成；.env.example 已与 .env 键名对齐 (May 18 at 2:26 PM)
S463 Configure EAGLE_OAUTH_REDIRECT_URIS in docker-compose and .env, plus .env.example SMS section cleanup (May 18 at 2:26 PM)
1523 2:28p ✅ eagle-services/.env.example SMS 段更新：添加 Tencent 特有字段 + 改进注释
1559 2:29p ✅ OAuth2 Redirect URIs Configuration for Docker Compose and .env
S464 Configure EAGLE_OAUTH_REDIRECT_URIS with localhost:8080 and 139.155.104.132:3000 callback URLs in docker-compose.yml and .env (May 18 at 3:54 PM)
1560 3:54p 🔵 EAGLE_OAUTH_REDIRECT_URIS Binding and Separator Convention
1561 " 🔵 OAuth Redirect URI Insertion Anchor Points in .env, .env.example, and docker-compose.yml
1562 3:55p 🔵 .env.example Missing EAGLE_OAUTH_ISSUER in Required Section
1563 " 🔵 Exact Insertion Points for EAGLE_OAUTH_REDIRECT_URIS in .env and docker-compose.yml
1564 " ✅ EAGLE_OAUTH_REDIRECT_URIS Added to eagle-services/.env
1565 " ✅ EAGLE_OAUTH_REDIRECT_URIS Added to docker-compose.yml system Service
S467 Two issues: (1) Configure EAGLE_OAUTH_REDIRECT_URIS in .env and docker-compose.yml; (2) Fix local test failures (H2 SQL syntax error + Redis connection refused) after local profile config was deleted (May 18 at 3:56 PM)
1571 3:56p 🔵 Test Failures: H2 SQL Syntax Error and Redis Connection Required in Local Tests
S468 Fix local test failures: H2 SQL syntax error on COMMENT ON COLUMN and Redis/Redisson connection refused after application-local.yml was deleted (May 18 at 5:25 PM)
1572 5:25p 🔵 H2 SQL Error Root Cause: JPA @Column(comment=) with Single Quotes in Value
1573 5:26p 🔵 Single-Quote Comment Issue Isolated to One Field in Entire Codebase
1574 " 🔴 FileMetadata.java tenant_id @Column Comment Contains Embedded Single Quotes Causing H2 Failure
1575 " 🔴 Fixed H2 SQL Syntax Error by Removing Single Quotes from FileMetadata @Column Comment
1576 5:27p 🔵 application-local.yml Fully Absent — No Residual Local Profile Config
1577 5:28p 🟣 application-local.yml Recreated with Full Zero-Dependency Local Dev Config
1578 " 🔴 All 33 eagle-system-service Tests Pass After H2 Comment Fix and application-local.yml Restoration
1579 " 🔵 application-local.yml Write Appears Not Persisted Despite Success Response
1580 " ⚖️ Test Philosophy Decision: No Infrastructure Dependencies in Unit/Integration Tests
S469 User objected to H2 usage in tests — "tests should not depend on database, Redis, or any infrastructure" (May 18 at 5:30 PM)
1581 5:35p 🔵 Test Suite Split: Two @SpringBootTest Classes vs 10+ Pure Mockito Unit Tests
1582 5:36p 🔵 EagleSystemApplicationTests Uses @ActiveProfiles("local") — Depends on application-local.yml
1583 5:37p ✅ EagleSystemApplicationTests Disabled by Default — Infrastructure Smoke Test Now Manual Only
1584 5:38p 🔵 test/resources/application.yml Still Activates Local Profile — Redisson Not Excluded in Test Context
1585 5:39p ✅ test/resources/application.yml Now Self-Sufficient — Removed Local Profile, Added Redisson Exclusion
1586 " 🔵 SchemaExportTest Still Uses @SpringBootTest — Second Infrastructure-Dependent Test Needs @Disabled
1587 " 🔴 Test Suite Fully Infrastructure-Free: 33 Tests Pass in 11s Without H2/Redis/Nacos
S470 Refactor tests to be infrastructure-free — no H2, no Redis, no external dependencies in unit tests (May 18 at 5:40 PM)
### May 20, 2026
1588 9:43a 🔵 Resource Server JWT 503 on JWK Endpoint Fetch
1589 " ⚖️ Resource Server Configured with Direct jwk-set-uri to Bypass OpenID Discovery
1592 2:10p 🔴 Admin 权限 Bug 已修复并通过本地测试验证
1596 3:48p 🔵 生产环境启动失败：BlacklistAwareJwtDecoder 缺少无参构造函数
1597 4:20p 🔵 其他资源服务器启动失败：IdGeneratorAutoConfiguration 存在多个 IdGenerator Bean 歧义
### May 21, 2026
1600 2:53p 🔵 RocketMQ Shaded Logback SAX Parser Incompatibility on Startup

Access 546k tokens of past work via get_observations([IDs]) or mem-search skill.
</claude-mem-context>