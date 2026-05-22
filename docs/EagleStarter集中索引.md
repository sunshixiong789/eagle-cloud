# Eagle Starter 集中索引

> 本文档是所有 starter 的集中目录。每个 starter 详细使用方法见各自 `USAGE.md`。
>
> 业务项目接入基础架子时：
> 1. 引 `eagle-bom`（管理版本）
> 2. 按场景挑选下表 starter 引入
> 3. 详细 API、配置、示例查阅对应 starter 的 `USAGE.md`

## 按场景速查

| 场景                   | Starter                                                                                        | 关键能力                                                 |
|----------------------|------------------------------------------------------------------------------------------------|------------------------------------------------------|
| **核心基础（必引）**         | [eagle-common-starter](../eagle-starter/eagle-common-starter/USAGE.md)                         | 基类、异常、领域事件、分布式锁接口、i18n                               |
| **数据访问（JPA）**        | [eagle-data-jpa-starter](../eagle-starter/eagle-data-jpa-starter/USAGE.md)                     | JPA 配置、审计、多数据库                                       |
| **数据访问（MyBatis）**    | [eagle-mybatis-starter](../eagle-starter/eagle-mybatis-starter/USAGE.md)                       | MyBatis-Plus、慢 SQL、通用 CRUD                           |
| **多数据源 / 读写分离**      | [eagle-dynamic-datasource-starter](../eagle-starter/eagle-dynamic-datasource-starter/USAGE.md) | `@ReadOnly`、动态路由                                     |
| **Elasticsearch 检索** | [eagle-elasticsearch-starter](../eagle-starter/eagle-elasticsearch-starter/USAGE.md)           | 文档基类、查询构造器、分页高亮                                      |
| **缓存 / 锁 / 限流**      | [eagle-redis-starter](../eagle-starter/eagle-redis-starter/USAGE.md)                           | Redisson + Caffeine、击穿/穿透防护、布隆过滤器                    |
| **消息队列**             | [eagle-rocketmq-starter](../eagle-starter/eagle-rocketmq-starter/USAGE.md)                     | 事件发布、事务消息、死信、分布式锁                                    |
| **分布式 ID**           | [eagle-id-generator-starter](../eagle-starter/eagle-id-generator-starter/USAGE.md)             | 雪花 / 号段 / TSID / 业务单号                                |
| **接口幂等**             | [eagle-idempotency-starter](../eagle-starter/eagle-idempotency-starter/USAGE.md)               | `@Idempotent`、Token 模式、Key 模式                        |
| **多租户**              | [eagle-tenant-starter](../eagle-starter/eagle-tenant-starter/USAGE.md)                         | 租户上下文、`@TenantFilter`、数据源路由                          |
| **行级数据权限**           | [eagle-row-security-starter](../eagle-starter/eagle-row-security-starter/USAGE.md)             | `@DataPermission`、部门 / 本人 / 自定义范围                    |
| **OAuth2 资源服务器**     | [eagle-resource-server-starter](../eagle-starter/eagle-resource-server-starter/USAGE.md)       | JWT 鉴权、`SecurityUtils`、`@PreAuthorize`               |
| **服务间 RPC（同步）**    | [eagle-restclient-starter](../eagle-starter/eagle-restclient-starter/USAGE.md)                 | 同步 RestClient + `@HttpExchange`、Token / 租户 / XID 自动透传、错误转换 |
| **服务间 RPC（反应式）**  | [eagle-webclient-starter](../eagle-starter/eagle-webclient-starter/USAGE.md)                   | 反应式 WebClient + `@HttpExchange`、同套透传 + 统一错误处理（WebFlux 用）   |
| **链路追踪**             | [eagle-tracing-starter](../eagle-starter/eagle-tracing-starter/USAGE.md)                       | Brave / B3 / Zipkin、MDC 注入                           |
| **OpenAPI 文档**       | [eagle-openapi-starter](../eagle-starter/eagle-openapi-starter/USAGE.md)                       | SpringDoc 3.0、分组、JWT Security Scheme                 |
| **对象存储**             | [eagle-oss-minio-starter](../eagle-starter/eagle-oss-minio-starter/USAGE.md)                   | MinIO + 本地降级、签名 URL、上传校验                             |
| **消息通知**             | [eagle-notification-starter](../eagle-starter/eagle-notification-starter/USAGE.md)             | 短信 + 邮件、模板、多渠道                                       |
| **支付**               | [eagle-payment-starter](../eagle-starter/eagle-payment-starter/USAGE.md)                       | 支付宝 / 微信支付、退款、异步通知                                   |
| **定时任务**             | [eagle-scheduler-starter](../eagle-starter/eagle-scheduler-starter/USAGE.md)                   | XXL-JOB、分片、调度中心                                      |
| **分布式事务**            | [eagle-seata-starter](../eagle-starter/eagle-seata-starter/USAGE.md)                           | Seata AT / TCC、XID 透传                                |
| **限流熔断**             | [eagle-sentinel-starter](../eagle-starter/eagle-sentinel-starter/USAGE.md)                     | `@RateLimit`、Sentinel 控制台                            |
| **WebSocket / SSE**  | [eagle-websocket-starter](../eagle-starter/eagle-websocket-starter/USAGE.md)                   | STOMP、离线消息、SSE                                       |

## 推荐组合

### 标准业务服务（CRUD + 鉴权 + 文档 + 监控）

```gradle
implementation platform('com.eagle:eagle-bom')

implementation project(':eagle-starter:eagle-common-starter')          // 必引
implementation project(':eagle-starter:eagle-data-jpa-starter')        // 持久化
implementation project(':eagle-starter:eagle-redis-starter')           // 缓存 + 锁
implementation project(':eagle-starter:eagle-resource-server-starter') // 鉴权
implementation project(':eagle-starter:eagle-openapi-starter')         // 文档
implementation project(':eagle-starter:eagle-tracing-starter')         // 追踪
implementation project(':eagle-starter:eagle-restclient-starter')      // 同步 RPC（servlet 服务）
// 或反应式服务：implementation project(':eagle-starter:eagle-webclient-starter')
runtimeOnly 'mysql:mysql-connector-j'
```

### 多租户 SaaS

```gradle
// 在标准业务服务基础上叠加
implementation project(':eagle-starter:eagle-tenant-starter')          // 租户隔离
implementation project(':eagle-starter:eagle-row-security-starter')    // 数据权限
```

### 高并发交易服务（订单 / 支付）

```gradle
// 在标准业务服务基础上叠加
implementation project(':eagle-starter:eagle-rocketmq-starter')        // 异步事件
implementation project(':eagle-starter:eagle-idempotency-starter')     // 接口幂等
implementation project(':eagle-starter:eagle-id-generator-starter')    // 业务单号
implementation project(':eagle-starter:eagle-sentinel-starter')        // 限流
implementation project(':eagle-starter:eagle-seata-starter')           // 分布式事务（按需）
implementation project(':eagle-starter:eagle-payment-starter')         // 支付（按需）
```

### 检索 / 大数据服务

```gradle
implementation project(':eagle-starter:eagle-common-starter')
implementation project(':eagle-starter:eagle-elasticsearch-starter')
implementation project(':eagle-starter:eagle-resource-server-starter')
implementation project(':eagle-starter:eagle-scheduler-starter')       // 定时索引同步
```

### 实时推送服务

```gradle
implementation project(':eagle-starter:eagle-common-starter')
implementation project(':eagle-starter:eagle-redis-starter')           // 集群广播 + 离线
implementation project(':eagle-starter:eagle-websocket-starter')
implementation project(':eagle-starter:eagle-resource-server-starter')
```

## Starter 间依赖关系

```
                       eagle-common-starter（所有 starter 依赖）
                              ▲
              ┌───────────────┼────────────────────┐
              │               │                    │
        eagle-redis      eagle-rocketmq      其他业务 starter
        (DistributedLock 实现)              (按需)
              │               │
              └─── 提供分布式锁 + 事件发布 ───┘
```

- `eagle-common-starter` 是**基础**，所有其他 starter 都依赖
- `eagle-redis-starter` 提供 `DistributedLock` 默认实现（Redisson）
- `eagle-rocketmq-starter` 也可作为 `DistributedLock` 备选实现
- `eagle-tracing-starter` 与 `eagle-restclient-starter` / `eagle-webclient-starter` / `eagle-rocketmq-starter` 自动协作（traceId 透传）
- `eagle-tenant-starter` 与 `eagle-row-security-starter` 互补（租户 + 行级权限）
- `eagle-idempotency-starter` 依赖 `eagle-redis-starter`（Token 存储）
- `eagle-websocket-starter` 集群部署依赖 `eagle-redis-starter`（跨实例广播 + 离线消息）

## 开发规范联动

每份 `USAGE.md` 末尾都有"关联规则"指向 `.claude/rules/` 中对应规范文件。AI 编程时可按"功能 → starter → 规则"链路深入。

| 规则                                                   | 关联 starter                                            |
|------------------------------------------------------|-------------------------------------------------------|
| `.claude/rules/03-architecture.md`、`07-exception.md` | `eagle-common-starter`                                |
| `.claude/rules/06-database.md`                       | `eagle-data-jpa-starter` / `eagle-mybatis-starter`    |
| `.claude/rules/11-feign.md`                          | `eagle-restclient-starter` / `eagle-webclient-starter` |
| `.claude/rules/12-security.md`                       | `eagle-resource-server-starter`                       |
| `.claude/rules/14-cache.md`                          | `eagle-redis-starter`                                 |
| `.claude/rules/15-messaging.md`                      | `eagle-rocketmq-starter`                              |
| `.claude/rules/16-transaction-distributed.md`        | `eagle-seata-starter` / `eagle-rocketmq-starter`      |
| `.claude/rules/17-tenant-permission.md`              | `eagle-tenant-starter` / `eagle-row-security-starter` |
| `.claude/rules/18-openapi.md`                        | `eagle-openapi-starter`                               |
| `.claude/rules/26-file-storage.md`                   | `eagle-oss-minio-starter`                             |
| `.claude/rules/27-scheduling.md`                     | `eagle-scheduler-starter`                             |

## 接入新项目快速指南

```
新业务项目接入 eagle-cloud 基建的标准动作：

1) 引 BOM
   build.gradle → implementation platform('com.eagle:eagle-bom:x.y.z')

2) 按场景挑 starter
   参照本文档"推荐组合"

3) 配置 application.yml
   每个 starter 的 USAGE.md → 配置项 章节

4) 抄最小示例验证
   每个 starter 的 USAGE.md → 最小示例 章节

5) 阅读关联规则避免常见错误
   每个 starter 的 USAGE.md → 关联规则 章节
```

## 反馈与迭代

发现 USAGE.md 错漏 / 想补充新示例 → 提 PR 修改对应 `eagle-starter/{name}/USAGE.md`，CI 通过即合并。
