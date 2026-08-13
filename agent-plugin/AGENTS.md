# Eagle Cloud 开发规范（Codex Plugin 注入）

本文件是 Codex 使用 Eagle Cloud 插件时的轻量入口。业务项目自己的 `AGENTS.md` 仍优先；本文件只负责说明插件能力、
规则路由和高频 Eagle 专有坑点。

## 插件能力

- `../.agents/rules/`：按场景读取，不要一次性展开全部规则。
- `skills/`：涉及具体 starter 时读取对应 `SKILL.md`，例如 Redis、AMQP、JPA、Tenant、Resource Server、OpenAPI、MinIO。
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
| 命名 / Java 风格 / Lombok / 测试 / 依赖 | `../.agents/rules/00-core.md` |
| Java 25 语言基线（record / sealed / 模式匹配 / 虚拟线程） | `../.agents/rules/01-java25.md` |
| DDD 分层 / Modulith / 领域事件 / 集成事件 | `../.agents/rules/02-architecture.md` |
| REST / OpenAPI / 异常 / 错误码 / i18n | `../.agents/rules/03-api-error.md` |
| JPA / 索引 / 事务 / 并发 / Flyway | `../.agents/rules/04-data.md` |
| 安全 / 租户 / 数据权限 / 日志 | `../.agents/rules/05-security.md` |
| Spring Boot 4 / Jackson 3 / starter / HTTP 客户端 | `../.agents/rules/06-boot4.md` |
| 高频陷阱 / 存量违例台账 / PR 自检 | `../.agents/rules/07-checklist.md` |
| 内聚与耦合 / 规模红线 / 抽象与复用决策 / 各层厚度 | `../.agents/rules/08-quality.md` |
| 缓存 / 消息 / 调度 / 存储 / 韧性 | 对应 starter skill（`eagle-redis` / `eagle-amqp` / `eagle-scheduler` / `eagle-oss-minio` / `eagle-resilience`） |

只保留和当前任务相关的规则上下文。通用编程常识由模型默认能力和现有代码风格处理；Eagle 专有 API、starter 用法、
模块边界、配置键、迁移策略和踩坑记录必须按规则执行。

## Starter Skills

编码触及 starter 时读取对应 skill，而不是凭记忆写 API：

- 基础：`eagle-common`、`eagle-id-generator`、`eagle-resilience`、`eagle-audit-log`
- 数据：`eagle-data-jpa`、`eagle-data-r2dbc`、`eagle-sharding`
- 基础设施：`eagle-redis`、`eagle-amqp`、`eagle-oss-minio`、`eagle-scheduler`
- 安全与治理：`eagle-resource-server`、`eagle-openapi`、`eagle-tracing`、`eagle-idempotency`
- 业务能力：`eagle-websocket`、`eagle-encrypt`、`eagle-restclient`、`eagle-webclient`

**已移除的 9 个 starter**（skill 已删，不要引用）：`tenant`、`rocketmq`（→ `amqp`）、
`dynamic-datasource`、`elasticsearch`、`excel`、`notification`、`seata`、`sentinel`（→ `resilience`）、`ai`。

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
3. **多租户能力已整体移除**：`TenantContextHolder` / `@TenantFilter` / `eagle.tenant.*` 全部不存在，
   `ContextPropagationConfig` 也不再传播租户。新表不要加 `tenant_id`。
4. AMQP 消费者继承 `AbstractAmqpListener<T>`，实现 `getTopic() / getEventClass() / handle(T event)`；不要用
   `@RabbitListener`。子类构造器必须显式调用 `super(amqpProperties)`，不要用 `@RequiredArgsConstructor` 代替。
   每个消费者要有自己的 `getConsumerGroup()`，否则多消费者竞争消费同一条消息。
   通配 routing key 用 `#` 不是 `*`（AMQP 的 `*` 只匹配恰好一个单词）。
5. `DistributedLock.tryLock(key, waitSec, leaseSec, Supplier)` 的时间参数是 `long` 秒，不是 `Duration`。
6. `CacheProtectionUtil.getWithMutex(key, ttl, loader, type)` 需要 4 个参数，包含返回类型 `Class<T>`。
7. `DataScope` 枚举：`ALL / SELF / DEPT / DEPT_AND_CHILD / CUSTOM`（无注解式数据权限，需手写过滤）。
8. **不存在** `eagle.xxx.enabled` 总开关 —— starter 引入即生效，依赖坐标本身就是开关；
   条件注解只用于「选哪种实现」，不用于「要不要装」。要禁用就移除依赖或用 `spring.autoconfigure.exclude`。
9. 生产环境必改的默认值：`eagle.storage.type=local`、`eagle.tracing.sampling-probability=1.0`（全采样）。
10. Jackson 3 分包：核心类在 `tools.jackson.*`，注解仍在 `com.fasterxml.jackson.annotation.*`；
    `JsonProcessingException`（checked）已换成 `JacksonException`（unchecked）。
11. 自定义 `SecurityFilterChain` 取代 starter 默认 chain 时，`oauth2ResourceServer.jwt` 必须显式接
    `EagleJwtAuthenticationConverter`，否则 principal 不是 `EagleUser`，所有 `hasRole(...)` 静默失效。
12. starter 里的 util 类只标 `@Component` 业务服务扫不到，必须在 `@AutoConfiguration` 里显式 `@Bean` 注册。
13. **9 个 starter 已移除**（目录空壳仍在，但不在 `settings.gradle`）：`tenant` / `rocketmq`（→ `amqp`）/
    `dynamic-datasource` / `elasticsearch` / `excel` / `notification` / `seata` / `sentinel` / `ai`。
    对应地：无 `@GlobalTransactional`、无 `@ReadOnly`、限流走 `eagle-resilience` 的 `@RateLimit` 或
    `RedisRateLimiter`、短信由 auth-service 自己实现。
14. 注册中心是 **Consul**（`spring-cloud-starter-consul-discovery`），不是 Nacos。
