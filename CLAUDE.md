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
- Lombok / SpringDoc OpenAPI 3.0.2
- **Redisson 4.3.0** / RocketMQ 2.3.5 / XXL-JOB 2.4.2 / Seata 2.2.0 / MinIO 8.5.17

## PR 前必跑（速查）

```bash
# 1) 完整构建（含编译 / 测试）
./gradlew clean build

# 2) Spring Modulith 架构验证（涉及 system-server 时必跑）
./gradlew :eagle-services:eagle-system-service:test --tests "*.ModulithArchitectureTest"

# 3) 仅当前模块测试（开发期快速反馈）
./gradlew :path:to:module:test
```

或一键：`/check-arch`（详见 `.claude/commands/check-arch.md`）。

## 构建与运行

```bash
# 构建整个项目
./gradlew build

# 构建特定模块
./gradlew :eagle-services:eagle-system-service:build

# 运行测试
./gradlew test
./gradlew :eagle-services:eagle-system-service:test
./gradlew :eagle-services:eagle-system-service:test --tests "com.eagle.system.YourTestClass"
./gradlew :eagle-services:eagle-system-service:test --tests "com.eagle.system.YourTestClass.testMethod"

# 运行服务
./gradlew :eagle-services:eagle-system-service:bootRun
./gradlew :eagle-services:eagle-gateway-service:bootRun
```

## 项目结构

### 可执行服务 (eagle-services)

| 服务                      | 说明                                       | 技术栈                                                  |
|-------------------------|------------------------------------------|------------------------------------------------------|
| `eagle-system-service`  | 系统服务：用户/角色/权限/部门/菜单管理、文件、消息、公告                  | JPA, WebSocket, Nacos                                |
| `eagle-auth-service`    | 认证服务：OAuth2 授权服务器、JWT 签发、微信/短信/手机一键登录    | JPA, OAuth2 Auth Server, Thymeleaf                   |
| `eagle-gateway-service` | API 网关：路由、JWT 鉴权、限流、链路追踪                 | Spring Cloud Gateway (WebFlux), Sentinel, Nacos      |
### Starter 库模块 (eagle-starter)

| 模块                                 | 说明                                                                                             |
|------------------------------------|------------------------------------------------------------------------------------------------|
| `eagle-common-starter`             | 核心基础设施：基类（BaseAggregateRoot/BaseEntity）、异常体系（AppException/ErrorCode）、领域事件（BaseEvent）、i18n、安全工具 |
| `eagle-data-jpa-starter`           | JPA/Hibernate 配置、审计、MySQL/PostgreSQL/H2 支持                                                     |
| `eagle-redis-starter`              | Redisson + Caffeine 多级缓存 + CacheProtectionUtil（穿透/击穿防护）                                        |
| `eagle-resource-server-starter`    | OAuth2 资源服务器 JWT 验证                                                                            |
| `eagle-restclient-starter`         | 同步阻塞 RestClient + `@HttpExchange`（servlet 服务用，含 JWT / 租户 / Seata XID 透传）                       |
| `eagle-webclient-starter`          | 反应式 WebClient + `@HttpExchange`（WebFlux 服务用，同套透传 + 统一错误处理）                                  |
| `eagle-tracing-starter`            | 分布式链路追踪（Brave/Zipkin）                                                                          |
| `eagle-rocketmq-starter`           | RocketMQ v5 消息队列（事务消息、DLQ、AbstractRocketMqListener）                                            |
| `eagle-dynamic-datasource-starter` | 多数据源动态路由（主从切换、@ReadOnly、轮询负载均衡）                                                                |
| `eagle-tenant-starter`             | 多租户支持（COLUMN/DATABASE 隔离模式、TenantContextHolder）                                                |
| `eagle-oss-minio-starter`          | 对象存储（MinIO 8.x，签名 URL、分片上传）                                                                    |
| `eagle-notification-starter`       | 多渠道消息（阿里云 SMS、Spring Mail）                                                                     |
| `eagle-scheduler-starter`          | 分布式定时任务（XXL-JOB 2.4.2）                                                                         |
| `eagle-openapi-starter`            | Swagger/OpenAPI 文档集成（SpringDoc 3.0.2）                                                          |
| `eagle-seata-starter`              | 分布式事务（Seata AT/TCC 2.2.0）                                                                      |
| `eagle-sentinel-starter`           | 流量控制与熔断（Sentinel，网关层限流）                                                                        |
| `eagle-id-generator-starter`       | 分布式 ID 生成（雪花算法 / Leaf 等）                                                                       |
| `eagle-idempotency-starter`        | 接口幂等性（@Idempotent，Redis SETNX + 唯一约束双重保障）                                                      |
| `eagle-elasticsearch-starter`      | Elasticsearch 全文检索（Spring Data ES）                                                             |
| `eagle-websocket-starter`          | WebSocket 实时通信（STOMP + SockJS）                                                                 |
| `eagle-sharding-starter`           | 分库分表（Apache ShardingSphere 5.5.0，YAML 驱动）                                                      |
| `eagle-excel-starter`              | Excel 导入导出（Apache POI，@ExcelColumn，大数据流式写入）                                                    |
| `eagle-resilience-starter`         | 容错弹性（Resilience4J，熔断器 / 重试 / 超时，eagle-default 命名实例）                                            |
| `eagle-encrypt-starter`            | 字段级加密（AES-256，JPA AttributeConverter，@Convert 注解驱动）                                            |
| `eagle-audit-log-starter`          | 操作审计日志（@AuditLog，AOP 切面 + 异步事件 + 可插拔 Handler）                                                  |
| `eagle-ai-starter`                 | AI 集成（Spring AI 2.x，ChatClient、EmbeddingClient）                                                |

Starter 模块设置 `bootJar.enabled = false`、`jar.enabled = true`，依赖使用 `api` 范围暴露传递依赖。

## Spring Modulith 模块划分

当前实际的 `@ApplicationModule` 声明（以各 `package-info.java` 为准）：

| 模块  | 包                        | 所属服务                  | allowedDependencies |
|-----|--------------------------|-----------------------|---------------------|
| 系统管理 | `com.eagle.system.base`    | eagle-system-service  | 未声明（默认全开）           |
| 文件管理 | `com.eagle.system.file`    | eagle-system-service  | 未声明                 |
| 站内消息 | `com.eagle.system.message` | eagle-system-service  | `{}`（完全隔离）          |
| 认证授权 | `com.eagle.auth.core`      | **eagle-auth-service** | `{}`（完全隔离）          |

**协作方式：**

- **auth 已拆为独立服务**，不再是 `com.eagle.system.auth` 子模块——system ↔ auth 走 HTTP client（`infrastructure/remote/`）+ 集成事件，而非 Named Interface
- `message` 与 `auth.core` 已声明 `allowedDependencies = {}`，新增任何跨模块 import 都会让 `ModulithArchitectureTest` 失败；需要协作时加 Port 或走事件，**不要**放宽 `allowedDependencies`
- 模块内协作仍遵循 Port + Adapter 或领域事件两条路径（详见 `.claude/rules/02-architecture.md`）

## DDD 分层架构

每个业务模块内部遵循 `interfaces / application / domain / infrastructure` 四层，依赖方向：
`interfaces → application → domain ← infrastructure`。完整分层结构和规范见 `.claude/rules/02-architecture.md`。

## 关键基类

- `BaseAggregateRoot<T>` — 聚合根：ID (IDENTITY)、审计字段、`@Version` 乐观锁、`registerEvent()` 事件能力
- `BaseEntity` — 子实体：审计字段 + 乐观锁，无事件能力
- `BaseEvent` — 领域事件：time-ordered UUID `eventId` + `occurredOn`
- `ErrorCode` 接口 → 各域枚举实现（`toNotFoundException()` / `toDomainException()` / `toConflictException()` /
  `toServiceException()`）

## Gradle 配置要点

- 所有模块的 AOT 任务（processAot/processTestAot）已禁用
- Hibernate 字节码增强已启用（`enableAssociationManagement`）
- DTO ↔ 领域对象映射统一采用纯 Java `@Component` Mapper（不引入 MapStruct）
- 测试使用 JUnit 5 (JUnit Platform)，Mockito 以 JVM Agent 方式加载（解决 JDK 21+ 动态 Agent 警告）
- 测试超时 5 分钟

## 详细开发规范（按场景查阅）

| 文件                                     | 适用场景                                                            |
|----------------------------------------|-----------------------------------------------------------------|
| `.claude/rules/00-core.md`             | **必看**：中文回答、禁 `@Value`、Lombok 分角色规则、DDD 命名、测试范围、依赖与 Git         |
| `.claude/rules/01-java25.md`           | **必看**：record / sealed / 模式匹配 / Stream Gatherers / 虚拟线程 / ScopedValue |
| `.claude/rules/02-architecture.md`     | DDD 分层、跨域 Port/Adapter、Modulith 边界、领域事件与集成事件契约、Saga             |
| `.claude/rules/03-api-error.md`        | RESTful、`@PreAuthorize` 用法、异常体系、ErrorCode 号段、i18n、OpenAPI        |
| `.claude/rules/04-data.md`             | JPA 实体、禁物理外键、索引唯一性、事务与并发、线程池、Flyway 迁移                          |
| `.claude/rules/05-security.md`         | OAuth2/JWT 取当前用户、脱敏、多租户与数据权限、审计日志、日志规范                          |
| `.claude/rules/06-boot4.md`      | **必看**：Jackson 3 分包、`@AutoConfiguration`、`RestClient`、Security 7 DSL |
| `.claude/rules/07-checklist.md`        | **必看**：高频陷阱速查（Eagle 特有 API）+ PR 前自检清单                           |

缓存、消息队列、分布式事务、定时任务、对象存储、韧性等主题**不设常驻规则文件**，
规范随对应 starter skill（`eagle-redis` / `eagle-rocketmq` / `eagle-seata` / `eagle-scheduler` /
`eagle-oss-minio` / `eagle-resilience`）按需自动加载。

## 项目级 Commands（slash command）

| 命令                | 作用                                                                              |
|-------------------|---------------------------------------------------------------------------------|
| `/eagle-flow`     | **启动 6 阶段端到端流程**（仅手动触发；详见 `eagle-feature-flow` skill）                            |
| `/check-arch`     | Modulith 架构验证 + 模块测试 + 全量构建一键检查                                                 |
| `/new-module`     | 按 DDD 模板创建新业务模块（含 `package-info.java` + 四层骨架）                                   |
| `/new-aggregate`  | 创建聚合根全栈骨架（聚合根 + Repository + ErrorCode + ApplicationService + Controller + DTO） |
| `/new-starter`    | 按 Spring Boot 4 模板创建新 starter 模块                                                |
| `/add-error-code` | 在 ErrorCode 枚举追加常量并同步 i18n 三语翻译                                                 |
| `/verify-rules`   | 校验规则断言与代码实况一致，防止规则腐烂（改 rules、删 starter/模块后必跑）                                  |
| `/new-adr`        | 新建架构决策记录（ADR），记录规则背后的「为什么」                                                     |

## 端到端开发流程（eagle-feature-flow skill）

主干用 **Superpowers 6 阶段**，在 **规划** 与 **写代码** 阶段嵌入式调用本仓库的 rules / commands / starter skills（不使用
OpenSpec）：

| 阶段 | 名称         | 主干调用                                         | agent-plugin 注入                                                                 |
|----|------------|----------------------------------------------|---------------------------------------------------------------------------------|
| 1  | Brainstorm | `superpowers:brainstorming`                  | （无，聚焦需求澄清）                                                                      |
| 2  | Plan       | `superpowers:writing-plans`                  | ★ 必读相关 `.claude/rules/*` + 在 plan 中预定要触发的 commands（`/new-module` 等）             |
| 3  | TDD        | `superpowers:test-driven-development`        | ★ 加载相关 starter skills（`eagle-common` / `eagle-rocketmq` 等）+ 执行 plan 中的 commands |
| 4  | Verify     | `superpowers:verification-before-completion` | ★ 强制 `/check-arch`                                                              |
| 5  | Review     | `superpowers:requesting-code-review`         | ★ 对照 `.claude/rules/07-checklist.md`（高频陷阱 + 自检清单）                            |
| 6  | Finish     | `superpowers:finishing-a-development-branch` | 按 `.claude/rules/00-core.md` 整理 commit + PR 描述                                   |

**设计哲学**：Superpowers 提供工程纪律（brainstorm → plan → TDD → verify → review → finish），
本仓库的 `agent-plugin` 提供 Eagle 平台的"约束"（rules）和"工具箱"（commands + per-starter skills），后者在主流程的关键节点被嵌入式调用。

详见 `agent-plugin/skills/eagle-feature-flow/SKILL.md`。**仅手动触发**：使用 `/eagle-flow [可选功能描述]` 或显式说
"按 eagle flow 走" / "启动 eagle flow"。普通需求描述（"做一个新功能 / 加一个模块 / 重构 X"）不会自动进入 flow，
按常规方式处理即可。
