# eagle-system-service

Eagle 平台**系统服务**，承载认证授权、OAuth2 授权服务器与用户/角色/权限/部门/菜单管理。基于 DDD 分层 + Spring Modulith
模块化设计，可独立运行（搭配 Nacos / 数据库），亦可被 `eagle-monolith-service` 复用为单体启动。

## 定位

- **OAuth2 授权服务器**（基于 Spring Authorization Server）
- **RBAC 权限管理**：用户、角色、权限、部门、菜单、岗位、字典
- **多种登录方式**：用户名密码 / 微信小程序 / PC 扫码 / H5 / 短信验证码
- **行级数据权限**（基于 `eagle-row-security-starter` 切面注入）
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
eagle-row-security-starter     # 行级数据权限
eagle-websocket-starter        # WebSocket / STOMP

spring-boot-starter-thymeleaf
spring-boot-starter-oauth2-authorization-server
spring-boot-starter-flyway + flyway-mysql

spring-cloud-starter-alibaba-nacos-discovery   # 服务注册发现
weixin-java-miniapp                            # 微信小程序 SDK
dysmsapi20170525                               # 阿里云短信 SDK
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

| 端点                | 默认地址                              | 说明                |
|-------------------|-----------------------------------|-------------------|
| Swagger UI        | http://localhost/swagger-ui.html  | API 文档 + OAuth2 调试 |
| OpenAPI JSON      | http://localhost/v3/api-docs      | 契约导出              |
| OAuth2 Token      | http://localhost/oauth2/token     | 令牌端点              |
| OAuth2 Authorize  | http://localhost/oauth2/authorize | 授权端点              |
| JWK Set           | http://localhost/oauth2/jwks      | 资源服务器拉取公钥         |
| WebSocket（STOMP） | ws://localhost/ws-stomp           | 实时推送握手端点          |
| Actuator Health   | http://localhost/actuator/health  | 健康检查              |

## 关键配置（前缀 `eagle.*`）

| 配置项                              | 默认                                  | 说明                              |
|----------------------------------|-------------------------------------|---------------------------------|
| `eagle.jwt.keystore-location`    | `classpath:jwt-keystore.p12`        | JWT 签名 keystore（生产请挂外部）         |
| `eagle.jwt.keystore-password`    | `eagle-jwt-dev-2026`                | keystore 密码（生产必须改）              |
| `eagle.admin.password`           | env `EAGLE_ADMIN_PASSWORD`          | 初始管理员密码（必填）                     |
| `eagle.oauth.default-client.*`   | —                                   | 默认 OAuth2 公开客户端（PKCE）           |
| `eagle.wechat.mini-program.*`    | env `WECHAT_MINI_APP_*`             | 微信小程序 AppID / Secret            |
| `eagle.wechat.web.pc/h5.*`       | env `WECHAT_WEB_*` / `WECHAT_MP_*`  | 微信网页/H5 登录                      |
| `eagle.sms.aliyun.*`             | env `ALIYUN_SMS_*`                  | 阿里云短信 AccessKey / 签名 / 模板       |
| `eagle.log.cleanup.cron`         | `0 0 2 * * ?`                       | 审计日志每日清理                        |
| `eagle.websocket.endpoint`       | `/ws-stomp`                         | STOMP 握手路径                      |
| `spring.cloud.nacos.discovery.*` | env `NACOS_SERVER_ADDR / NAMESPACE` | Nacos 注册中心地址 / 命名空间             |

完整字段见 `src/main/resources/application.yml` 与 `application-{profile}.yml`。

## Profile

| Profile | 默认数据源       | 用途                |
|---------|-------------|-------------------|
| `local` | H2（内存）      | 本机快速启动，零依赖        |
| `dev`   | MySQL + 真实依赖 | 开发联调环境            |

切换：`SPRING_PROFILES_ACTIVE=dev` 或 `--spring.profiles.active=dev`。

## 数据库迁移

生产路径 `src/main/resources/db/migration/`，遵循 Flyway 命名 `V{yyyyMMddHHmm}__{snake}.sql`（详见
`.claude/rules/28-migration.md`）。本地开发为加快迭代默认使用 H2 + `ddl-auto: update`，**生产严禁**。

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
