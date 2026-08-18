# eagle-auth-service

Eagle 平台**认证服务**：OAuth2 授权服务器、账号聚合根、第三方登录、短信验证码、Token 黑名单与在线态。

已从 `eagle-system-service` 拆出，Consul 注册名 **`auth`**，默认监听 `:9090`。

## 定位

- **OAuth2 Authorization Server**（Spring Authorization Server）
- **账号生命周期**：注册、改密、换绑手机、冻结、注销
- **登录方式**：授权码 + PKCE、刷新令牌、微信小程序 / App / PC / H5、短信验证码、淘宝 App、Apple、社交账号绑定
- **内部 API**：在线用户、账号快照、黑名单，供 system 管理端调用
- **集成事件**：账号注册 / 删除 / 换绑手机，经 RabbitMQ 发给 system 等消费方

## 模块划分

包根 `com.eagle.auth`，启动类 `EagleAuthApplication`。唯一业务模块：

| 模块 | 包路径 | allowedDependencies |
|---|---|---|
| **core** | `com.eagle.auth.core` | `{}`（完全隔离） |

分层：`interfaces → application → domain ← infrastructure`，配置契约放在与四层平级的 `core/config`。

## 依赖

```
eagle-data-jpa-starter
eagle-resource-server-starter
eagle-resilience-starter
eagle-websocket-starter
eagle-amqp-starter
eagle-openapi-starter
eagle-audit-log-starter
eagle-encrypt-starter
eagle-restclient-starter          # 调 system /internal/authorization/{id}

spring-boot-starter-webmvc
spring-boot-starter-thymeleaf
spring-boot-starter-oauth2-authorization-server
spring-cloud-starter-consul-discovery
spring-cloud-starter-consul-config
```

微信 / 淘宝 SDK 已去掉，改为 RestClient 直调官方接口。一键登录的阿里云 / 腾讯云 SDK 也已移除。

## 启动

```bash
export CONSUL_TOKEN=<service-token>
gradle :eagle-services:eagle-auth-service:bootRun --args='--spring.profiles.active=local'
```

未提供 token 时 KV 静默跳过，启动会在缺 `EAGLE_ADMIN_PASSWORD` 时失败。

| 端点 | 默认地址 | 说明 |
|---|---|---|
| Swagger UI | http://localhost:9090/swagger-ui.html | 经网关聚合为 `/v3/api-docs/auth` |
| Token | http://localhost:9090/oauth2/token | 网关按原路径透传 |
| Authorize | http://localhost:9090/oauth2/authorize | 授权码 |
| JWK Set | http://localhost:9090/oauth2/jwks | 资源服务器拉公钥 |
| OIDC Discovery | `/.well-known/openid-configuration` | issuer 由 `X-Forwarded-*` 派生为网关 URL |
| 登录页 | http://localhost:9090/login | Thymeleaf |
| 短信 / 账号自助 | `/sms/**` `/accounts/**` | 网关透传 |
| 内部 API | `/internal/online-users/**` `/internal/account-blacklist/**` | 仅集群内 |

OAuth2 表（`oauth2_authorization` / `oauth2_authorization_consent`）不是 JPA 实体，启动时由 `spring.sql.init` 执行 `db/oauth2-schema-postgresql.sql`（`IF NOT EXISTS`，幂等）。业务表仍由 Hibernate 同步。

## 默认客户端

| 客户端 | Client ID | 授权类型 | 认证方式 |
|---|---|---|---|
| Web | `eagleWeb` | `authorization_code`、`refresh_token`、`wechat_mini_program`、`sms_code` | 无（公开客户端，必须 PKCE） |
| App | `eagleApp` | `refresh_token`、`sms_code`、`wechat_app`、`wechat_mini_program`、`taobao_app`、`apple_app`、`social_bind` | 无 |
| 运营 | `shengxinOps` | `client_credentials` | `client_secret_basic`，默认关闭 |

| 项 | 值 |
|---|---|
| Access Token | 1 小时 |
| Refresh Token | 30 天 |
| Scopes | `openid`、`profile`、`email`、`address`、`phone` |

`phone_one_click` 已从默认授权类型中移除。原因：唯一实现 `MockPhoneOneClickProvider` 把 access_token 当手机号返回，公开客户端下等于知道手机号即可登录。三道锁：

1. `eagle.auth.one-click.enabled` 默认 `false`
2. Mock provider 仅 `@Profile("dev")`
3. grant 已从客户端默认名单去掉（`syncMode=OVERWRITE`，重启即改库）

接入真实运营商后再逐一打开，并补集成验证。

## 登录 grant_type

| grant_type | 用途 | 关键参数 |
|---|---|---|
| `authorization_code` | 标准授权码 + PKCE | `code` / `code_verifier` |
| `refresh_token` | 刷新令牌 | `refresh_token` |
| `sms_code` | 短信验证码 | `phone` / `code` |
| `wechat_mini_program` | 微信小程序 | `js_code` |
| `wechat_app` | 微信 App | 微信授权码 |
| `taobao_app` | 淘宝 App | 淘宝授权码 |
| `apple_app` | Sign in with Apple | Apple authorization code |
| `social_bind` | 已登录用户绑定社交账号 | — |

短信验证码示例：

```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=sms_code&phone=13800001234&code=123456&client_id=eagleWeb
```

## 与 system-service 的集成

```
本服务发布 AMQP 集成事件（消费方各自声明 Message 类）：
  account.registered / account.deleted / account.phone_changed
  契约：docs/contracts/*.json

本服务调用：
  GET lb://system/internal/authorization/{accountId}   → JWT 姓名 + 角色码

本服务暴露：
  /internal/online-users/**
  /internal/account-blacklist/**
  /internal/accounts/**
```

Token 撤销与在线态走 Redis：`token:blacklist:{jti}`、`account:online:{accountId}`。

## 关键配置

| 配置项 | 说明 |
|---|---|
| `eagle.oauth.issuer` | Token `iss`。`local` 默认 `http://localhost:9090`，经网关时应是网关对外 URL |
| `eagle.jwt.keystore-location` | 默认 `classpath:jwt-keystore.p12`，生产挂外部文件 |
| `eagle.jwt.keystore-password` | 环境 / KV：`EAGLE_JWT_KEYSTORE_PASSWORD`，无默认值 |
| `eagle.admin.password` | `EAGLE_ADMIN_PASSWORD`，无默认值，缺则 fail-fast |
| `eagle.remember-me.key` | `EAGLE_REMEMBER_ME_KEY`，集群必须一致 |
| `eagle.amqp.consumer-group` | 默认 `auth_consumer`；`local` 加 `_local` 后缀 |
| `spring.cloud.consul.*` | KV：`config/auth,<profile>/data` |

第三方登录与短信账号全部进 Consul KV，不要写进 compose。

## Profile

| Profile | 行为 |
|---|---|
| `local` | KV 开、discovery 关；连开发机中间件（可用 `LOCAL_*` 改 localhost）；本机 issuer |
| `dev` | 注册到 Consul，连容器网络 |
| `prod` | `ddl-auto=validate`，Swagger 关闭 |

## 测试

```bash
gradle :eagle-services:eagle-auth-service:test
gradle :eagle-services:eagle-auth-service:test --tests "*ModulithArchitectureTest"
gradle :eagle-services:eagle-auth-service:test --tests "*LayeredArchitectureTest"
gradle :eagle-services:eagle-auth-service:test --tests "*IntegrationEventContractTest"
```

## 容器化

```bash
gradle :eagle-services:eagle-auth-service:bootJar
docker build -t eagle/auth-service:1.9.0 eagle-services/eagle-auth-service
```

compose 库名固定 `DB_NAME=eagle_auth`。引导参数与 ACL 见 [`../docs/consul-config.md`](../docs/consul-config.md)。
