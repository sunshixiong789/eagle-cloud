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

# [eagle-cloud] recent context, 2026-05-14 6:35pm GMT+8

Legend: 🎯session 🔴bugfix 🟣feature 🔄refactor ✅change 🔵discovery ⚖️decision 🚨security_alert 🔐security_note
Format: ID TIME TYPE TITLE
Fetch details: get_observations([IDs]) | Search: mem-search skill

Stats: 50 obs (12,235t read) | 420,771t work | 97% savings

### May 14, 2026
1168 5:05p 🟣 新增 TencentSmsProvider：腾讯云短信核心实现
1169 5:06p 🔵 SmsMessageChannel 需重构为 SmsProvider 委派层，当前仍为旧阿里云实现
1184 5:08p 🔵 SMS diagnostic logs confirm code 225697 written to Caffeine cache, bean identity hash 590578720
1170 " 🔄 SmsMessageChannel 重构为 SmsProvider 委派层，移除所有阿里云 SDK 直接依赖
1171 " ✅ MessageAutoConfiguration 新增 SmsProvider 相关 import
1172 5:09p 🟣 MessageAutoConfiguration.SmsChannelConfiguration 重构为双 provider 条件配置
1173 " 🔵 gradlew 不在当前工作目录，编译验证需切换至项目根路径
1174 " 🔵 eagle-cloud 项目无 gradlew 包装器，无法在命令行编译验证
1175 5:10p 🔵 Gradle 9.5.0 全局安装于 /opt/homebrew/bin/gradle，可替代 gradlew 编译
1176 " 🔵 编译失败：com.tencentcloudapi:tencentcloud-sdk-java-sms:3.1.1141 在配置的 Maven 仓库中不存在
1177 " 🔵 本地 Maven 和 Gradle 缓存均无腾讯云 SDK，需核实正确 Maven 坐标
1178 " 🔵 腾讯云 SMS SDK 最新版本为 3.1.1451，BOM 中使用的 3.1.1141 版本号错误
1179 5:11p 🔴 修正 eagle-bom 中腾讯云 SMS SDK 版本号：3.1.1141 → 3.1.1451
1180 5:12p 🟣 eagle-notification-starter 腾讯云 SMS 集成编译成功
1181 " 🟣 eagle-notification-starter 完整构建成功，腾讯云 SMS 集成交付完毕
1182 " ✅ USAGE.md 开始更新：短信服务商描述改为阿里云/腾讯云二选一
1183 " ✅ USAGE.md 配置示例更新，新增腾讯云完整配置说明和注意事项
1185 " ✅ USAGE.md 完整重写：配置项表格新增腾讯云字段，常见错误新增腾讯云专项警告
1186 5:14p 🔵 SMS code grant type string confirmed as `"sms_code"` in `SmsCodeAuthenticationToken`
1187 " 🔵 Exception handler locations in eagle-cloud
1188 " 🔵 `GlobalExceptionHandler` does not intercept `invalid_grant` — OAuth2 errors handled by Spring Authorization Server's own mechanism
1189 5:15p 🔵 `SmsCodeAuthenticationConverter` wired exclusively in `SecurityConfig.java`
1190 " 🔵 `SmsCodeAuthenticationConverter` full content revealed — throws `DomainException` for missing params, not OAuth2 errors
1191 5:16p ✅ Debug logging added to `SmsCodeAuthenticationConverter` to trace grant type matching
1192 " 🔵 `SmsCodeAuthenticationProvider` wired exclusively in `SecurityConfig.java`
1193 " 🔵 `SmsCodeAuthenticationProvider` full content revealed — missing `FactorGrantedAuthority.OTT_AUTHORITY` causes `authenticationTime cannot be null`
1194 5:17p ✅ Diagnostic logs downgraded from `INFO` to `DEBUG` in `AliyunSmsServiceImpl` and `SmsCodeAuthenticationConverter`; `SmsCodeAuthenticationProvider` keeps `INFO`
1195 " ✅ All SMS grant diagnostic logs unified to `log.debug` level across all three classes
1196 " ✅ SMS grant diagnostic logging changes compile cleanly
1197 " 🔵 Second DEBUG test run: code 026893 generated, bean hash 2040958233, log truncated before converter/provider lines appear
S374 Fix mobile app SMS code grant `invalid_grant` — comprehensive DEBUG logging added across full grant chain; awaiting user test run with DEBUG enabled (May 14 at 5:17 PM)
S376 Fix mobile app SMS code grant — converter/provider confirmed NOT being called; escalated to Spring Security TRACE logging to find where request is being intercepted (May 14 at 5:18 PM)
S377 Fix mobile app SMS code grant — root cause definitively identified as PKCE `code_verifier` enforcement by `PublicClientAuthenticationProvider` blocking ALL custom grants (May 14 at 5:43 PM)
1198 5:50p 🔵 Root cause identified: SMS code grant fails at `PublicClientAuthenticationProvider` due to PKCE `code_verifier` enforcement on `eagleWeb` client
S378 Fix mobile app SMS code grant — user chose option B (new dedicated `eagleApp` client); planning implementation and cleanup of diagnostic logs (May 14 at 5:52 PM)
S380 Fix mobile app SMS code grant (POST /oauth2/token?grant_type=sms_code) returning invalid_grant in eagle-cloud Spring Authorization Server 7.0.5 — implement Option B (new eagleApp client with require_proof_key=false) (May 14 at 5:52 PM)
1199 5:53p 🔵 `OAuthClientProperties` is single-client mode only — supports only `eagleWeb` via `eagle.oauth.default-client` prefix
S381 Fix eagle-cloud Spring Authorization Server 7.0.5 sms_code grant returning invalid_grant; remove all diagnostic debug logs; apply FactorGrantedAuthority.OTT_AUTHORITY fix (May 14 at 5:53 PM)
1200 5:58p 🔵 eagle-system-service 已有独立 SMS 配置块（eagle.sms.aliyun），与 notification-starter 路径不同
1201 5:59p 🔵 eagle-system-service auth 模块有独立 SMS 实现（AliyunSmsServiceImpl），用于验证码发送和校验
1202 " 🔵 eagle-services/.env.example 中 SMS 环境变量只有 Aliyun 4个字段，profile 配置文件无 SMS 覆盖
1203 " 🔵 .env 与 .env.example 中 SMS 变量内容完全一致，均为 4 个空值 ALIYUN_SMS_* 变量
1205 " 🔵 auth/infrastructure/external/ 已有多个外部服务实现，SMS 错误码已完备
1206 " 🔄 新增 AbstractCachedSmsService 抽象基类，提取验证码缓存和频率限制通用逻辑
1204 6:00p 🔵 .env 中阿里云短信块结构确认：4行变量后紧跟两个空行再接分隔注释
1207 " 🔵 auth/infrastructure/config/ 确认 Properties 类模式，TencentSmsProperties 尚未创建
S382 Fix eagle-cloud sms_code grant invalid_grant; remove all diagnostic debug logs; apply FactorGrantedAuthority.OTT_AUTHORITY; understand why require_proof_key=false is insufficient (May 14 at 6:00 PM)
S383 Fix eagle-cloud sms_code grant invalid_grant by creating CustomGrantClientAuthenticationProvider to bypass PublicClientAuthenticationProvider PKCE enforcement for custom grants (May 14 at 6:01 PM)
1208 6:01p 🟣 新增 TencentSmsProperties 配置类，绑定 eagle.sms.tencent 前缀
1209 6:02p 🔄 AliyunSmsServiceImpl 重构为继承 AbstractCachedSmsService，添加 @ConditionalOnProperty(provider=aliyun)
1210 6:03p 🔵 eagle-system-service build.gradle 只有阿里云 dysmsapi SDK，需新增腾讯云 tencentcloud-sdk-java-sms 依赖
1211 " ✅ eagle-system-service/build.gradle 新增腾讯云 SMS SDK 依赖
1212 " 🟣 新增 TencentSmsServiceImpl，eagle-system-service auth 模块腾讯云短信实现完成
1213 " 🔵 application.yml 中 eagle.sms 块位置确认（第179行），准备扩展为多 provider 结构
1215 6:04p 🟣 application.yml eagle.sms 配置块扩展为多 provider 结构，新增 tencent 子块和 provider 切换字段
1216 " 🟣 .env.example 更新：新增 SMS_PROVIDER 切换变量和 6 个 TENCENT_SMS_* 腾讯云短信变量
1217 " 🔵 application.yml 多次 Edit 后读取仍显示旧内容，SMS 配置块更新未持久化
1214 6:05p 🔵 application.yml eagle.sms 配置块精确内容确认（第179-185行），即将扩展 provider 和 tencent 子块
S384 Fix eagle-cloud sms_code invalid_grant: wire CustomGrantClientAuthenticationProvider into SecurityConfig to bypass PKCE enforcement for custom grants (May 14 at 6:27 PM)
S385 Fix eagle-cloud Spring Authorization Server 7.0.5 sms_code grant returning invalid_grant by implementing CustomGrantClientAuthenticationProvider to bypass mandatory PKCE for custom grant types (May 14 at 6:29 PM)
**Investigated**: - SAS 7.0.5 source: `PublicClientAuthenticationProvider.authenticate()` calls `codeVerifierAuthenticator.authenticateRequired()` UNCONDITIONALLY for all `none`-method clients
    - `CodeVerifierAuthenticator.authenticate()` returns `false` for non-authorization_code grants → `authenticateRequired()` always throws `invalid_grant: code_verifier`
    - `require_proof_key=false` on `RegisteredClient` does NOT bypass token endpoint PKCE — that flag only affects the authorization endpoint
    - `ProviderManager` stops at first provider returning non-null — custom providers registered via SAS DSL `.clientAuthentication(c -> c.authenticationProvider(...))` are prepended before built-in providers

**Learned**: - Root cause confirmed: `PublicClientAuthenticationProvider` enforces PKCE unconditionally for ALL `none`-auth-method clients regardless of grant type or `require_proof_key` setting
    - Fix strategy: Register a custom `AuthenticationProvider` BEFORE `PublicClientAuthenticationProvider` that handles custom grants (`sms_code`, `wechat_mini_program`, `phone_one_click`) by returning an already-authenticated token, causing `ProviderManager` to stop before reaching `PublicClientAuthenticationProvider`
    - For `authorization_code` grant, the custom provider returns `null` → falls through to `PublicClientAuthenticationProvider` → PKCE still enforced for web OAuth2 flow (security preserved)
    - SAS DSL `.clientAuthentication(c -> c.authenticationProvider(...))` prepends custom providers to the list automatically

**Completed**: - **NEW `CustomGrantClientAuthenticationProvider.java`** created:
      - Supports: `sms_code`, `wechat_mini_program`, `phone_one_click`
      - Only intercepts `ClientAuthenticationMethod.NONE` requests
      - Returns `null` for non-custom grant types (falls through to built-in providers)
      - Validates: client exists, has `none` auth method, grant type is in whitelist
      - Returns authenticated `OAuth2ClientAuthenticationToken(client, NONE, null)` — bypasses PKCE
    - **`SecurityConfig.java`** updated:
      - Import added: `CustomGrantClientAuthenticationProvider`
      - `filterChain()` signature: `RegisteredClientRepository registeredClientRepository` added as parameter
      - `.clientAuthentication(...)` block: `.authenticationProvider(new CustomGrantClientAuthenticationProvider(registeredClientRepository))` registered
    - **`CustomGrantPublicClientAuthenticationConverter.java`** (previously created) — converts custom grant requests into `OAuth2ClientAuthenticationToken` to enter the client auth chain
    - **`OAuthAppClientProperties.java`** + **`OAuthClientInitializer.java`** (rewritten with dual web/app `ClientSpec` record) — `eagleApp` client with `none` auth method, custom grants whitelist
    - **`application.yml`**: `eagle.oauth.app-client` block added
    - **All diagnostic debug logs removed** from `SmsCodeAuthenticationProvider`, `SmsCodeAuthenticationConverter`, `AliyunSmsServiceImpl`
    - **`LoginController.smsLogin`** + **`WechatWebLoginController.authenticateAndRedirect`**: `FactorGrantedAuthority.OTT_AUTHORITY` added to token authorities (fixes OIDC `auth_time` null)
    - `gradle compileJava` → **clean compile verified**

**Next Steps**: - User needs to: run `gradle :eagle-services:eagle-system-service:clean :eagle-services:eagle-system-service:bootJar`, restart service, and test `POST /oauth2/token` with `grant_type=sms_code&client_id=eagleApp&phone=...&code=...`
    - Optionally remove temporary logging config from `application.yml` (`logging.level.com.eagle.system.auth: DEBUG`, `logging.level.org.springframework.security: TRACE`) before deploying
    - `SmsCodeAuthenticationProvider.generateTokens()` may still need `FactorGrantedAuthority.OTT_AUTHORITY` — currently uses `AuthorityUtils.createAuthorityList("ROLE_USER")` — verify whether this causes `authenticationTime cannot be null` after PKCE is fixed
    - Awaiting user test results to confirm the fix works end-to-end


Access 421k tokens of past work via get_observations([IDs]) or mem-search skill.
</claude-mem-context>