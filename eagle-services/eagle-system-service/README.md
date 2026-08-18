# eagle-system-service

Eagle 平台**系统服务**：用户 / 角色 / 字典 / 日志 / 监控、文件元数据、站内消息与公告。OAuth2 授权服务器已拆到独立的 `eagle-auth-service`，本服务是纯资源服务器。

Consul 注册名 **`system`**。compose 默认监听 `:8082`；基线 yml 未注入 `SERVER_PORT` 时是 `:80`。

## 定位

- **RBAC**：用户、角色、字典、操作日志、控制台统计、服务监控
- **文件**：业务文件元数据 + `eagle-oss-minio-starter`（local / minio）
- **站内消息 / 公告**：消费通用 topic 落库，在线用户走 WebSocket 推送
- **跨服务**：消费 auth 的账号集成事件；通过 RestClient 调 auth 内部 API；反向暴露授权查询给 auth 拼 JWT claims

## 模块划分（Spring Modulith）

包根 `com.eagle.system`，启动类 `EagleSystemApplication`。

| 模块 | 包路径 | 职责 | allowedDependencies |
|---|---|---|---|
| **base** | `com.eagle.system.base` | 用户、角色、字典、日志、监控、控制台 | 未声明 |
| **file** | `com.eagle.system.file` | 文件元数据与对象存储 | 未声明 |
| **message** | `com.eagle.system.message` | 站内信 + 公告 | `{}`（完全隔离） |

模块边界由 `package-info.java` 的 `@ApplicationModule` 声明，CI 通过 `ModulithArchitectureTest` + `LayeredArchitectureTest` 静态校验。

`message` 不依赖任何业务模块，也不暴露 Named Interface。其他服务发站内信只需往 `user_message_send` 发集成事件。

## 依赖

```
eagle-data-jpa-starter
eagle-resource-server-starter
eagle-websocket-starter
eagle-amqp-starter
eagle-resilience-starter
eagle-oss-minio-starter
eagle-openapi-starter
eagle-audit-log-starter
eagle-restclient-starter          # lb://auth 内部 API

spring-boot-starter-webmvc
springdoc-openapi-starter-webmvc-ui
spring-cloud-starter-consul-discovery
spring-cloud-starter-consul-config
```

PostgreSQL 驱动由 `eagle-data-jpa-starter` 传递引入。Schema 暂由 Hibernate `ddl-auto` 同步，尚未引入 Flyway。

## 启动

```bash
# 中间件：PostgreSQL / Redis / Consul / RabbitMQ
# compose 见 ../docker-compose.yml

export CONSUL_TOKEN=<service-token>
gradle :eagle-services:eagle-system-service:bootRun --args='--spring.profiles.active=local'
```

| 端点 | 默认地址（compose） | 说明 |
|---|---|---|
| Swagger UI | http://localhost:8082/swagger-ui.html | 经网关则为 `/v3/api-docs/system` |
| OpenAPI JSON | http://localhost:8082/v3/api-docs | 契约导出 |
| WebSocket（STOMP） | `ws://localhost:8082/ws-stomp` | 网关对外是 `/system/ws-stomp` |
| Actuator | http://localhost:8082/actuator/health | 健康检查 |
| 内部授权查询 | `/internal/authorization/{accountId}` | 仅集群内，网关会 403 |

## 与 auth-service 的集成

```
auth-service                         system-service
     │                                      │
     │  AMQP  account.registered / deleted  │
     │ ───────────────────────────────────▶ │  创建 / 删除 User
     │                                      │
     │  HTTP  GET /internal/authorization/{id}
     │ ◀─────────────────────────────────── │  JWT claims（姓名 + 角色码）
     │                                      │
     │  HTTP  /internal/online-users/**     │
     │  HTTP  /internal/account-blacklist/**
     │ ───────────────────────────────────▶ │  管理端在线用户 / 黑名单
```

消费方在本模块独立声明 `AccountRegisteredMessage` 等，不 import auth 的 `*IntegrationEvent`。契约文件在仓库 `docs/contracts/`。

## 关键配置

| 配置项 | 说明 |
|---|---|
| `spring.datasource.*` | PostgreSQL，库名固定 `eagle_system` |
| `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` | 直连 auth 的 JWKS，避免 issuer discovery 启动风暴 |
| `eagle.resource-server.permit-paths` | 额外公开路径；默认已放行 health / swagger |
| `eagle.websocket.endpoint` | 默认 `/ws-stomp` |
| `eagle.amqp.consumer-group` | 默认 `system_consumer`；`local` 会加 `_local` 后缀，避免和 dev 抢队列 |
| `eagle.storage.type` | 默认 `local`；生产应改为 minio |
| `spring.cloud.consul.*` | 注册名 `system`；KV：`config/system,<profile>/data` |

完整字段见 `src/main/resources/application.yml` 与 `application-{profile}.yml`。凭据进 Consul KV，不要写回 compose / yml。

## Profile

| Profile | 行为 |
|---|---|
| `local` | Consul KV 开、discovery 关；地址覆盖为开发机公网（可用 `LOCAL_*` 改回 localhost）；`ddl-auto=update` |
| `dev` | 注册到 Consul，连容器网络内的 postgres / redis / rabbitmq |
| `prod` | `ddl-auto=validate`，Swagger 关闭 |

`local` 连的是开发环境真库时，实体改动会直接改 dev 表。保护表结构请设 `LOCAL_DDL_AUTO=validate`。

## 测试

```bash
gradle :eagle-services:eagle-system-service:test
gradle :eagle-services:eagle-system-service:test --tests "*ModulithArchitectureTest"
gradle :eagle-services:eagle-system-service:test --tests "*LayeredArchitectureTest"
```

## 容器化

```bash
gradle :eagle-services:eagle-system-service:bootJar
docker build -t eagle/system-service:1.9.0 eagle-services/eagle-system-service
```

compose 只注入引导参数（`CONSUL_*` / `SERVER_PORT` / `DB_NAME=eagle_system` / `JAVA_OPTS`）。其余见 [`../docs/consul-config.md`](../docs/consul-config.md)。

## 与其他服务的关系

```
浏览器 ─→ eagle-gateway-service ─→ eagle-system-service
                │                         │
                │                         ├─ Consul 注册发现
                │                         ├─ PostgreSQL（eagle_system）
                │                         ├─ Redis（缓存 / 会话辅助）
                │                         ├─ RabbitMQ（消费 auth 事件 / 站内信）
                │                         └─ RestClient → lb://auth
                └─ 不转发 /internal/**
```
