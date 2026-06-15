# Eagle Cloud 开发规范（Codex Plugin 注入）

本文件是 Codex 使用 Eagle Cloud 插件时的轻量入口。业务项目自己的 `AGENTS.md` 仍优先；本文件只负责说明插件能力、
规则路由和高频 Eagle 专有坑点。

## 插件能力

- `rules/`：按场景读取，不要一次性展开全部规则。
- `skills/`：涉及具体 starter 时读取对应 `SKILL.md`，例如 Redis、RocketMQ、JPA、Tenant、Resource Server、OpenAPI、MinIO。
- `commands/`：可参考 `/check-arch`、`/new-module`、`/new-aggregate`、`/new-starter`、`/add-error-code` 的脚手架规范。

## 技术栈定位

- Java 25 / Gradle 8.x（Groovy DSL）
- Spring Boot 4.0.6 / Spring Cloud 2025.1.1 / Spring Cloud Alibaba 2025.1.0.0
- Spring Modulith 2.0.5
- DDD + 六边形架构，领域层稳定，`infrastructure` 可替换为微服务适配器
- Hibernate 7.2.6 / MySQL / PostgreSQL / Druid
- Spring Security + OAuth2 Resource Server

## 规则路由

| 场景 | 规则 |
| --- | --- |
| 命名 / Java 风格 | `rules/01-naming.md`、`rules/02-code-style.md` |
| DDD / Modulith / 跨模块 | `rules/03-architecture.md`、`rules/04-modulith.md` |
| REST / OpenAPI | `rules/05-api.md`、`rules/18-openapi.md` |
| JPA / Flyway | `rules/06-database.md`、`rules/28-migration.md` |
| 异常 / i18n | `rules/07-exception.md`、`rules/20-i18n.md` |
| 事务 / 并发 / 事件 | `rules/08-concurrency.md`、`rules/29-event-driven.md` |
| 测试 | `rules/09-testing.md` |
| Starter / 依赖 | `rules/10-starter.md`、`rules/30-dependency.md` |
| HTTP 客户端 | `rules/11-feign.md` |
| 安全 / 配置 | `rules/12-security.md`、`rules/19-config.md` |
| 日志 / 性能 / 部署 | `rules/13-logging.md`、`rules/23-performance.md`、`rules/24-deployment.md` |
| 缓存 / 消息 / 分布式事务 | `rules/14-cache.md`、`rules/15-messaging.md`、`rules/16-transaction-distributed.md` |
| 租户 / 数据权限 | `rules/17-tenant-permission.md` |
| 文件存储 / 调度 / 韧性 | `rules/26-file-storage.md`、`rules/27-scheduling.md`、`rules/21-resilience.md` |
| PR 自检 | `rules/25-review-checklist.md` |

只保留和当前任务相关的规则上下文。通用编程常识由模型默认能力和现有代码风格处理；Eagle 专有 API、starter 用法、
模块边界、配置键、迁移策略和踩坑记录必须按规则执行。

## Starter Skills

编码触及 starter 时读取对应 skill，而不是凭记忆写 API：

- 基础：`eagle-common`、`eagle-id-generator`、`eagle-resilience`、`eagle-audit-log`
- 数据：`eagle-data-jpa`、`eagle-mybatis`、`eagle-dynamic-datasource`、`eagle-sharding`、`eagle-elasticsearch`
- 基础设施：`eagle-redis`、`eagle-rocketmq`、`eagle-oss-minio`、`eagle-scheduler`、`eagle-seata`、`eagle-sentinel`
- 安全与治理：`eagle-tenant`、`eagle-row-security`、`eagle-resource-server`、`eagle-openapi`、`eagle-tracing`
- 业务能力：`eagle-notification`、`eagle-payment`、`eagle-websocket`、`eagle-excel`、`eagle-encrypt`、`eagle-ai`

## 验证

```bash
gradle build
gradle :path:to:module:test
gradle :path:to:module:test --tests "*.ModulithArchitectureTest"
```

业务项目若没有 Gradle Wrapper，使用本机 `gradle`。涉及 Modulith 边界或公共契约时优先参考 `/check-arch`。

## 高频专有坑点

1. 审计字段名：`createBy / updateBy / createTime / updateTime`，不是 `createdBy / updatedBy / createdAt / updatedAt`。
2. `AsyncConfig` Bean 名：`taskExecutor`，不是 `eagleTaskExecutor`。
3. `TenantContextHolder` API：`getTenantId() / setTenantId() / clear()`，不是 `getCurrentTenantId()`。
4. RocketMQ 消费者继承 `AbstractRocketMqListener<T>`，实现 `getTopic() / getEventClass() / handle(T event)`；不要用
   `@RocketMQMessageListener`。子类构造器必须显式调用 `super(rocketMqProperties)`，不要用 `@RequiredArgsConstructor` 代替。
5. `DistributedLock.tryLock(key, waitSec, leaseSec, Supplier)` 的时间参数是 `long` 秒，不是 `Duration`。
6. `CacheProtectionUtil.getWithMutex(key, ttl, loader, type)` 需要 4 个参数，包含返回类型 `Class<T>`。
7. `DataScope` 枚举：`ALL / SELF / DEPT / DEPT_AND_CHILD / CUSTOM`。
8. `eagle.tenant.enabled`、`eagle.datasource.enabled` 默认 `false`；生产环境不要沿用 `eagle.storage.type=local` 或
   `eagle.tracing.sampling-probability=1.0`。
