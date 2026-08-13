---
name: eagle-cloud
description: Eagle Cloud 底座项目的统一开发知识入口。用于开发、修改、排查或评审 Eagle Cloud 项目中的领域模型、异常、事件、JPA/R2DBC、Redis、RabbitMQ、鉴权、HTTP 客户端、幂等、审计、文件存储、定时任务、分库分表、链路追踪、WebSocket/SSE、OpenAPI、加密和韧性治理；按当前任务从 references 中选择性加载对应能力文档。
---

# Eagle Cloud 开发知识

这是原 `agent-plugin/skills` 的单一入口。先判断任务涉及哪些底座能力，只读取相应 reference；不要预加载全部文档。

## 使用流程

1. 读取项目根目录和改动位置附近的 `AGENTS.md` 或 `CLAUDE.md`。
2. 检查目标模块的构建文件、包结构、配置、现有实现和测试，确认项目当前实际使用的 starter 与 API。
3. 根据下表读取最少数量的 reference。一个任务涉及多个能力时可以组合读取。
4. reference 与当前代码不一致时，以代码、测试和项目局部规则为准，并在结果中指出差异；不要套用不存在的旧 API。
5. 实现后运行与改动范围相称的测试或构建检查。

## 按需路由

| 任务或关键词 | 必读文档 |
|---|---|
| DDD 基类、聚合根、实体、异常、错误码、领域事件、通用 DTO、异步线程池、国际化、业务指标 | `references/common.md` |
| RabbitMQ、AMQP、跨服务领域事件、消费者、重试、DLQ | `references/amqp.md`；事件模型同时读 `references/common.md` |
| 操作审计、`@AuditLog`、审计查询 | `references/audit-log.md` |
| JPA、Hibernate、Repository、审计字段、批处理、慢 SQL | `references/data-jpa.md` |
| WebFlux 数据访问、R2DBC、Reactive Repository、响应式审计 | `references/data-r2dbc.md` |
| JPA 敏感字段加密、AES、`EncryptedStringConverter` | `references/encrypt.md`；同时读 `references/data-jpa.md` |
| 分布式 ID、Snowflake、UUID v7、TSID、NanoId、业务单号 | `references/id-generator.md` |
| 接口幂等、重复提交、Token、业务键、结果缓存 | `references/idempotency.md`；使用 Redis 时再读 `references/redis.md` |
| Swagger、SpringDoc、OpenAPI 注解、接口分组、JWT 文档 | `references/openapi.md` |
| MinIO、对象存储、上传下载、文件校验、对象键设计 | `references/oss-minio.md` |
| Redis 缓存、分布式锁、限流、布隆过滤器、计数器、延迟队列、Pub/Sub | `references/redis.md` |
| 熔断、重试、超时、Resilience4J、fallback | `references/resilience.md` |
| OAuth2 Resource Server、JWT、权限、`@PreAuthorize`、当前用户 | `references/resource-server.md` |
| Servlet 同步服务调用、RestClient、`@HttpExchange`、请求头透传 | `references/restclient.md` |
| WebFlux 响应式服务调用、WebClient、响应式 `@HttpExchange` | `references/webclient.md` |
| XXL-JOB、分布式定时任务、分片任务 | `references/scheduler.md` |
| ShardingSphere、分库分表、分片键、读写分离 | `references/sharding.md` |
| Trace、Span、Brave、B3、Zipkin、MDC | `references/tracing.md` |
| WebSocket、STOMP、SSE、实时推送、离线消息 | `references/websocket.md`；集群消息涉及 Redis 时再读 `references/redis.md` |
| 用户明确说“按 eagle flow 走”“启动 eagle flow”或“走 eagle flow” | `references/feature-flow.md` |

## 组合规则

- Servlet 调用下游服务用 `restclient.md`；WebFlux 非阻塞链路用 `webclient.md`，不要同时套用两套客户端模式。
- JPA 与 R2DBC 按模块技术栈二选一；只有迁移或跨模块任务才同时读取两份。
- 涉及鉴权的接口先读 `resource-server.md`；还要生成 Swagger 鉴权说明时再读 `openapi.md`。
- 涉及跨服务事件时读 `amqp.md`；同进程领域事件只需结合 `common.md` 和现有 Spring 事件实现。
- `feature-flow.md` 只允许用户显式触发，普通新增功能、修复或重构不自动进入该流程。

## 内容边界

- references 保留旧插件中各 starter 的完整用法、配置、示例、限制和踩坑记录。
- 旧文档提到的 `.claude/rules/*`、slash command 或已移除组件仅作为历史关联线索，不代表目标项目中一定存在；使用前必须在当前项目验证。
