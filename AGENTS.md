# AGENTS.md

本文件是 Codex 在本仓库工作时的主入口规则。Claude Code 专用说明见 `CLAUDE.md`；Eagle 平台的细则集中放在 `claude-plugin/rules/`，开发时必须按场景读取并遵守。

## 项目概述

Eagle Cloud 是一个基于 DDD + 六边形架构 + Spring Modulith 模块化单体构建的 Spring Boot 平台。系统设计为微服务拆分就绪：领域层通过 Port 接口隔离，未来拆分时优先替换 `infrastructure/` 层实现。

## 技术栈

- Java 25 / Gradle 8.x（Groovy DSL）
- Spring Boot 4.0.6 / Spring Cloud 2025.1.1 / Spring Cloud Alibaba 2025.1.0.0
- Spring Modulith 2.0.5
- Hibernate 7.2.6 / JPA / MySQL / PostgreSQL / Druid
- Spring Security + OAuth2 Authorization Server / Resource Server
- MapStruct 1.6.3 / Lombok / SpringDoc OpenAPI 3.0.2
- Redisson / RocketMQ / XXL-JOB / Seata / MinIO

## 项目结构与模块组织

`settings.gradle` 定义主要模块：

- `eagle-bom`：依赖版本对齐。
- `eagle-services/`：服务应用。
- `eagle-starter/`：可复用 Spring Boot Starter。
- `eagle-doc/`：项目文档。
- `claude-plugin/`：Claude Code 插件、rules、commands 和 starter skills。
- `codex-plugin/`：Codex 插件源码。

各模块遵循标准目录：`src/main/java`、`src/main/resources`、`src/test/java`。Spring Boot 自动配置入口位于 `src/main/resources/META-INF/spring/`。

## 构建、测试与开发命令

当前仓库未提交 Gradle Wrapper，Codex 执行命令时使用本机 `gradle`，不要改用 `./gradlew`。

```bash
gradle build
gradle test
gradle :eagle-starter:eagle-websocket-starter:test
gradle :eagle-starter:eagle-rocketmq-starter:build
gradle dependencyUpdates
```

开发单个 starter 或服务时，优先运行受影响模块的任务获取快速反馈。跨模块或公共契约变更后运行 `gradle build`。`dependencyUpdates` 来自 Ben Manes Versions 插件，用于检查依赖升级。

## DDD 与 Modulith 约定

业务模块内部遵循 `web / application / domain / infrastructure` 四层，依赖方向为 `web -> application -> domain <- infrastructure`。跨模块协作优先使用 Port、领域事件和 `@NamedInterface` 暴露的稳定接口，避免直接穿透其他模块内部实现。

`eagle-services/eagle-system-service` 中 `com.eagle.system` 按有界上下文划分模块，典型模块包括 `auth`、`base`、`config`、`common`。涉及模块边界、Named Interface 或跨模块依赖时，先读取 `claude-plugin/rules/04-modulith.md`。

## 关键基类与基础设施

- `BaseAggregateRoot<T>`：聚合根，包含 ID、审计字段、`@Version` 乐观锁和 `registerEvent()` 事件能力。
- `BaseEntity`：子实体，包含审计字段和乐观锁，不承载领域事件能力。
- `BaseEvent`：领域事件，包含 time-ordered UUID `eventId` 和 `occurredOn`。
- `ErrorCode`：各领域错误码枚举实现该接口，并通过工厂方法创建 `AppException`。

## 编码风格与命名约定

包名保持在 `com.eagle.<area>` 下。类名使用清晰职责后缀，例如 `*AutoConfiguration`、`*Properties`、`*Service`、`*Repository`、`*Gateway`、`*Request`、`*Result`。

Starter 模块应通过 `@AutoConfiguration` 和 properties 类暴露配置，并在 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册自动配置。保持现有 Java 4 空格缩进和简洁的 SLF4J 日志风格。

## 测试指南

测试基于 JUnit Platform 和 `spring-boot-starter-test`，Mockito 已在 Gradle 中配置为 JVM Agent。测试放在对应模块的 `src/test/java`，包结构与生产代码保持一致。测试类命名为 `*Test`。

Starter 工具类优先写聚焦单元测试，自动配置逻辑补充 Spring Context 测试。提交前至少运行受影响模块的 `test`；跨模块修改需运行 `gradle build`。

## 必须遵守的规则索引

修改代码、测试、Gradle、文档或发布配置时，先识别本次任务涉及的领域，只读取相关规则文件并遵守。不要把 `claude-plugin/rules/` 当作装饰性文档。

| 文件 | 适用场景 |
| --- | --- |
| `claude-plugin/rules/01-naming.md` | 命名约定：Java、DDD 组件、ErrorCode、Modulith。 |
| `claude-plugin/rules/02-code-style.md` | Java 风格、Lombok、`@NullMarked`、格式要求。 |
| `claude-plugin/rules/03-architecture.md` | DDD 分层、Port / Adapter、聚合根、领域事件。 |
| `claude-plugin/rules/04-modulith.md` | Spring Modulith、`@ApplicationModule`、`@NamedInterface`、模块边界。 |
| `claude-plugin/rules/05-api.md` | REST API、URL、鉴权、CORS、响应格式。 |
| `claude-plugin/rules/06-database.md` | JPA 实体、跨聚合 ID 引用、索引、CQRS 投影。 |
| `claude-plugin/rules/07-exception.md` | `AppException`、`ErrorCode`、异常映射。 |
| `claude-plugin/rules/08-concurrency.md` | 事务、领域事件、`@Async + AFTER_COMMIT`、缓存失效。 |
| `claude-plugin/rules/09-testing.md` | JUnit 5、Mockito、测试命名、覆盖要求。 |
| `claude-plugin/rules/10-starter.md` | Spring Boot Starter、自动配置、Properties、imports。 |
| `claude-plugin/rules/11-feign.md` | HTTP Service 客户端、错误处理、分页参数。 |
| `claude-plugin/rules/12-security.md` | OAuth2、JWT、密码、敏感数据脱敏、审计。 |
| `claude-plugin/rules/13-logging.md` | SLF4J、MDC、异常日志、敏感字段脱敏。 |
| `claude-plugin/rules/14-cache.md` | Redis + Caffeine、Key 命名、TTL、缓存保护。 |
| `claude-plugin/rules/15-messaging.md` | RocketMQ Topic、幂等、死信、事务消息。 |
| `claude-plugin/rules/16-transaction-distributed.md` | Seata、本地消息表、分布式事务取舍。 |
| `claude-plugin/rules/17-tenant-permission.md` | 多租户隔离、行级数据权限、跨租户操作。 |
| `claude-plugin/rules/18-openapi.md` | SpringDoc 注解、版本、错误码文档化。 |
| `claude-plugin/rules/19-config.md` | Properties、Nacos、profile、Jasypt 加密。 |
| `claude-plugin/rules/20-i18n.md` | messages 组织、key 规则、Locale 解析。 |
| `claude-plugin/rules/22-git.md` | 分支模型、Conventional Commits、PR、Tag。 |
| `claude-plugin/rules/23-performance.md` | N+1、慢 SQL、连接池、Async 池、JVM。 |
| `claude-plugin/rules/24-deployment.md` | Dockerfile、Kubernetes、健康检查、优雅停机。 |
| `claude-plugin/rules/25-review-checklist.md` | PR 前完整自检清单。 |
| `claude-plugin/rules/26-file-storage.md` | MinIO Bucket、Object Key、上传校验、签名 URL。 |
| `claude-plugin/rules/27-scheduling.md` | XXL-JOB、路由、分片、幂等、超时。 |
| `claude-plugin/rules/28-migration.md` | Flyway 命名、不可变迁移、回滚策略。 |
| `claude-plugin/rules/30-dependency.md` | Gradle 依赖范围、BOM、版本升级、CVE。 |

## Starter Skill 参考

`claude-plugin/skills/` 中维护了各 starter 的专项说明。涉及具体 starter 时，除读取 rules 外，还应读取对应 skill，例如：

- RocketMQ：`claude-plugin/skills/eagle-rocketmq/SKILL.md`
- Redis：`claude-plugin/skills/eagle-redis/SKILL.md`
- JPA：`claude-plugin/skills/eagle-data-jpa/SKILL.md`
- 多租户：`claude-plugin/skills/eagle-tenant/SKILL.md`
- OAuth2 资源服务器：`claude-plugin/skills/eagle-resource-server/SKILL.md`
- OpenAPI：`claude-plugin/skills/eagle-openapi/SKILL.md`
- WebSocket：`claude-plugin/skills/eagle-websocket/SKILL.md`

## 项目级命令参考

Claude Code slash command 存放在 `claude-plugin/commands/` 和 `.claude/commands/`。Codex 不会自动执行 slash command，但实现同类任务时应读取对应命令文档作为脚手架规范。

- `/check-arch`：Modulith 架构验证、模块测试、全量构建检查。
- `/new-module`：按 DDD 模板创建新业务模块。
- `/new-aggregate`：创建聚合根全栈骨架。
- `/new-starter`：按 Spring Boot 4 模板创建新 starter。
- `/add-error-code`：追加 ErrorCode 并同步 i18n。

## 提交与 Pull Request 规范

Git 历史使用带 scope 的 Conventional Commits，例如 `feat(auth): add Account aggregate root`、`docs(rocketmq): add messaging specification`、`chore(config): disable plugin`。每个提交聚焦一个变更，scope 优先使用模块名或领域名。

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

# [eagle-cloud] recent context, 2026-05-15 3:40pm GMT+8

Legend: 🎯session 🔴bugfix 🟣feature 🔄refactor ✅change 🔵discovery ⚖️decision 🚨security_alert 🔐security_note
Format: ID TIME TYPE TITLE
Fetch details: get_observations([IDs]) | Search: mem-search skill

Stats: 50 obs (17,883t read) | 503,507t work | 96% savings

### May 15, 2026
1294 11:38a 🔵 pkce.ts: Full PKCE Implementation with Pure-JS SHA-256 Fallback and Dual-Storage Cross-Tab Resilience
1295 11:39a 🔵 Both OAuth2 Clients (default-client and app-client) Are Public Clients with PKCE Now Disabled
S414 OAuth2 PKCE error persists after revert (same error, new state param) — error is still occurring at 139.155.104.132:3000 with code_challenge_method invalid_request (May 15 at 11:42 AM)
1296 11:42a 🔴 Reverted commit 641f52f: require-proof-key Restored to true in eagle-system-service application.yml
1297 " 🔴 Committed Revert e8725a8: require-proof-key Restored to true, 641f52f Workaround Undone
1298 " 🔵 OAuthClientInitializer Confirmed: YAML-to-DB Sync on Startup, No Custom PKCE Validators Anywhere
S415 OAuth2 PKCE error still occurring after revert — investigating OAuthClientInitializer sync mechanism and confirming no custom validators (May 15 at 11:42 AM)
S413 Debug and fix OAuth2 PKCE error (invalid_request: code_challenge_method) — root cause traced, wrong fix reverted, correct state restored (May 15 at 11:42 AM)
S416 OAuth2 PKCE error root cause fully identified: production server running stale bundle where code_challenge_method is a function reference, not the string 'S256' (May 15 at 11:42 AM)
1299 11:53a 🔵 Frontend Bundle Already Contains code_challenge_method — Stale Bundle Hypothesis Eliminated
1300 " 🔵 Deployed Bundle (index-EO3NgK0Z.js) Differs from Local Build (index-BmutAtl6.js) — Stale Bundle IS the Root Cause
1301 11:54a 🔵 Deployed Bundle Has code_challenge_method as Variable (r) Not Hardcoded 'S256' — Critical Difference from Local Build
1302 " 🔵 Deployed Bundle Uses Direct Config URL (SX.oauth2.authorizeUrl) Not OIDC Discovery — Confirms Older Code Version
S417 Fix OAuth2 PKCE callback error (invalid_request: code_challenge_method) in ease-mind-web frontend deployed at http://139.155.104.132:3000 (May 15 at 11:55 AM)
1303 11:59a 🔵 PKCE Error Persists After Frontend Redeployment — Same State Parameter Reused Suggests Browser Cache Not Cleared
1304 " 🔵 Redeployment Failed: Server Still Serving Old Bundle index-EO3NgK0Z.js
1305 " 🔵 Local dist Bundle is Stale: Built May 1, pkce.ts Modified Today May 15 — yarn build Required Before Deployment
1306 " 🔵 Second Frontend Project Discovered: ease-mind-web at /Users/sunshixiong/my-work/ease-mind-web
1307 12:01p 🔵 Root Cause Found: ease-mind-web Falls Back to PKCE "plain" Method on HTTP — Spring AS Rejects It
1308 12:02p 🔵 ease-mind-web pkce.ts Has Two Consumers: auth-provider (login) and oauth2-callback Page
1309 " 🔴 ease-mind-web pkce.ts Fixed: Pure-JS SHA-256 Fallback Added, "plain" Method Eliminated
S418 Add Swagger annotations to ChatController and audit all controllers in eagle-system-service for missing annotations (May 15 at 12:02 PM)
S419 Add Swagger annotations to ChatController and audit all controllers in eagle-system-service for missing annotations (May 15 at 12:03 PM)
1323 12:03p 🔵 Swagger Annotation Audit — 3 Controllers Missing @Tag/@Operation in eagle-system-service
S421 New task: Expose SMS_PROVIDER config in docker-compose; keep only HNSL-related SMS config and remove Alibaba Cloud and Tencent Cloud configs (May 15 at 12:03 PM)
1324 1:59p 🔵 LoginController and WechatWebLoginController Are MVC Template Controllers, Not REST APIs
1325 " ✅ Added @Operation Swagger Annotation to ChatController broadcast-message STOMP Endpoint
1326 " ✅ ChatController @Operation Annotations Complete — Both STOMP Endpoints Annotated
1327 " ✅ ChatController Swagger Annotation Complete — @Tag Added with Javadoc Note Pointing to AsyncAPI Docs
1328 " 🔵 ChatMessage and PrivateMessage DTOs Are Only Used by ChatController
1329 2:01p ✅ ChatMessage and PrivateMessage DTOs Annotated with @Schema for WebSocket Documentation
1330 " ✅ LoginController @Tag Annotation Added
1331 " ✅ LoginController @Operation Annotations Added to Three Endpoints
1332 " ✅ LoginController @Operation Annotation Complete — All 4 Endpoints Annotated
1333 2:02p ✅ LoginController Swagger Imports Added; WechatWebLoginController @Tag Started
1334 " ✅ WechatWebLoginController @Operation Annotations Complete — All 4 Endpoints Annotated
1335 " ✅ WechatWebLoginController Swagger Annotation Complete — All 3 Missing Controllers Now Fully Annotated
1336 2:03p ✅ Swagger Annotation Audit Complete — All 12 Controllers Have Full Coverage; Compile Check Skipped
1337 " 🔵 Gradle Wrapper Not Found at eagle-cloud Project Root in Claude Code Shell
1338 " 🔵 eagle-cloud Project Has No Gradle Wrapper — Build Tool Unclear
1339 " 🔵 eagle-cloud Uses System Gradle at /opt/homebrew/bin/gradle (No Wrapper)
S420 Add Swagger annotations to ChatController and audit/fix all controllers missing annotations in eagle-system-service (May 15 at 2:03 PM)
1340 2:46p 🔵 eagle-cloud SMS/Notification Service Architecture Discovered
1341 " 🔵 手拉手 SMS Provider API Protocol Fully Extracted
1342 2:47p 🔵 SMS Provider Extension Pattern and Existing Code Structure Fully Mapped
1343 " 🔵 SMS Provider Configuration Structure in application.yml and .env.example Mapped
1345 " 🟣 HnslsSmsServiceImplTest Created as TDD Red-Light Test
1344 2:48p ⚖️ Implementation Plan Established for ShoulaShou SMS Provider Integration
1346 " 🔵 TDD Red-Light Confirmed: HnslsSmsServiceImpl and HnslsSmsProperties Don't Exist Yet
1347 " 🟣 HnslsSmsProperties @ConfigurationProperties Class Created
1348 2:49p 🔴 HnslsSmsServiceImpl 构造函数自动装配歧义修复
1351 3:08p 🔵 手拉手短信发送失败 — 待调试
1349 3:20p 🔵 Docker Compose and .env Files Located in eagle-services/ Subdirectory
1350 3:21p 🔵 SMS Configuration Gap: docker-compose.yml Missing SMS_PROVIDER; Only Aliyun Vars Exposed (Not HNSL)
S422 SMS配置修复：将SMS_PROVIDER暴露到docker-compose，只保留HNSLS短信配置，去掉阿里云和腾讯云 (May 15 at 3:22 PM)
1352 3:30p 🟣 手拉手 SMS 实际发送测试请求 — 环境变量已配置
1354 " 🔵 手拉手三个网关地址全部无法连通 — 网络层封锁而非配置问题
1353 3:39p 🔵 手拉手 SMS 发送失败根因：.env 中 SEND_URL 配置为无法连通的 IP 地址
1355 3:40p 🟣 HnslsSmsServiceImplTest 全部通过，DEBUG 日志格式验证正确
1356 " 🔴 修复 doSend() 双重 ERROR 日志 — ServiceException 被外层 catch 重复捕获

Access 504k tokens of past work via get_observations([IDs]) or mem-search skill.
</claude-mem-context>