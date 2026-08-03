# eagle-system-service

Eagle 平台**系统服务**，承载认证授权、OAuth2 授权服务器与用户/角色/权限/部门/菜单管理。基于 DDD 分层 + Spring Modulith
模块化设计，可独立运行（搭配 Nacos / 数据库），亦可被 `eagle-monolith-service` 复用为单体启动。

## 定位

- **OAuth2 授权服务器**（基于 Spring Authorization Server）
- **RBAC 权限管理**：用户、角色、权限、部门、菜单、岗位、字典
- **多种登录方式**：用户名密码 / 微信小程序 / PC 扫码 / H5 / 短信验证码 / 手机号一键登录
- **WebSocket 实时推送**（STOMP 端点）
- 注册到 Nacos 作为微服务节点（默认监听 `:80`）

## 模块划分（Spring Modulith 有界上下文）

包根 `com.eagle.system`，启动类 `EagleSystemApplication`。

| 模块         | 包路径                       | 类型          | 职责                                                  | allowedDependencies                   |
|------------|---------------------------|-------------|-----------------------------------------------------|---------------------------------------|
| **auth**   | `com.eagle.system.auth`   | 业务域         | 登录、OAuth2、微信/短信认证                                   | `common`                              |
| **base**   | `com.eagle.system.base`   | 业务域         | 用户、角色、权限、部门、菜单                                      | `auth::port`, `auth::event`, `common` |
| **config** | `com.eagle.system.config` | 基础设施        | SecurityConfig / CacheConfig / WebSocket / 全局异常处理 等 | `auth::security`, `common`            |
| **common** | `com.eagle.system.common` | 共享内核 (OPEN) | ErrorCode、通用 DTO、异常基础设施                             | 无外部依赖                                 |

模块边界由 `package-info.java` 中的 `@ApplicationModule` / `@NamedInterface` 声明，CI 通过 `ModulithArchitectureTest`
静态校验。

## 依赖（build.gradle）

```
eagle-data-jpa-starter         # JPA + Hibernate 审计
eagle-resource-server-starter  # OAuth2 资源服务器 JWT 验证
eagle-websocket-starter        # WebSocket / STOMP

spring-boot-starter-thymeleaf
spring-boot-starter-oauth2-authorization-server
spring-boot-starter-flyway + flyway-mysql

spring-cloud-starter-alibaba-nacos-discovery   # 服务注册发现
weixin-java-miniapp                            # 微信小程序 SDK
dysmsapi20170525                               # 阿里云短信 SDK
dypnsapi20170525                               # 阿里云号码认证 / 一键登录 SDK
caffeine                                       # 本地缓存
poi-ooxml                                      # Excel 导入导出
fastjson2                                      # 限流过滤器 / Token 跟踪
spring-dotenv                                  # 本地 .env 自动加载
```

## 启动

```bash
# 1) 启动依赖（MySQL / Redis / Nacos / RocketMQ 等，按 application-{profile}.yml 决定）
./eagle-services/docker-compose.yml

# 2) 启动服务（默认 profile = local）
./gradlew :eagle-services:eagle-system-service:bootRun
```

启动成功后控制台打印 Swagger / OAuth2 入口 URL。

| 端点               | 默认地址                              | 说明                 |
|------------------|-----------------------------------|--------------------|
| Swagger UI       | http://localhost/swagger-ui.html  | API 文档 + OAuth2 调试 |
| OpenAPI JSON     | http://localhost/v3/api-docs      | 契约导出               |
| OAuth2 Token     | http://localhost/oauth2/token     | 令牌端点               |
| OAuth2 Authorize | http://localhost/oauth2/authorize | 授权端点               |
| JWK Set          | http://localhost/oauth2/jwks      | 资源服务器拉取公钥          |
| WebSocket（STOMP） | ws://localhost/ws-stomp           | 实时推送握手端点           |
| Actuator Health  | http://localhost/actuator/health  | 健康检查               |

## 关键配置（前缀 `eagle.*`）

| 配置项                              | 默认                                  | 说明                                         |
|----------------------------------|-------------------------------------|--------------------------------------------|
| `eagle.jwt.keystore-location`    | `classpath:jwt-keystore.p12`        | JWT 签名 keystore（生产请挂外部）                    |
| `eagle.jwt.keystore-password`    | `eagle-jwt-dev-2026`                | keystore 密码（生产必须改）                         |
| `eagle.admin.password`           | env `EAGLE_ADMIN_PASSWORD`          | 初始管理员密码（必填）                                |
| `eagle.oauth.default-client.*`   | —                                   | 默认 OAuth2 公开客户端（PKCE）                      |
| `eagle.wechat.mini-program.*`    | env `WECHAT_MINI_APP_*`             | 微信小程序 AppID / Secret                       |
| `eagle.wechat.web.pc/h5.*`       | env `WECHAT_WEB_*` / `WECHAT_MP_*`  | 微信网页/H5 登录                                 |
| `eagle.message.sms.*`            | env `SMS_*` / `HNSLS_SMS_*`         | 短信服务商配置（由 eagle-notification-starter 统一处理） |
| `eagle.auth.one-click.*`         | provider 默认 `mock`                  | 一键登录提供方 / 阿里云 dypnsapi 配置                  |
| `eagle.log.cleanup.cron`         | `0 0 2 * * ?`                       | 审计日志每日清理                                   |
| `eagle.websocket.endpoint`       | `/ws-stomp`                         | STOMP 握手路径                                 |
| `spring.cloud.nacos.discovery.*` | env `NACOS_SERVER_ADDR / NAMESPACE` | Nacos 注册中心地址 / 命名空间                        |

完整字段见 `src/main/resources/application.yml` 与 `application-{profile}.yml`。

## 登录认证流程

所有登录方式最终都通过 OAuth2 授权服务器签发 access_token + refresh_token。下表汇总现有 grant_type：

| grant_type            | 用途           | 关键参数                                       | Provider                                  |
|-----------------------|--------------|--------------------------------------------|-------------------------------------------|
| `authorization_code`  | 标准授权码 + PKCE | `code` / `code_verifier`                   | Spring Authorization Server 内置            |
| `refresh_token`       | 刷新令牌         | `refresh_token`                            | Spring Authorization Server 内置            |
| `wechat_mini_program` | 微信小程序登录      | `js_code`                                  | `WechatMiniProgramAuthenticationProvider` |
| `sms_code`            | 短信验证码登录      | `phone` / `code`                           | `SmsCodeAuthenticationProvider`           |
| `phone_one_click`     | 手机号一键登录      | `access_token`（运营商 / 阿里云 dypnsapi 颁发的短期凭证） | `PhoneOneClickAuthenticationProvider`     |

### 手机号一键登录

由运营商在网络层识别 SIM 卡持有人后下发短期 token，服务端凭 token 反查真实手机号，无需用户输入手机号或验证码。

**请求示例**

```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=phone_one_click
&client_id=eagleWeb
&access_token=运营商SDK返回的token
```

**端到端时序**

```
端 SDK            授权服务器                                   阿里云 dypnsapi          DB
  │                 │                                            │                  │
  │── access_token ─▶ Converter (PhoneOneClickAuthenticationConverter)              │
  │                 │  解析 grant_type / access_token                                │
  │                 ▼                                                                │
  │                Provider (PhoneOneClickAuthenticationProvider)                    │
  │                 │  ① 校验客户端是否声明 phone_one_click                              │
  │                 │  ② PhoneOneClickService.verifyAndGetPhone(token) ──▶ getMobile │
  │                 │                                            │                  │
  │                 │  ◀── 真实手机号 ─────────────────────────────│                  │
  │                 │  ③ AccountApplicationService.findOrCreateByPhone(phone) ─▶  │
  │                 │  ④ 构建 EagleUser + 生成 OAuth2 Token + 保存授权                 │
  │ ◀── access_token + refresh_token ──┤                                            │
```

**关键组件**

- `auth/domain/service/PhoneOneClickService` — 领域接口（token → phone）
- `auth/infrastructure/external/PhoneOneClickServiceImpl` — `mock` / `aliyun` 双分支适配
- `auth/infrastructure/security/PhoneOneClickAuthenticationConverter` — 解析 token endpoint 参数
- `auth/infrastructure/security/PhoneOneClickAuthenticationProvider` — 校验 + 找/建账号 + 签发 Token

**配置示例**

```yaml
eagle:
  auth:
    one-click:
      enabled: true
      provider: aliyun                # mock（默认） | aliyun
      endpoint: dypnsapi.aliyuncs.com
      access-key-id: ENC(...)         # 必须 Jasypt 加密
      access-key-secret: ENC(...)
```

- `provider=mock`（默认）：access_token 直接当作手机号使用，仅用于开发联调；非 11 位号码格式的 token 会被拒绝。
- `provider=aliyun`：调用 `dypnsapi.GetMobile`，期望响应 `code=OK` 且 `getMobileResultDTO.mobile` 为 11 位手机号。

**默认客户端的 grant_types**

`OAuthClientProperties.authorizationGrantTypes` 默认已包含 `phone_one_click`；通过 `OAuthClientInitializer` 在启动时同步到
DB，旧环境重启即可生效。

**错误码（11034–11037）**

| 错误码   | 含义                             |
|-------|--------------------------------|
| 11034 | 一键登录 access_token 不能为空         |
| 11035 | 一键登录校验失败（运营商接口异常 / 业务码非 OK）    |
| 11036 | 一键登录服务未启用（开关关闭或未配置 AccessKey）  |
| 11037 | 一键登录获取手机号失败（响应缺失 mobile 或格式异常） |

i18n 消息见 `messages_zh_CN.properties` / `messages_en.properties` 中的 `error.auth.one_click_*`。

## Profile

| Profile | 默认数据源        | 用途         |
|---------|--------------|------------|
| `local` | H2（内存）       | 本机快速启动，零依赖 |
| `dev`   | MySQL + 真实依赖 | 开发联调环境     |

切换：`SPRING_PROFILES_ACTIVE=dev` 或 `--spring.profiles.active=dev`。

## 数据库迁移

生产路径 `src/main/resources/db/migration/`，遵循 Flyway 命名 `V{yyyyMMddHHmm}__{snake}.sql`（详见
`.claude/rules/04-data.md`）。本地开发为加快迭代默认使用 H2 + `ddl-auto: update`，**生产严禁**。

## 测试

```bash
# 全部测试（含 ModulithArchitectureTest）
./gradlew :eagle-services:eagle-system-service:test

# 仅架构验证
./gradlew :eagle-services:eagle-system-service:test --tests "*.ModulithArchitectureTest"
```

## 容器化

`Dockerfile` 已就绪，构建：

```bash
./gradlew :eagle-services:eagle-system-service:bootJar
docker build -t eagle/system-service:1.0.0 eagle-services/eagle-system-service
```

需注入的环境变量：`EAGLE_ADMIN_PASSWORD` / `EAGLE_JWT_KEYSTORE_PASSWORD` / `NACOS_SERVER_ADDR` /
`SPRING_DATASOURCE_*` / `SPRING_REDIS_*` 等。

## 与其他服务的关系

```
浏览器 ─→ eagle-gateway-service ─JWT─→ eagle-system-service
                                        │
                                        ├─ Nacos 注册发现
                                        ├─ MySQL（业务库）
                                        ├─ Redis（缓存 / 限流 / Token 黑名单）
                                        └─ RocketMQ（领域事件外发，可选）
```

`eagle-monolith-service` 通过 Gradle 依赖直接复用本服务的全部业务代码，剥离 Nacos 后作为单体启动。
