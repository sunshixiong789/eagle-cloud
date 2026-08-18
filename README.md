# Eagle Cloud

基于 **DDD（领域驱动设计）+ 六边形架构 + Spring Modulith** 构建的 Spring Boot 模块化单体平台，为后续拆成微服务预留边界。

当前仓库版本 **`1.9.0-SNAPSHOT`**。平台拆成三个可执行服务：

- **eagle-auth-service** — OAuth2 授权服务器、账号生命周期、第三方登录、短信验证码
- **eagle-system-service** — 用户 / 角色 / 字典 / 日志 / 监控、文件、站内消息、公告
- **eagle-gateway-service** — API 网关（路由、CORS、Redis 限流、OpenAPI 聚合）

内置完整的 **OAuth2 授权服务器**、**RBAC**、多种登录方式（微信小程序 / App / PC 扫码 / H5、短信验证码、淘宝、Apple），以及面向高并发的可复用 Starter。

## 特性

- **DDD 分层** — `interfaces / application / domain / infrastructure` 四层分离，领域逻辑不依赖框架
- **六边形架构（Ports & Adapters）** — 跨域协作走 Port；拆服务时只替换基础设施实现
- **Spring Modulith 模块治理** — 编译期静态验证模块边界，杜绝循环依赖和非法跨模块访问
- **OAuth2 授权服务器** — Spring Authorization Server；授权码 + PKCE、刷新令牌、微信 / 短信 / 淘宝 / Apple
- **RBAC** — 用户、角色、字典、操作日志、在线用户、黑名单
- **多级缓存** — Redis（Redisson）+ Caffeine，内置穿透 / 击穿 / 雪崩防护
- **API 网关** — Spring Cloud Gateway；Request ID 全链路注入、Redis 令牌桶限流、Consul 动态路由
- **统一异常体系** — 类型化异常 + `ErrorCode` 枚举 + 三语 i18n
- **接口幂等** — `TOKEN` / `BUSINESS_KEY` / `RESULT_CACHE` 三种模式，注解驱动
- **分布式 ID** — Snowflake / UUID v7 / TSID / NanoId，以及订单号 / 支付单号语义生成
- **流量治理** — 网关 Redis 限流（集群级）+ `eagle-resilience-starter` 的 `@RateLimit`（单实例）
- **跨服务一致性** — 本地事务 + AFTER_COMMIT 发 AMQP（`publish()` 等 broker confirm，nack / 不可路由抛失败）+ 关键路径 HTTP 降级 + 消费方幂等；**没有 Seata / outbox**
- **实时推送** — STOMP WebSocket + SSE，离线消息存储
- **分库分表** — Apache ShardingSphere YAML 驱动
- **容错弹性** — Resilience4J 熔断 / 重试 / 超时
- **字段级加密** — AES-256 JPA `AttributeConverter`，`@Convert` 渐进迁移
- **操作审计** — `@AuditLog`，AOP + 异步落表
- **全链路压测** — `X-Eagle-Gray` 压测标记跨服务透传
- **GraalVM Native Image** — 支持原生镜像；日常 `gradle build` 不跑 AOT

## 技术栈

| 类别 | 技术 | 版本 |
|---|---|---|
| 语言 | Java | 25 |
| 框架 | Spring Boot | 4.1.0 |
| 微服务 | Spring Cloud | 2025.1.2 |
| 模块治理 | Spring Modulith | 2.1.0 |
| ORM | Hibernate | 7.x（跟随 Boot） |
| 数据库 | PostgreSQL | 16 |
| 缓存 | Redis (Redisson) + Caffeine | 4.7.0 / 3.2.4 |
| 安全 | Spring Security + OAuth2 Authorization Server | — |
| 网关 | Spring Cloud Gateway | — |
| 注册 / 配置中心 | Consul | 1.20 |
| 消息队列 | RabbitMQ / Spring AMQP | 4.2 |
| 定时任务 | XXL-JOB | 3.4.0 |
| 对象存储 | MinIO | 8.5.17 |
| 实时推送 | STOMP WebSocket + SSE | — |
| 分库分表 | Apache ShardingSphere | 5.5.3 |
| 容错弹性 | Resilience4J | 2.4.0 |
| API 文档 | SpringDoc OpenAPI | 3.0.3 |
| 构建 | Gradle（Groovy DSL，无 Wrapper） | 9.x |

## 项目结构

```
eagle-cloud/
├── eagle-bom/                          # 依赖版本对齐
├── docs/
│   ├── contracts/                      # 跨服务集成事件 JSON 契约
│   ├── consul-config.md                # Consul KV / ACL（在 eagle-services/docs/）
│   ├── EagleStarter集中索引.md
│   └── Gradle发布与使用指南.md
│
├── eagle-services/                     # 可执行服务
│   ├── eagle-auth-service/             # 认证服务（:9090）
│   ├── eagle-system-service/           # 系统服务（:8082）
│   ├── eagle-gateway-service/          # API 网关（:8080）
│   ├── docker-compose.yml              # 开发环境：中间件 + 三服务
│   ├── docker-compose.prod.yml         # 生产应用层
│   ├── docker-compose.middleware.yml   # 生产中间件（Redis / Consul / RabbitMQ）
│   └── docker-compose.pg.yml           # 自托管 PostgreSQL
│
└── eagle-starter/                      # 可复用 Starter（共 19 个）
    │
    │   # ── 基础能力 ──
    ├── eagle-common-starter/           # 基类、异常、领域事件、i18n、压测标记
    ├── eagle-data-jpa-starter/         # JPA / Hibernate（审计、多数据库方言）
    ├── eagle-redis-starter/            # Redis + Caffeine（穿透 / 击穿 / 雪崩）
    ├── eagle-resource-server-starter/  # OAuth2 资源服务器 JWT 验证
    ├── eagle-restclient-starter/       # 同步 RestClient + @HttpExchange
    ├── eagle-webclient-starter/        # 反应式 WebClient + @HttpExchange
    ├── eagle-tracing-starter/          # Brave / Zipkin 链路追踪
    ├── eagle-openapi-starter/          # SpringDoc OpenAPI
    │
    │   # ── 数据访问 ──
    ├── eagle-data-r2dbc-starter/       # WebFlux 响应式 R2DBC
    ├── eagle-sharding-starter/         # ShardingSphere YAML 分库分表
    │
    │   # ── 消息与任务 ──
    ├── eagle-amqp-starter/             # RabbitMQ（领域事件 / 重试 / DLQ）
    ├── eagle-scheduler-starter/        # XXL-JOB
    │
    │   # ── 高并发 ──
    ├── eagle-idempotency-starter/      # 接口幂等
    ├── eagle-id-generator-starter/     # 分布式 ID
    ├── eagle-resilience-starter/       # Resilience4J 熔断 / 重试 / 超时 / 限流
    ├── eagle-websocket-starter/        # STOMP + SSE
    │
    │   # ── 平台能力 ──
    ├── eagle-oss-minio-starter/        # MinIO / 本地存储
    ├── eagle-encrypt-starter/          # AES-256 字段加密
    └── eagle-audit-log-starter/        # @AuditLog 操作审计
```

Starter 场景速查与推荐组合见 [`docs/EagleStarter集中索引.md`](docs/EagleStarter集中索引.md)。

> 以下 starter **已清空并移出构建**，不要再引用：`tenant` / `rocketmq` / `dynamic-datasource` / `elasticsearch` / `excel` / `notification` / `seata` / `sentinel` / `ai`。

### Spring Modulith 模块划分

当前实际的 `@ApplicationModule` 声明（以各 `package-info.java` 为准）：

| 模块 | 所属服务 | allowedDependencies |
|---|---|---|
| `com.eagle.system.base` | eagle-system-service | 未声明（默认全开） |
| `com.eagle.system.file` | eagle-system-service | 未声明 |
| `com.eagle.system.message` | eagle-system-service | `{}`（完全隔离） |
| `com.eagle.auth.core` | **eagle-auth-service** | `{}`（完全隔离） |

**auth 已从模块化单体拆为独立服务。** system ↔ auth 走 HTTP client（`infrastructure/remote/`）+ AMQP 集成事件，不再是 Named Interface。事件字段契约在 `docs/contracts/`，由双方各自声明消息类，靠 JSON 字段名兼容。

模块内协作通过 **领域事件** 异步解耦，跨域依赖通过 **六边形 Port** 隔离。边界由 `ModulithArchitectureTest`（模块间）+ `LayeredArchitectureTest`（模块内 DDD 分层）双重静态验证。

### DDD 分层

```
{module}/
├── interfaces/             # REST Controller + record DTO
├── application/            # 用例编排、事务边界、纯 Java Mapper
├── domain/                 # 聚合根、实体、仓储接口、领域事件、Port
└── infrastructure/         # JPA、远程调用、MQ、缓存、安全、配置
```

依赖方向（单向）：`interfaces → application → domain ← infrastructure`

## 高并发 Starter 速览

### eagle-idempotency-starter — 接口幂等

```java
@PostMapping("/orders")
@Idempotent(mode = IdempotencyMode.TOKEN)
public OrderResponse createOrder(@RequestBody CreateOrderRequest req) { ... }

@Idempotent(mode = IdempotencyMode.BUSINESS_KEY, key = "#req.orderNo")
public void payOrder(@RequestBody PayRequest req) { ... }

@Idempotent(mode = IdempotencyMode.RESULT_CACHE, tokenHeader = "X-Idempotency-Key")
public PayResult submitPay(@RequestBody PayRequest req) { ... }
```

申请 Token（TOKEN / RESULT_CACHE 模式）：`GET /idempotency/token`

### eagle-id-generator-starter — 分布式 ID

```java
private final IdGeneratorFacade idGen;

long orderId   = idGen.snowflakeId();
String orderNo = idGen.orderNo("ORD");   // ORD20260430123456789
String payNo   = idGen.payNo();          // PAY...
String refundNo = idGen.refundNo();      // RFD...
```

### eagle-resilience-starter — 限流 / 熔断

`@RateLimit` 取代已移除的 Sentinel。限流是**单实例**计数；集群级限流放在网关 Redis 令牌桶。

```java
@RateLimit(resource = "createOrder", qps = 100)
public OrderResponse createOrder(...) { ... }

@RateLimit(qps = 10, behavior = RateLimitBehavior.QUEUEING, maxQueueingTimeMs = 1000)
public void sendSms(String phone) { ... }
```

触发限流返回 HTTP 429。不再提供 `WARM_UP`。

### eagle-websocket-starter — 实时推送

```java
wsManager.sendToUser(userId, "/queue/order-status", orderStatusDto);

@GetMapping(value = "/sse/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter subscribe(@PathVariable String userId) {
    return sseManager.connect(userId);
}
sseManager.sendToUser(userId, "ORDER_UPDATE", orderDto);
sseManager.broadcast("ANNOUNCEMENT", announcementDto);
```

客户端：

```javascript
const client = new Client({ brokerURL: 'ws://localhost:8080/system/ws-stomp' });
client.subscribe('/user/queue/order-status', msg => { ... });

const es = new EventSource('/sse/' + userId);
es.addEventListener('ORDER_UPDATE', e => console.log(JSON.parse(e.data)));
```

## 快速开始

### 环境要求

- **Java 25**（推荐 [SDKMAN](https://sdkman.io/) 或 [Eclipse Temurin](https://adoptium.net/)）
- **Gradle 9.x**（仓库不含 Gradle Wrapper，需本机安装）
- 开发联调还需要能访问 **Consul**（配置中心已开 ACL，启动必须带 `CONSUL_TOKEN`）

仓库里**没有** `.env` 文件。中间件地址、库凭据、短信密钥等在 Consul KV；compose 只保留引导参数（Consul 地址、profile、端口、库名）。详见 [`eagle-services/docs/consul-config.md`](eagle-services/docs/consul-config.md)。

### 方式一：Docker Compose 一键启动（推荐）

在 `eagle-services/` 下启动 PostgreSQL + Redis + Consul + RabbitMQ + 三个应用：

```bash
cd eagle-services
read -rs CONSUL_TOKEN && export CONSUL_TOKEN
docker compose -f docker-compose.yml up -d --build
```

不带 token 也能起容器，但 ACL 开启后服务读 KV 会 403、注册失败。

| 服务 | 端口 | 说明 |
|---|---|---|
| PostgreSQL | 5432 | 库：`eagle_system` / `eagle_auth` |
| Redis | 6379 | 缓存 / 网关限流 / Token 黑名单 |
| Consul | 8500 | 注册中心 + KV 配置中心（UI 需登录 token） |
| RabbitMQ | 5672 / 15672 | AMQP + 管理台 |
| auth | 9090 | 认证服务（JDWP `5105`） |
| system | 8082 | 系统服务（JDWP `5106`） |
| gateway | 8080 | API 网关入口（JDWP `5107`） |

对外统一走网关：

- **Swagger UI（聚合）** — http://localhost:8080/swagger-ui.html
- **OAuth2 Token** — http://localhost:8080/oauth2/token
- **OAuth2 Authorize** — http://localhost:8080/oauth2/authorize
- **JWK Set** — http://localhost:8080/oauth2/jwks
- **WebSocket / STOMP** — `ws://localhost:8080/system/ws-stomp`
- **Consul UI** — http://localhost:8500
- **RabbitMQ 管理台** — http://localhost:15672

### 方式二：本机跑服务，中间件用开发环境

`local` profile 默认把数据源 / Redis / RabbitMQ 指到开发机公网地址，凭据从开发环境 Consul KV 读取，**本机不注册进 Consul**（避免把死实例挂到 dev 网关）。

```bash
export CONSUL_TOKEN=<只读 config/ 前缀的 service token>
gradle :eagle-services:eagle-auth-service:bootRun --args='--spring.profiles.active=local'
gradle :eagle-services:eagle-system-service:bootRun --args='--spring.profiles.active=local'
gradle :eagle-services:eagle-gateway-service:bootRun --args='--spring.profiles.active=local'
```

未提供 token 时 `optional:consul:` 会静默跳过，auth 会在缺 `EAGLE_ADMIN_PASSWORD` 时启动失败。

连本机 compose 中间件时，额外覆盖地址：

```bash
export LOCAL_DB_HOST=localhost LOCAL_REDIS_HOST=localhost LOCAL_RABBITMQ_HOST=localhost
```

> `local` 连的是开发环境真库时，`ddl-auto=update` 会改 dev 表结构。改实体前确认影响，或设 `LOCAL_DDL_AUTO=validate`。

### 方式三：只起中间件，本机跑应用

```bash
cd eagle-services
docker compose -f docker-compose.yml up -d postgres redis consul rabbitmq
# 再按方式二启动三个 bootRun，并把 LOCAL_* 指到 localhost
```

生产拆分部署用 `docker-compose.pg.yml` + `docker-compose.middleware.yml` + `docker-compose.prod.yml`，不要和开发 compose 混用。

## Profile

| Profile | 数据源 | 缓存 | 注册 / 配置 | 适用场景 |
|---|---|---|---|---|
| `local` | 覆盖为可达地址（默认开发机公网，可改 localhost） | Redis | Consul KV 开、discovery 关 | 本机开发，复用远程中间件 |
| `dev` | PostgreSQL（容器名 / KV） | Redis | Consul 注册 + KV | 开发环境 compose |
| `prod` | PostgreSQL | Redis | Consul 注册 + KV | 生产；`ddl-auto=validate`，Swagger 关闭 |

基线 `application.yml` 的安全默认是生产口径（`validate` / 不打 SQL / health 不暴露细节）。`dev` / `local` 再显式打开调试便利。

Schema 当前由 Hibernate 同步，**尚未引入 Flyway**。生产禁止 `ddl-auto: update`。

## OAuth2 认证

项目内置 OAuth2 授权服务器（在 **auth-service**，不再在 system）。默认客户端：

| 客户端 | Client ID | 授权类型 | 说明 |
|---|---|---|---|
| Web 前端 | `eagleWeb` | `authorization_code`（必须 PKCE）、`refresh_token`、`wechat_mini_program`、`sms_code` | 公开客户端 |
| App | `eagleApp` | `refresh_token`、`sms_code`、`wechat_app`、`wechat_mini_program`、`taobao_app`、`apple_app`、`social_bind` | 公开客户端 |
| 运营系统 | `shengxinOps` | `client_credentials` | 默认关闭，需显式注入 secret |

| 配置项 | 值 |
|---|---|
| Access Token 有效期 | 1 小时 |
| Refresh Token 有效期 | 30 天 |
| Scopes | `openid`、`profile`、`email`、`address`、`phone` |

`phone_one_click` 已下线：没有真实运营商 provider，只剩 mock（token 即手机号），对公开客户端等于任意手机号可换 token。

第三方登录通过 Consul KV / 环境变量配置：

- 微信小程序 — `WECHAT_MINI_APP_ID` / `WECHAT_MINI_APP_SECRET`
- 微信 App — `WECHAT_APP_APP_ID` / `WECHAT_APP_APP_SECRET`
- 微信 PC 扫码 — `WECHAT_WEB_APP_ID` / `WECHAT_WEB_APP_SECRET`
- 微信 H5 — `WECHAT_MP_APP_ID` / `WECHAT_MP_APP_SECRET`
- 短信 — 由 auth-service 直接对接，不再经过已移除的 `eagle-notification-starter`

## 常用命令

```bash
# 构建
gradle build
gradle clean build

# 测试
gradle test
gradle :eagle-services:eagle-system-service:test
gradle :eagle-services:eagle-auth-service:test
gradle :eagle-services:eagle-gateway-service:test

# 架构验证（PR 前必须通过）
gradle :eagle-services:eagle-system-service:test --tests "*ModulithArchitectureTest"
gradle :eagle-services:eagle-auth-service:test --tests "*ModulithArchitectureTest"

# 运行服务
gradle :eagle-services:eagle-auth-service:bootRun
gradle :eagle-services:eagle-system-service:bootRun
gradle :eagle-services:eagle-gateway-service:bootRun

# 发布 starter / BOM 到本机 Maven
gradle publishToMavenLocal

# GraalVM Native Image（会启用 processAot）
gradle :eagle-services:eagle-auth-service:nativeCompile
```

## API 网关

网关只做 **路由 + 流量治理**。JWT 验签与用户身份提取由下游 `eagle-resource-server-starter` 负责。

核心能力：

- **请求增强** — `RequestEnrichmentGlobalFilter` 注入 `X-Request-Id` + `X-Real-IP`
- **内部路径拦截** — `InternalPathBlockingGlobalFilter` 拒绝外部访问任何含 `/internal/` 的路径
- **请求日志** — 方法、路径、状态码、耗时、客户端 IP、Trace ID
- **Redis 限流** — Spring Cloud Gateway `RequestRateLimiter` + Redis 令牌桶（集群级，取代 Sentinel）
- **动态路由** — `discovery.locator` 按 Consul 服务名生成 `/auth/**`、`/system/**`；OAuth2 规范路径单独透传
- **WebSocket** — `/system/ws-stomp` → `lb:ws://system`
- **OpenAPI 聚合** — 按实例 metadata `spring-doc` 动态注册文档组
- **统一错误响应** — `GatewayWebExceptionHandler` 输出含 `requestId` 的 JSON

服务间调用走 `lb://{service-id}/internal/...`，不经网关。

详细配置见 [`eagle-services/eagle-gateway-service/README.md`](eagle-services/eagle-gateway-service/README.md)。

## 资源服务器依赖选择

`eagle-resource-server-starter` 只提供 OAuth2 JWT 资源服务器能力，不替业务应用选择 Web 栈。每个资源服务器必须自己选 Web 栈、文档栈和数据访问栈。公开路径写在 yml `eagle.resource-server.permit-paths`，不要写 `@PreAuthorize("permitAll()")`。

### Servlet / Spring MVC

```gradle
dependencies {
    implementation platform('com.eagle:eagle-bom')
    implementation 'com.eagle:eagle-resource-server-starter'
    implementation 'org.springframework.boot:spring-boot-starter-webmvc'
    implementation 'com.eagle:eagle-openapi-starter'
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui'
    implementation 'com.eagle:eagle-data-jpa-starter'
    implementation 'com.eagle:eagle-redis-starter'
    implementation 'com.eagle:eagle-tracing-starter'
}
```

### Reactive / WebFlux

不要同时引入 `spring-boot-starter-webmvc` 和 `spring-boot-starter-webflux`。

```gradle
dependencies {
    implementation platform('com.eagle:eagle-bom')
    implementation 'com.eagle:eagle-resource-server-starter'
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    implementation 'com.eagle:eagle-openapi-starter'
    implementation 'org.springdoc:springdoc-openapi-starter-webflux-ui'
    implementation 'com.eagle:eagle-data-r2dbc-starter'
    implementation 'com.eagle:eagle-redis-starter'
    implementation 'com.eagle:eagle-tracing-starter'
}
```

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: ${AUTH_SERVER_JWK_SET_URI:http://localhost:9090/oauth2/jwks}

eagle:
  resource-server:
    permit-paths:
      - /public/**
      - /actuator/health
      - /actuator/info
```

推荐配 `jwk-set-uri`（懒加载）。`issuer-uri` 启动时会立刻拉 OIDC discovery，auth 未就绪会启动失败。

## 开发指南

### 新增业务模块

1. 在对应服务包下创建模块目录，遵循四层分层
2. 在模块根目录创建 `package-info.java`，声明 `@ApplicationModule` 和 `allowedDependencies`
3. 需要对外暴露的子包加 `@NamedInterface`
4. 跑对应服务的 `*ModulithArchitectureTest` 和 `*LayeredArchitectureTest`

跨服务协作只允许：调用方 `domain/port/` + 对方 HTTP/AMQP 适配器，或各自声明集成事件类。

### 新增错误码

在对应域的 `ErrorCode` 枚举中加常量，并同步三语消息文件。业务域新号段用 **50000–89999**，不要复用已占用号段（见 `AGENTS.md` 索引的 `03-api-error.md`）。

```java
throw OrderErrorCode.ORDER_ITEM_LIMIT_EXCEEDED.toDomainException();    // 400
throw OrderErrorCode.ORDER_NOT_FOUND.toNotFoundException();            // 404
throw OrderErrorCode.ORDER_DUPLICATE.toConflictException();            // 409
throw PaymentErrorCode.GATEWAY_ERROR.toServiceException(cause);        // 500
```

资源不存在必须用 `toNotFoundException()`，不要一律 `DomainException`。

### AI 开发规范

Claude Code 与 Codex 共用同一套规则，无需装插件：

- `AGENTS.md` — 项目规则入口
- `.agents/rules/` — 按场景读取的规则真源（`.claude/rules/` 是兼容链接）
- `.agents/skills/eagle-cloud/` — 统一 Skill，按任务加载 `references/*.md`

## 相关文档

| 文档 | 内容 |
|---|---|
| [eagle-auth-service/README.md](eagle-services/eagle-auth-service/README.md) | 认证服务、OAuth2、第三方登录 |
| [eagle-system-service/README.md](eagle-services/eagle-system-service/README.md) | 系统服务、RBAC、消息、文件 |
| [eagle-gateway-service/README.md](eagle-services/eagle-gateway-service/README.md) | 网关路由、限流、OpenAPI 聚合 |
| [consul-config.md](eagle-services/docs/consul-config.md) | Consul KV 布局与 ACL |
| [EagleStarter集中索引.md](docs/EagleStarter集中索引.md) | Starter 场景速查 |
| [Gradle发布与使用指南.md](docs/Gradle发布与使用指南.md) | BOM / Starter 发布 |
| [rabbitmq-dlq-policy.md](docs/rabbitmq-dlq-policy.md) | 死信策略 |

## License

[Apache License 2.0](LICENSE)
