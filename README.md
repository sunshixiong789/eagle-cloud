# Eagle Cloud

基于 **DDD（领域驱动设计）+ 六边形架构 + Spring Modulith** 构建的 Spring Boot 模块化单体平台，为微服务拆分就绪。

内置完整的 **OAuth2 授权服务器**、**RBAC 权限管理**、**多种第三方登录**（微信小程序 / PC 扫码 / H5 / 短信验证码），开箱即用。

## 特性

- **DDD 分层架构** — 严格的 `web / application / domain / infrastructure` 四层分离，领域逻辑纯净无框架依赖
- **六边形架构（Ports & Adapters）** — 跨域协作通过 Port 接口隔离，拆分微服务时仅替换基础设施层实现，业务代码零改动
- **Spring Modulith 模块治理** — 编译期静态验证模块边界，杜绝循环依赖和非法跨模块访问
- **OAuth2 授权服务器** — 基于 Spring Authorization Server，支持授权码 + PKCE、刷新令牌、微信登录、短信登录
- **RBAC 权限体系** — 用户、角色、权限、部门、菜单、岗位、字典完整管理
- **行级数据权限** — 基于 AspectJ 的细粒度数据访问控制
- **多级缓存** — Redis (Redisson) + Caffeine 两级缓存架构
- **API 网关** — Spring Cloud Gateway + Sentinel 限流 + JWT 鉴权 + 链路追踪
- **统一异常体系** — 类型化异常 + ErrorCode 枚举 + i18n 国际化消息
- **GraalVM Native Image** — 支持原生镜像编译

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 25 |
| 框架 | Spring Boot | 4.0.3 |
| 微服务 | Spring Cloud / Spring Cloud Alibaba | 2025.1.1 / 2025.1.0.0 |
| 模块治理 | Spring Modulith | 2.0.5 |
| ORM | Hibernate (JPA) | 7.2.6 |
| 数据库 | MySQL / PostgreSQL / H2 | 9.x / 42.7 / - |
| 缓存 | Redis (Redisson) + Caffeine | 4.3.0 / 3.2.0 |
| 安全 | Spring Security + OAuth2 Authorization Server | - |
| 网关 | Spring Cloud Gateway + Sentinel | - |
| 注册中心 | Nacos | v3 |
| 消息队列 | Apache RocketMQ | 2.3.5 |
| 分布式事务 | Seata | 2.2.0 |
| 定时任务 | XXL-JOB | 2.4.2 |
| 对象存储 | MinIO | 8.5.17 |
| API 文档 | SpringDoc OpenAPI | 3.0.2 |
| 构建工具 | Gradle (Groovy DSL) | 9.x |

## 项目结构

```
eagle-cloud/
├── eagle-base-server/                  # 可执行服务
│   ├── eagle-system-server/            # 系统服务（OAuth2 + 用户权限管理）
│   ├── eagle-gateway-server/           # API 网关
│   └── docker-compose.yml              # 开发环境容器编排
│
└── eagle-starter/                      # 可复用 Starter 库
    ├── eagle-common-starter/           # 核心基础设施（基类、异常、事件、i18n）
    ├── eagle-data-jpa-starter/         # JPA / Hibernate 配置
    ├── eagle-redis-starter/            # Redis + Caffeine 多级缓存
    ├── eagle-resource-server-starter/  # OAuth2 资源服务器
    ├── eagle-feign-starter/            # OpenFeign 客户端
    ├── eagle-tracing-starter/          # 分布式链路追踪（Brave / Zipkin）
    ├── eagle-rocketmq-starter/         # RocketMQ 消息队列
    ├── eagle-data-permission-starter/  # 行级数据权限
    ├── eagle-dynamic-datasource-starter/ # 多数据源动态路由
    ├── eagle-tenant-starter/           # 多租户支持
    ├── eagle-oss-starter/              # 对象存储（MinIO）
    ├── eagle-message-starter/          # 多渠道消息（短信 / 邮件）
    ├── eagle-xxl-job-starter/          # 分布式定时任务
    └── eagle-openapi-starter/          # Swagger / OpenAPI 文档
```

### Spring Modulith 模块划分

`eagle-system-server` 内按有界上下文划分为 4 个模块：

| 模块 | 职责 |
|------|------|
| **auth** | 认证授权 — OAuth2 授权服务器、微信 / 短信第三方登录、账号管理 |
| **base** | 系统管理 — 用户、角色、权限、部门、菜单、岗位、字典、审计日志 |
| **config** | 全局配置 — Security、Cache、Async、i18n、OpenAPI、WebSocket、全局异常处理 |
| **common** | 共享内核 — 跨域事件契约、ErrorCode 枚举、通用 DTO |

模块间通过 **领域事件** 异步解耦，跨域依赖通过 **六边形 Port 接口** 隔离。

### DDD 分层架构

每个业务模块内部遵循严格的四层架构：

```
{module}/
├── web/                    # 表现层（REST Controller + DTO）
├── application/            # 应用层（用例编排、事务边界、MapStruct）
├── domain/                 # 领域层（聚合根、实体、值对象、仓储接口、领域事件、Port 接口）
└── infrastructure/         # 基础设施层（仓储实现、Port 适配器、安全、配置）
```

依赖方向（单向）：`web → application → domain ← infrastructure`

## 快速开始

### 环境要求

- **Java 25**（推荐使用 [SDKMAN](https://sdkman.io/) 或 [Eclipse Temurin](https://adoptium.net/) 安装）
- **Gradle 9.x**（项目未包含 Gradle Wrapper，需自行安装）
- **MySQL 8.0+**（生产/开发环境）或使用内置 H2（本地开发）

### 方式一：本地零依赖启动（H2 + Caffeine）

无需安装 MySQL、Redis、Nacos，使用内置 H2 数据库 + Caffeine 本地缓存立即运行：

```bash
# 1. 克隆项目
git clone https://gitee.com/sunshixiong/eleganteer-cloud.git
cd eleganteer-cloud

# 2. 构建
gradle build

# 3. 启动系统服务（默认 local profile，使用 H2 + Caffeine）
gradle :eagle-base-server:eagle-system-server:bootRun
```

服务启动后访问：
- **Swagger UI** — http://localhost/swagger-ui.html
- **OAuth2 Token 端点** — http://localhost/oauth2/token
- **API 文档** — http://localhost/v3/api-docs

> 默认管理员账号：`admin` / `123456`

### 方式二：Docker Compose 一键启动

```bash
# 1. 构建应用 JAR
gradle build

# 2. 启动全部服务（MySQL + Redis + Nacos + Gateway + System）
cd eagle-base-server
docker compose up -d
```

| 服务 | 端口 | 说明 |
|------|------|------|
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |
| Nacos | 8848 | 注册中心 / 配置中心 |
| Gateway | 8080 | API 网关入口 |
| System Server | 8082 | 系统服务 |

### 方式三：连接外部 MySQL + Redis

1. 复制环境变量模板：

```bash
cp .env.example .env
```

2. 编辑 `.env` 填入实际配置：

```properties
# MySQL
DB_HOST=localhost
DB_PORT=3306
DB_NAME=eagle
DB_USERNAME=root
DB_PASSWORD=your_password

# Redis
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=

# 第三方服务（可选）
WECHAT_MINI_APP_ID=
WECHAT_MINI_APP_SECRET=
ALIYUN_SMS_ACCESS_KEY_ID=
ALIYUN_SMS_ACCESS_KEY_SECRET=
```

3. 以 dev profile 启动：

```bash
gradle :eagle-base-server:eagle-system-server:bootRun --args='--spring.profiles.active=dev'
```

### 启动网关（可选）

本地开发可直接访问系统服务，如需网关路由：

```bash
# local profile（无需 Nacos）
gradle :eagle-base-server:eagle-gateway-server:bootRun
```

网关地址：http://localhost:8080

## 配置说明

### Profile 配置

| Profile | 数据库 | 缓存 | 注册中心 | 适用场景 |
|---------|--------|------|---------|---------|
| `local`（默认） | H2 文件数据库 | Caffeine 本地缓存 | 禁用 | 本地开发、调试 |
| `dev` | MySQL + HikariCP | Redis (Redisson) | Nacos | 开发环境 |
| `docker` | MySQL (容器) | Redis (容器) | Nacos (容器) | Docker Compose |
| `prod` | MySQL | Redis | Nacos | 生产环境 |

### OAuth2 认证

项目内置 OAuth2 授权服务器，默认注册客户端：

| 配置项 | 值 |
|--------|-----|
| Client ID | `eagleWeb` |
| 授权类型 | `authorization_code` (PKCE), `refresh_token`, `wechat_mini_program`, `sms_code` |
| PKCE | 必须启用（公开客户端） |
| Access Token 有效期 | 1 小时 |
| Refresh Token 有效期 | 30 天 |
| Scopes | `openid`, `profile`, `email`, `address`, `phone` |

### 第三方登录（可选配置）

通过环境变量或 `application.yml` 配置：

- **微信小程序** — `WECHAT_MINI_APP_ID` / `WECHAT_MINI_APP_SECRET`
- **微信 PC 扫码** — `WECHAT_WEB_APP_ID` / `WECHAT_WEB_APP_SECRET`
- **微信 H5 公众号** — `WECHAT_MP_APP_ID` / `WECHAT_MP_APP_SECRET`
- **阿里云短信** — `ALIYUN_SMS_ACCESS_KEY_ID` / `ALIYUN_SMS_ACCESS_KEY_SECRET` / `ALIYUN_SMS_SIGN_NAME` / `ALIYUN_SMS_TEMPLATE_CODE`

## 常用命令

```bash
# 构建
gradle build                    # 构建全部
gradle clean build              # 清理后构建

# 测试
gradle test                     # 运行全部测试
gradle :eagle-base-server:eagle-system-server:test    # 运行指定模块测试
gradle :eagle-base-server:eagle-system-server:test --tests "com.eagle.system.YourTestClass"           # 单个测试类
gradle :eagle-base-server:eagle-system-server:test --tests "com.eagle.system.YourTestClass.testMethod" # 单个测试方法

# 架构验证（PR 前必须通过）
gradle :eagle-base-server:eagle-system-server:test --tests "*.ModulithArchitectureTest"

# 运行服务
gradle :eagle-base-server:eagle-system-server:bootRun   # 系统服务
gradle :eagle-base-server:eagle-gateway-server:bootRun   # 网关服务

# GraalVM Native Image
gradle nativeCompile
```

## API 网关

网关提供统一入口，核心功能：

- **JWT 鉴权** — 全局过滤器校验 Token，将用户信息（`X-User-Id`、`X-Username`、`X-Roles`）透传到下游服务
- **请求日志** — 记录请求方法、路径、状态码、耗时、客户端 IP、链路 Trace ID
- **Sentinel 限流** — 集成 Sentinel 网关限流
- **Seata 事务透传** — 自动传递分布式事务 XID
- **API 文档聚合** — 通过 Nacos 动态发现各服务的 OpenAPI 文档

公开路径（无需认证）：`/public/**`、`/oauth2/**`、`/login`、`/swagger-ui/**`、`/v3/api-docs/**`、`/actuator/health`

## 开发指南

### 新增业务模块

1. 在 `eagle-system-server` 对应包下创建模块目录，遵循 `web / application / domain / infrastructure` 分层
2. 在模块根目录创建 `package-info.java`，声明 `@ApplicationModule` 和 `allowedDependencies`
3. 为需要暴露的子包添加 `@NamedInterface`
4. 运行 `gradle test --tests "*.ModulithArchitectureTest"` 验证模块边界

### 新增错误码

在对应域的 ErrorCode 枚举中添加常量，并在 i18n 消息文件中添加翻译：

```java
ORDER_ITEM_LIMIT_EXCEEDED(30005, "error.order.item_limit", "订单项超出上限");
```

使用时通过工厂方法抛出类型化异常：

```java
throw OrderErrorCode.ORDER_ITEM_LIMIT_EXCEEDED.toDomainException();    // → 400
throw OrderErrorCode.ORDER_NOT_FOUND.toNotFoundException();             // → 404
throw OrderErrorCode.ORDER_DUPLICATE.toConflictException();             // → 409
throw PaymentErrorCode.GATEWAY_ERROR.toServiceException(cause);        // → 500
```

### 编码规范

项目编码规范定义在 `.claude/rules/` 目录下，涵盖命名、架构分层、RESTful API、日志、安全、并发、测试、代码风格、异常处理、数据库、配置注入、模块治理等 12 项规范。

## License

[Apache License 2.0](LICENSE)
