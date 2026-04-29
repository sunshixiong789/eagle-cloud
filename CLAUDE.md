# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

Eagle Cloud 是一个基于 DDD + 六边形架构 + Spring Modulith 模块化单体构建的 Spring Boot 平台。设计为微服务拆分就绪——领域层通过 Port 接口隔离，拆分时只需替换 `infrastructure/` 层实现。

## 技术栈

- **Java 25** / Gradle 8.x (Groovy DSL)
- **Spring Boot 4.0.3** / Spring Cloud 2025.1.1 / Spring Cloud Alibaba 2025.1.0.0
- **Spring Modulith 2.0.5** — 模块边界静态验证
- **Hibernate 7.2.6** (JPA) / MySQL 9.6.0 / PostgreSQL 42.7.10 / Druid 1.2.28
- **Spring Security + OAuth2 Authorization Server**
- **MapStruct 1.6.3** / Lombok / SpringDoc OpenAPI 3.0.2
- **Redisson 4.3.0** / RocketMQ 2.3.5 / XXL-JOB 2.4.2 / Seata 2.2.0 / MinIO 8.5.17

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

# Spring Modulith 架构验证（PR 前必须通过）
./gradlew :eagle-base-server:eagle-system-server:test --tests "*.ModulithArchitectureTest"

# 运行服务
./gradlew :eagle-base-server:eagle-system-server:bootRun
./gradlew :eagle-base-server:eagle-gateway-server:bootRun
```

## 项目结构

### 可执行服务 (eagle-base-server)

| 服务 | 说明 | 技术栈 |
|------|------|--------|
| `eagle-system-server` | 系统服务：OAuth2 授权服务器、用户/角色/权限管理、微信/短信登录 | JPA, OAuth2 Auth Server, WebSocket, Nacos, Thymeleaf |
| `eagle-gateway-server` | API 网关：路由、JWT 鉴权、限流、链路追踪 | Spring Cloud Gateway (WebFlux), Sentinel, Nacos |

### Starter 库模块 (eagle-starter)

| 模块 | 说明 |
|------|------|
| `eagle-common-starter` | 核心基础设施：基类（BaseAggregateRoot/BaseEntity）、异常体系（AppException/ErrorCode）、领域事件（BaseEvent）、i18n、安全工具 |
| `eagle-data-jpa-starter` | JPA/Hibernate 配置、审计、MySQL/PostgreSQL/H2 支持 |
| `eagle-redis-starter` | Redisson + Caffeine 多级缓存 |
| `eagle-resource-server-starter` | OAuth2 资源服务器 JWT 验证 |
| `eagle-feign-starter` | OpenFeign 客户端配置（含 Seata XID 透传） |
| `eagle-tracing-starter` | 分布式链路追踪（Brave/Zipkin） |
| `eagle-rocketmq-starter` | RocketMQ v5 消息队列 |
| `eagle-data-permission-starter` | 行级数据权限控制（AspectJ） |
| `eagle-dynamic-datasource-starter` | 多数据源动态路由 |
| `eagle-tenant-starter` | 多租户支持（动态数据源路由） |
| `eagle-oss-starter` | 对象存储（MinIO） |
| `eagle-message-starter` | 多渠道消息（阿里云 SMS、Spring Mail） |
| `eagle-xxl-job-starter` | 分布式定时任务（XXL-JOB） |
| `eagle-openapi-starter` | Swagger/OpenAPI 文档集成 |

Starter 模块设置 `bootJar.enabled = false`、`jar.enabled = true`，依赖使用 `api` 范围暴露传递依赖。

## Spring Modulith 模块划分

`eagle-system-server` 中 `com.eagle.system` 下按有界上下文划分为 4 个模块：

| 模块 | 包 | 类型 | 职责 | allowedDependencies |
|------|----|------|------|---------------------|
| **auth** | `com.eagle.system.auth` | 业务域 | 认证授权、OAuth2、微信/短信登录 | `common` |
| **base** | `com.eagle.system.base` | 业务域 | 用户、角色、权限、部门、菜单管理 | `auth::port`, `auth::event`, `common` |
| **config** | `com.eagle.system.config` | 基础设施胶水 | SecurityConfig、CacheConfig、AsyncConfig、WebSocket、i18n、全局异常处理 | `auth::security`, `common` |
| **common** | `com.eagle.system.common` | 共享内核 (OPEN) | ErrorCode 枚举、通用 DTO、异常基础设施 | 无外部依赖 |

**模块间协作方式：**
- auth 定义 Driven Port（`auth/domain/port/`），base 在 `infrastructure/` 层实现适配器——auth 对 base 零依赖
- auth → base 通过领域事件（`AccountRegisteredEvent` 等，放在 `auth/domain/event/`，通过 `@NamedInterface("event")` 暴露）异步解耦
- config 通过 Named Interface `auth::security` 引用安全组件装配过滤链

## DDD 分层架构

每个业务模块内部遵循 `web / application / domain / infrastructure` 四层，依赖方向：`web → application → domain ← infrastructure`。完整分层结构和规范见 `.claude/rules/03-architecture.md`。

## 关键基类

- `BaseAggregateRoot<T>` — 聚合根：ID (IDENTITY)、审计字段、`@Version` 乐观锁、`registerEvent()` 事件能力
- `BaseEntity` — 子实体：审计字段 + 乐观锁，无事件能力
- `BaseEvent` — 领域事件：time-ordered UUID `eventId` + `occurredOn`
- `ErrorCode` 接口 → 各域枚举实现（`toNotFoundException()` / `toDomainException()` / `toConflictException()` / `toServiceException()`）

## Gradle 配置要点

- 所有模块的 AOT 任务（processAot/processTestAot）已禁用
- Hibernate 字节码增强已启用（`enableAssociationManagement`）
- MapStruct 编译参数：`defaultComponentModel=spring`、`unmappedTargetPolicy=IGNORE`
- 测试使用 JUnit 5 (JUnit Platform)，Mockito 以 JVM Agent 方式加载（解决 JDK 21+ 动态 Agent 警告）
- 测试超时 5 分钟

## 详细开发规范

所有编码规范定义在 `.claude/rules/` 目录下，按场景查阅对应文件。
