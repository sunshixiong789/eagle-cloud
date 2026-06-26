# eagle-monolith-service

Eagle 平台单体启动模块。它在一个 Spring Boot 进程内复用 `eagle-auth-service` 和
`eagle-system-service` 的业务能力，用本地 Bean 替代服务间 HTTP / MQ 调用，剥离 Nacos、注册中心、Gateway
等微服务专属基础设施。

当前结论：单体模式可以继续启动，但需要按 profile 明确选择数据库。

## 定位

- 零注册中心：单体不依赖 Nacos / Gateway，直接启动 JAR。
- 代码复用：通过 Gradle `implementation project(...)` 复用 auth + system 业务代码。
- 本地启动：`local` profile 使用 H2 内存库 + Caffeine，不需要外部 MySQL、PostgreSQL、Redis。
- 生产启动：`prod,mysql` 使用 MySQL + Redis，`prod,postgresql` 使用 PostgreSQL + Redis。
- 单体集成：`com.eagle.monolith.integration` 提供本地 Port / Client 适配和账号事件桥接。

## Profile 约定

不要在 `application.yml` 中硬编码 `spring.profiles.active`，启动时通过环境变量或命令行指定。

| Profile | 数据库 | 缓存 | 用途 |
| --- | --- | --- | --- |
| `local` | H2 内存库 | Caffeine | 本地开发、快速验证 |
| `prod,mysql` | MySQL | Redis | MySQL 生产或容器部署 |
| `prod,postgresql` | PostgreSQL | Redis | PostgreSQL 生产或容器部署 |

对应配置文件：

- `src/main/resources/application.yml`：通用配置，不绑定具体数据库。
- `src/main/resources/application-local.yml`：H2 + Caffeine，本地零依赖启动。
- `src/main/resources/application-prod.yml`：生产通用加固配置，必须搭配数据库 profile。
- `src/main/resources/application-mysql.yml`：MySQL 数据源与方言。
- `src/main/resources/application-postgresql.yml`：PostgreSQL 数据源与方言。

## 环境变量

已提供环境变量模板：

```bash
cd eagle-services/eagle-monolith-service
cp .env.prod.example .env.prod
```

`.env` 不要提交到 Git。生产环境必须替换密码、JWT keystore 密码、OAuth issuer、数据库和 Redis 参数。

关键变量：

| 变量 | 说明 |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `local`、`prod,mysql` 或 `prod,postgresql` |
| `SERVER_PORT` | 服务端口，默认 `80` |
| `EAGLE_ADMIN_PASSWORD` | 初始管理员密码，生产必须注入强密码 |
| `EAGLE_JWT_KEYSTORE_PASSWORD` | JWT keystore 密码，生产必须注入 |
| `EAGLE_OAUTH_ISSUER` | OAuth2 issuer，生产应使用真实域名 |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | MySQL 或 PostgreSQL 连接信息 |
| `DB_USERNAME` / `DB_PASSWORD` | 数据库账号和密码 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis 连接信息，仅 `prod` 使用 |

## 启动

本仓库当前未提交 Gradle Wrapper，按仓库规范使用本机 `gradle`。

本地 H2 启动：

```bash
gradle :eagle-services:eagle-monolith-service:bootRun --args='--spring.profiles.active=local'
```

MySQL 生产模式：

```bash
SPRING_PROFILES_ACTIVE=prod,mysql \
DB_HOST=127.0.0.1 \
DB_PORT=3306 \
DB_NAME=eagle_monolith \
DB_USERNAME=eagle \
DB_PASSWORD=change-me \
REDIS_HOST=127.0.0.1 \
REDIS_PASSWORD=change-me \
EAGLE_ADMIN_PASSWORD=change-me \
EAGLE_JWT_KEYSTORE_PASSWORD=change-me \
gradle :eagle-services:eagle-monolith-service:bootRun
```

PostgreSQL 生产模式：

```bash
SPRING_PROFILES_ACTIVE=prod,postgresql \
DB_HOST=127.0.0.1 \
DB_PORT=5432 \
DB_NAME=eagle_monolith \
DB_USERNAME=eagle \
DB_PASSWORD=change-me \
REDIS_HOST=127.0.0.1 \
REDIS_PASSWORD=change-me \
EAGLE_ADMIN_PASSWORD=change-me \
EAGLE_JWT_KEYSTORE_PASSWORD=change-me \
gradle :eagle-services:eagle-monolith-service:bootRun
```

## 端点

| 端点 | 默认地址 | 说明 |
| --- | --- | --- |
| Swagger UI | `http://localhost/swagger-ui.html` | API 文档，生产默认关闭 |
| OAuth2 Token | `http://localhost/oauth2/token` | 令牌端点 |
| OAuth2 Authorize | `http://localhost/oauth2/authorize` | 授权码端点 |
| WebSocket STOMP | `ws://localhost/ws-stomp` | 实时推送 |
| Actuator Health | `http://localhost/actuator/health` | 健康检查 |

## 单体集成点

- `MonolithLocalIntegrationConfiguration`：把 auth / system 原本跨服务调用的 Port 和 Client 切到本地 Bean。
- `MonolithAccountEventBridge`：把 auth 账号领域事件转成 system 账号消息并在本地事务提交后处理。
- 单体模式下 auth 与 system 共进程，通过 `MonolithAccountEventBridge` 在本地事务提交后直接处理账号事件，无需 RocketMQ。需要对外发集成事件时再配 `eagle.rocketmq.endpoints` 接入 broker。

## 注意事项

- `prod` 只是生产通用配置，必须和 `mysql` 或 `postgresql` 一起使用。
- `local` 使用 `ddl-auto=update` 仅服务本地 H2 快速启动，生产配置固定为 `ddl-auto=validate`。
- MySQL / PostgreSQL profile 会通过 `spring.sql.init.schema-locations` 初始化 OAuth2 授权服务表。
- 不要重新引入 Nacos / Gateway 依赖，否则单体会退化成微服务节点。
- 不要在 `com.eagle.monolith` 下新增带 `@SpringBootApplication` 的类，避免二次 auto-configuration。
