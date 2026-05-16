# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

Eagle Cloud 是一个基于 DDD + 六边形架构 + Spring Modulith 模块化单体构建的 Spring Boot 平台。设计为微服务拆分就绪——领域层通过
Port 接口隔离，拆分时只需替换 `infrastructure/` 层实现。

## 技术栈

- **Java 25** / Gradle 8.x (Groovy DSL)
- **Spring Boot 4.0.6** / Spring Cloud 2025.1.1 / Spring Cloud Alibaba 2025.1.0.0
- **Spring Modulith 2.0.5** — 模块边界静态验证
- **Hibernate 7.2.6** (JPA) / MySQL 9.6.0 / PostgreSQL 42.7.10 / Druid 1.2.28
- **Spring Security + OAuth2 Authorization Server**
- **MapStruct 1.6.3** / Lombok / SpringDoc OpenAPI 3.0.2
- **Redisson 4.3.0** / RocketMQ 2.3.5 / XXL-JOB 2.4.2 / Seata 2.2.0 / MinIO 8.5.17

## PR 前必跑（速查）

```bash
# 1) 完整构建（含编译 / 测试）
./gradlew clean build

# 2) Spring Modulith 架构验证（涉及 system-server 时必跑）
./gradlew :eagle-base-server:eagle-system-server:test --tests "*.ModulithArchitectureTest"

# 3) 仅当前模块测试（开发期快速反馈）
./gradlew :path:to:module:test
```

或一键：`/check-arch`（详见 `.claude/commands/check-arch.md`）。

## 构建与运行

```bash
# 构建整个项目
./gradlew build

# 构建特定模块
./gradlew :eagle-base-server:eagle-system-server:build

# 运行测试
./gradlew test
./gradlew :eagle-base-server:eagle-system-server:test
./gradlew :eagle-base-server:eagle-system-server:test --tests "com.eagle.system.YourTestClass"
./gradlew :eagle-base-server:eagle-system-server:test --tests "com.eagle.system.YourTestClass.testMethod"

# 运行服务
./gradlew :eagle-base-server:eagle-system-server:bootRun
./gradlew :eagle-base-server:eagle-gateway-server:bootRun
```

## 项目结构

### 可执行服务 (eagle-base-server)

| 服务                     | 说明                                   | 技术栈                                                  |
|------------------------|--------------------------------------|------------------------------------------------------|
| `eagle-system-server`  | 系统服务：OAuth2 授权服务器、用户/角色/权限管理、微信/短信登录 | JPA, OAuth2 Auth Server, WebSocket, Nacos, Thymeleaf |
| `eagle-gateway-server` | API 网关：路由、JWT 鉴权、限流、链路追踪             | Spring Cloud Gateway (WebFlux), Sentinel, Nacos      |

### Starter 库模块 (eagle-starter)

| 模块                                 | 说明                                                                                             |
|------------------------------------|------------------------------------------------------------------------------------------------|
| `eagle-common-starter`             | 核心基础设施：基类（BaseAggregateRoot/BaseEntity）、异常体系（AppException/ErrorCode）、领域事件（BaseEvent）、i18n、安全工具 |
| `eagle-data-jpa-starter`           | JPA/Hibernate 配置、审计、MySQL/PostgreSQL/H2 支持                                                     |
| `eagle-redis-starter`              | Redisson + Caffeine 多级缓存 + CacheProtectionUtil（穿透/击穿防护）                                        |
| `eagle-resource-server-starter`    | OAuth2 资源服务器 JWT 验证                                                                            |
| `http-client-starter`              | RestClient / HTTP Service 客户端配置（含 Seata XID 透传）                                                 |
| `eagle-tracing-starter`            | 分布式链路追踪（Brave/Zipkin）                                                                          |
| `eagle-rocketmq-starter`           | RocketMQ v5 消息队列（事务消息、DLQ、AbstractRocketMqListener）                                           |
| `eagle-row-security-starter`       | 行级数据权限控制（@DataPermission，AspectJ + JPA Specification）                                          |
| `eagle-dynamic-datasource-starter` | 多数据源动态路由（主从切换、@ReadOnly、轮询负载均衡）                                                                |
| `eagle-tenant-starter`             | 多租户支持（COLUMN/DATABASE 隔离模式、TenantContextHolder）                                               |
| `eagle-oss-minio-starter`          | 对象存储（MinIO 8.x，签名 URL、分片上传）                                                                    |
| `eagle-notification-starter`       | 多渠道消息（阿里云 SMS、Spring Mail）                                                                     |
| `eagle-scheduler-starter`          | 分布式定时任务（XXL-JOB 2.4.2）                                                                         |
| `eagle-openapi-starter`            | Swagger/OpenAPI 文档集成（SpringDoc 3.0.2）                                                          |
| `eagle-seata-starter`              | 分布式事务（Seata AT/TCC 2.2.0）                                                                      |
| `eagle-sentinel-starter`           | 流量控制与熔断（Sentinel，网关层限流）                                                                        |
| `eagle-mybatis-starter`            | MyBatis-Plus 配置（分页、逻辑删除、审计）                                                                    |
| `eagle-id-generator-starter`       | 分布式 ID 生成（雪花算法 / Leaf 等）                                                                       |
| `eagle-idempotency-starter`        | 接口幂等性（@Idempotent，Redis SETNX + 唯一约束双重保障）                                                      |
| `eagle-elasticsearch-starter`      | Elasticsearch 全文检索（Spring Data ES）                                                             |
| `eagle-payment-starter`            | 支付集成（微信支付 / 支付宝）                                                                               |
| `eagle-websocket-starter`          | WebSocket 实时通信（STOMP + SockJS）                                                                 |
| `eagle-sharding-starter`           | 分库分表（Apache ShardingSphere 5.5.0，YAML 驱动）                                                      |
| `eagle-excel-starter`              | Excel 导入导出（Apache POI，@ExcelColumn，大数据流式写入）                                                    |
| `eagle-resilience-starter`         | 容错弹性（Resilience4J，熔断器 / 重试 / 超时，eagle-default 命名实例）                                           |
| `eagle-encrypt-starter`            | 字段级加密（AES-256，JPA AttributeConverter，@Convert 注解驱动）                                            |
| `eagle-audit-log-starter`          | 操作审计日志（@AuditLog，AOP 切面 + 异步事件 + 可插拔 Handler）                                                 |
| `eagle-ai-starter`                 | AI 集成（Spring AI 2.x，ChatClient、EmbeddingClient）                                               |

Starter 模块设置 `bootJar.enabled = false`、`jar.enabled = true`，依赖使用 `api` 范围暴露传递依赖。

## Spring Modulith 模块划分

`eagle-system-server` 中 `com.eagle.system` 下按有界上下文划分为 4 个模块：

| 模块         | 包                         | 类型          | 职责                                                           | allowedDependencies                   |
|------------|---------------------------|-------------|--------------------------------------------------------------|---------------------------------------|
| **auth**   | `com.eagle.system.auth`   | 业务域         | 认证授权、OAuth2、微信/短信登录                                          | `common`                              |
| **base**   | `com.eagle.system.base`   | 业务域         | 用户、角色、权限、部门、菜单管理                                             | `auth::port`, `auth::event`, `common` |
| **config** | `com.eagle.system.config` | 基础设施胶水      | SecurityConfig、CacheConfig、AsyncConfig、WebSocket、i18n、全局异常处理 | `auth::security`, `common`            |
| **common** | `com.eagle.system.common` | 共享内核 (OPEN) | ErrorCode 枚举、通用 DTO、异常基础设施                                   | 无外部依赖                                 |

**模块间协作方式：**

- auth 定义 Driven Port（`auth/domain/port/`），base 在 `infrastructure/` 层实现适配器——auth 对 base 零依赖
- auth → base 通过领域事件（`AccountRegisteredEvent` 等，放在 `auth/domain/event/`，通过 `@NamedInterface("event")` 暴露）异步解耦
- config 通过 Named Interface `auth::security` 引用安全组件装配过滤链

## DDD 分层架构

每个业务模块内部遵循 `interfaces / application / domain / infrastructure` 四层，依赖方向：
`interfaces → application → domain ← infrastructure`。完整分层结构和规范见 `.claude/rules/03-architecture.md`。

## 关键基类

- `BaseAggregateRoot<T>` — 聚合根：ID (IDENTITY)、审计字段、`@Version` 乐观锁、`registerEvent()` 事件能力
- `BaseEntity` — 子实体：审计字段 + 乐观锁，无事件能力
- `BaseEvent` — 领域事件：time-ordered UUID `eventId` + `occurredOn`
- `ErrorCode` 接口 → 各域枚举实现（`toNotFoundException()` / `toDomainException()` / `toConflictException()` /
  `toServiceException()`）

## Gradle 配置要点

- 所有模块的 AOT 任务（processAot/processTestAot）已禁用
- Hibernate 字节码增强已启用（`enableAssociationManagement`）
- MapStruct 编译参数：`defaultComponentModel=spring`、`unmappedTargetPolicy=IGNORE`
- 测试使用 JUnit 5 (JUnit Platform)，Mockito 以 JVM Agent 方式加载（解决 JDK 21+ 动态 Agent 警告）
- 测试超时 5 分钟

## 详细开发规范（按场景查阅）

| 文件                                            | 适用场景                                          |
|-----------------------------------------------|-----------------------------------------------|
| `.claude/rules/01-naming.md`                  | 命名约定（类、方法、DDD 组件、ErrorCode）                   |
| `.claude/rules/02-code-style.md`              | Google Java Style + Lombok 规则 + `@NullMarked` |
| `.claude/rules/03-architecture.md`            | DDD 分层、跨域 Port/Adapter、聚合根创建型事件               |
| `.claude/rules/04-modulith.md`                | `@ApplicationModule` / `@NamedInterface` 边界治理 |
| `.claude/rules/05-api.md`                     | RESTful URL、`@PreAuthorize`、CORS、响应格式         |
| `.claude/rules/06-database.md`                | JPA 实体、跨聚合 ID 引用、索引、CQRS 投影                   |
| `.claude/rules/07-exception.md`               | AppException 体系、ErrorCode 工厂方法                |
| `.claude/rules/08-concurrency.md`             | 事务、领域事件 `@Async + AFTER_COMMIT`、缓存失效          |
| `.claude/rules/09-testing.md`                 | JUnit 5 + Mockito、AAA、命名、覆盖要求                 |
| `.claude/rules/10-starter.md`                 | `@AutoConfiguration` + Properties + imports   |
| `.claude/rules/11-feign.md`                   | HTTP Service 客户端位置、错误处理、分页参数       |
| `.claude/rules/12-security.md`                | OAuth2 / JWT、密码、CORS、敏感数据脱敏、审计                |
| `.claude/rules/13-logging.md`                 | SLF4J 占位符、MDC、异常日志、敏感字段脱敏                     |
| `.claude/rules/14-cache.md`                   | Redis+Caffeine、Key 命名、TTL、击穿/穿透/雪崩            |
| `.claude/rules/15-messaging.md`               | RocketMQ Topic 命名、幂等、死信、事务消息                  |
| `.claude/rules/16-transaction-distributed.md` | Seata AT/TCC 选型、本地消息表                         |
| `.claude/rules/17-tenant-permission.md`       | 多租户隔离、行级数据权限、跨租户操作                            |
| `.claude/rules/18-openapi.md`                 | SpringDoc 注解、版本、错误码文档化                        |
| `.claude/rules/19-config.md`                  | Properties、Nacos、profile、Jasypt 加密            |
| `.claude/rules/20-i18n.md`                    | messages 组织、key 规则、Locale 解析                  |
| `.claude/rules/21-resilience.md`              | Resilience4J 熔断/重试/超时、Fallback、注解组合顺序        |
| `.claude/rules/22-git.md`                     | 分支模型、Conventional Commits、PR、Tag              |
| `.claude/rules/23-performance.md`             | N+1、慢 SQL、连接池、Async 池、JVM                     |
| `.claude/rules/24-deployment.md`              | Dockerfile、健康检查、优雅停机、K8s                      |
| `.claude/rules/25-review-checklist.md`        | **PR 前完整自检清单（必看）**                            |
| `.claude/rules/26-file-storage.md`            | MinIO Bucket、Object Key、上传校验、签名 URL           |
| `.claude/rules/27-scheduling.md`              | XXL-JOB 路由、分片、幂等、超时                           |
| `.claude/rules/28-migration.md`               | Flyway 命名、不可变、回滚                              |
| `.claude/rules/29-event-driven.md`            | 领域事件 vs 集成事件、Saga 编排、Event Sourcing、幂等       |
| `.claude/rules/30-dependency.md`              | Gradle 范围、BOM、版本升级、CVE                        |

## 项目级 Commands（slash command）

| 命令                | 作用                                                                              |
|-------------------|---------------------------------------------------------------------------------|
| `/eagle-flow`     | **启动 6 阶段端到端流程**（自然语言"做一个新功能"等价触发；详见 `eagle-feature-flow` skill） |
| `/check-arch`     | Modulith 架构验证 + 模块测试 + 全量构建一键检查                                                 |
| `/new-module`     | 按 DDD 模板创建新业务模块（含 `package-info.java` + 四层骨架）                                   |
| `/new-aggregate`  | 创建聚合根全栈骨架（聚合根 + Repository + ErrorCode + ApplicationService + Controller + DTO） |
| `/new-starter`    | 按 Spring Boot 4 模板创建新 starter 模块                                                |
| `/add-error-code` | 在 ErrorCode 枚举追加常量并同步 i18n 三语翻译                                                 |

## 端到端开发流程（eagle-feature-flow skill）

主干用 **Superpowers 6 阶段**，在 **规划** 与 **写代码** 阶段嵌入式调用本仓库的 rules / commands / starter skills（不使用 OpenSpec）：

| 阶段 | 名称         | 主干调用                                          | claude-plugin 注入                                                            |
|----|------------|-----------------------------------------------|---------------------------------------------------------------------------|
| 1  | Brainstorm | `superpowers:brainstorming`                   | （无，聚焦需求澄清）                                                                |
| 2  | Plan       | `superpowers:writing-plans`                   | ★ 必读相关 `.claude/rules/*` + 在 plan 中预定要触发的 commands（`/new-module` 等）       |
| 3  | TDD        | `superpowers:test-driven-development`         | ★ 加载相关 starter skills（`eagle-common` / `eagle-rocketmq` 等）+ 执行 plan 中的 commands |
| 4  | Verify     | `superpowers:verification-before-completion`  | ★ 强制 `/check-arch`                                                        |
| 5  | Review     | `superpowers:requesting-code-review`          | ★ 对照 `.claude/rules/25-review-checklist.md`（16 大类自检）                       |
| 6  | Finish     | `superpowers:finishing-a-development-branch`  | 按 `.claude/rules/22-git.md` 整理 commit + PR 描述                              |

**设计哲学**：Superpowers 提供工程纪律（brainstorm → plan → TDD → verify → review → finish），
本仓库的 `claude-plugin` 提供 Eagle 平台的"约束"（rules）和"工具箱"（commands + per-starter skills），后者在主流程的关键节点被嵌入式调用。

详见 `claude-plugin/skills/eagle-feature-flow/SKILL.md`。模型在识别到"做一个新功能 / 加一个模块 / 重构 X"等触发短语时自动激活；手动触发可直接说"按 eagle flow 走"。
