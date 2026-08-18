# Eagle Starter 集中索引

> 本文档是所有 **仍在构建中** 的 starter 目录。每个 starter 的详细用法见各自 `USAGE.md` 或 `README.md`。
>
> 业务项目接入：
> 1. 引 `eagle-bom`（管理版本，当前 `1.9.0-SNAPSHOT`）
> 2. 按下表按场景挑选 starter
> 3. 查阅对应 `USAGE.md` / `README.md`

仓库里若还看得到 `eagle-tenant-starter`、`eagle-rocketmq-starter` 等空目录，那是历史残留，**不在 `settings.gradle`，不要 import**。

## 按场景速查

| 场景 | Starter | 关键能力 |
|---|---|---|
| **核心基础（必引）** | [eagle-common-starter](../eagle-starter/eagle-common-starter/USAGE.md) | 基类、异常、领域事件、`DistributedLock` 接口、i18n、压测标记 |
| **数据访问（JPA）** | [eagle-data-jpa-starter](../eagle-starter/eagle-data-jpa-starter/USAGE.md) | JPA 配置、审计字段、多数据库方言 |
| **数据访问（R2DBC）** | [eagle-data-r2dbc-starter](../eagle-starter/eagle-data-r2dbc-starter/USAGE.md) | WebFlux 响应式仓库、响应式审计 |
| **缓存 / 锁 / 限流** | [eagle-redis-starter](../eagle-starter/eagle-redis-starter/USAGE.md) | Redisson + Caffeine、击穿 / 穿透防护、布隆过滤器 |
| **消息队列** | [eagle-amqp-starter](../eagle-starter/eagle-amqp-starter/) | RabbitMQ 领域事件、重试、DLQ |
| **分布式 ID** | [eagle-id-generator-starter](../eagle-starter/eagle-id-generator-starter/USAGE.md) | Snowflake / UUID v7 / TSID / 业务单号 |
| **接口幂等** | [eagle-idempotency-starter](../eagle-starter/eagle-idempotency-starter/USAGE.md) | `@Idempotent`：TOKEN / BUSINESS_KEY / RESULT_CACHE |
| **OAuth2 资源服务器** | [eagle-resource-server-starter](../eagle-starter/eagle-resource-server-starter/USAGE.md) | JWT 鉴权、`SecurityUtils`、`EagleUser` |
| **服务间 RPC（同步）** | [eagle-restclient-starter](../eagle-starter/eagle-restclient-starter/USAGE.md) | RestClient + `@HttpExchange`、Token 自动透传、错误转换 |
| **服务间 RPC（反应式）** | [eagle-webclient-starter](../eagle-starter/eagle-webclient-starter/README.md) | WebClient + `@HttpExchange`，WebFlux 用 |
| **链路追踪** | [eagle-tracing-starter](../eagle-starter/eagle-tracing-starter/USAGE.md) | Brave / B3 / Zipkin、MDC |
| **OpenAPI 文档** | [eagle-openapi-starter](../eagle-starter/eagle-openapi-starter/USAGE.md) | SpringDoc 3、分组、JWT Security Scheme |
| **对象存储** | [eagle-oss-minio-starter](../eagle-starter/eagle-oss-minio-starter/USAGE.md) | MinIO + 本地降级、签名 URL、上传校验 |
| **定时任务** | [eagle-scheduler-starter](../eagle-starter/eagle-scheduler-starter/USAGE.md) | XXL-JOB 3.4 |
| **分库分表** | [eagle-sharding-starter](../eagle-starter/eagle-sharding-starter/) | ShardingSphere YAML |
| **限流熔断** | [eagle-resilience-starter](../eagle-starter/eagle-resilience-starter/) | `@RateLimit`、Resilience4J 熔断 / 重试 / 超时 |
| **字段加密** | [eagle-encrypt-starter](../eagle-starter/eagle-encrypt-starter/) | AES-256 JPA `@Convert` |
| **操作审计** | [eagle-audit-log-starter](../eagle-starter/eagle-audit-log-starter/) | `@AuditLog`、异步落表 |
| **WebSocket / SSE** | [eagle-websocket-starter](../eagle-starter/eagle-websocket-starter/USAGE.md) | STOMP、离线消息、SSE |

## 已移除（不要再引用）

| 原 starter | 现状 |
|---|---|
| `eagle-tenant-starter` | 多租户整体移除，无 `TenantContextHolder` |
| `eagle-rocketmq-starter` | 由 `eagle-amqp-starter`（RabbitMQ）替代 |
| `eagle-dynamic-datasource-starter` | 已移除；分库分表用 `eagle-sharding-starter` |
| `eagle-elasticsearch-starter` | 已移除 |
| `eagle-excel-starter` | 已移除；需要 POI 时业务模块直接引 `poi-ooxml` |
| `eagle-notification-starter` | 已移除；短信由 auth-service 直连 |
| `eagle-seata-starter` | 已移除；跨服务一致性走本地事务 + AMQP 集成事件 |
| `eagle-sentinel-starter` | 已移除；方法级限流用 `eagle-resilience-starter`，网关用 Redis 令牌桶 |
| `eagle-ai-starter` | 已移除 |

## 推荐组合

### 标准业务服务（CRUD + 鉴权 + 文档 + 监控）

```gradle
implementation platform('com.eagle:eagle-bom')

implementation 'com.eagle:eagle-common-starter'
implementation 'com.eagle:eagle-data-jpa-starter'
implementation 'com.eagle:eagle-redis-starter'
implementation 'com.eagle:eagle-resource-server-starter'
implementation 'com.eagle:eagle-openapi-starter'
implementation 'com.eagle:eagle-tracing-starter'
implementation 'com.eagle:eagle-restclient-starter'      // servlet
// 或反应式：implementation 'com.eagle:eagle-webclient-starter'
```

运行时数据库：本仓库三个服务只用 PostgreSQL。starter 层仍传递 MySQL / H2 驱动，服务层已排除。

### 高并发交易服务（订单 / 支付）

```gradle
implementation 'com.eagle:eagle-amqp-starter'
implementation 'com.eagle:eagle-idempotency-starter'
implementation 'com.eagle:eagle-id-generator-starter'
implementation 'com.eagle:eagle-resilience-starter'
```

跨服务不要上 Seata。本地事务内 `registerEvent()`，AFTER_COMMIT 发集成事件，消费方按 `eventId` 幂等。

### 实时推送服务

```gradle
implementation 'com.eagle:eagle-common-starter'
implementation 'com.eagle:eagle-redis-starter'
implementation 'com.eagle:eagle-websocket-starter'
implementation 'com.eagle:eagle-resource-server-starter'
```

## Starter 间依赖关系

```
                       eagle-common-starter（所有 starter 依赖）
                              ▲
              ┌───────────────┼────────────────────┐
              │               │                    │
        eagle-redis      eagle-amqp           其他业务 starter
        (DistributedLock 实现)  (集成事件)
```

- `eagle-common-starter` 是基础，其他 starter 都依赖它
- `eagle-redis-starter` 提供 `DistributedLock` 的 Redisson 实现
- `eagle-tracing-starter` 与 restclient / webclient / amqp 自动协作透传 traceId
- `eagle-idempotency-starter` 依赖 Redis 存 Token
- `eagle-websocket-starter` 集群部署依赖 Redis（跨实例广播 + 离线消息）

## 开发规范联动

规则真源在 `.agents/rules/`（`.claude/rules/` 是兼容链接）。AI 编程按「功能 → starter → 规则」深入。

| 规则 | 关联 starter |
|---|---|
| `00-core.md` / `01-java25.md` | 全部 |
| `02-architecture.md` | `eagle-common-starter` / `eagle-amqp-starter`（集成事件契约） |
| `03-api-error.md` | `eagle-common-starter`（ErrorCode）/ `eagle-openapi-starter` |
| `04-data.md` | `eagle-data-jpa-starter` / `eagle-data-r2dbc-starter` |
| `05-security.md` | `eagle-resource-server-starter` |
| `06-boot4.md` | `eagle-restclient-starter` / `eagle-webclient-starter`（含自动配置写法） |

缓存、消息、调度、对象存储、韧性的规范在对应 Skill reference（`.agents/skills/eagle-cloud/references/`），不要再去翻已删除 starter 的旧文档。

## 接入新项目

```
1) 引 BOM
   implementation platform('com.eagle:eagle-bom:1.9.0-SNAPSHOT')

2) 按场景挑 starter（见「推荐组合」）

3) 配 application.yml
   JWT 用 jwk-set-uri 指向 auth-service
   公开路径写 eagle.resource-server.permit-paths

4) 抄 USAGE.md 最小示例验证

5) 读关联规则，避免套用已移除的 Tenant / Seata / Sentinel / Nacos / RocketMQ API
```
