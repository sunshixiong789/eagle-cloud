# eagle-gateway-service

Eagle 平台**API 网关**，基于 Spring Cloud Gateway（WebFlux 响应式栈），承担流量入口、JWT 鉴权、限流、链路追踪、OpenAPI
文档聚合等职责。

## 定位

- **统一入口**：浏览器 / 移动端 / 第三方调用唯一入口（默认 `:8080`）
- **JWT 鉴权**：在网关层完成 Token 校验，下游服务通过 `eagle-resource-server-starter` 仅做轻量解析
- **动态路由**：基于 Nacos 服务发现，`lb://` 自动负载均衡
- **限流熔断**：Sentinel Gateway 流控规则
- **CORS 统一处理**：避免每个下游服务重复配置
- **OpenAPI 聚合**：通过 SpringDoc 动态发现各微服务的 `/v3/api-docs`，统一 Swagger UI
- **Seata XID 透传**：分布式事务上下文传递
- **链路追踪**：`eagle-tracing-starter`（B3 / Brave / Zipkin）

## 依赖（build.gradle）

```
eagle-common-starter

spring-cloud-starter-gateway-server-webflux       # 响应式网关核心
spring-boot-starter-oauth2-resource-server        # JWT 验证
spring-boot-starter-data-redis-reactive           # Redis 响应式（限流/黑名单）

springdoc-openapi-starter-webflux-ui              # OpenAPI 聚合
spring-cloud-starter-alibaba-nacos-discovery      # 服务发现
spring-cloud-alibaba-sentinel-gateway             # 网关限流
spring-cloud-starter-alibaba-sentinel             # Sentinel 客户端
seata-spring-boot-starter                         # 分布式事务 XID 透传

eagle-tracing-starter                             # 链路追踪
```

## 主要组件

| 组件                              | 作用                                         |
|---------------------------------|--------------------------------------------|
| `JwtAuthenticationGlobalFilter` | 全局 JWT 校验过滤器，未通过返回 401（`/public/**` 等放行）   |
| `SeataXidFilter`                | 解析 / 注入 `TX_XID` 头，跨服务透传分布式事务上下文           |
| `RequestLoggingGlobalFilter`    | 请求/响应日志埋点，记录耗时与状态码                         |
| `GatewayWebExceptionHandler`    | 统一异常响应，按 ErrorCode 体系返回标准 JSON             |
| `GatewaySecurityConfig`         | Spring Security WebFlux 链装配（OAuth2 资源服务器） |
| `SentinelGatewayConfig`         | 流控规则 BlockHandler 与默认拦截响应                  |
| `GatewayOpenApiConfig`          | 通过 Nacos 实例列表动态注册 SpringDoc Group          |
| `GatewayProperties`             | `eagle.gateway.*` 配置绑定（白名单 / 授权服务器 URL 等）  |

## 启动

```bash
./gradlew :eagle-services:eagle-gateway-service:bootRun
```

| 端点                | 默认地址                                          | 说明                             |
|-------------------|-----------------------------------------------|--------------------------------|
| Swagger UI（聚合）   | http://localhost:8080/swagger-ui              | 各服务接口聚合视图                      |
| OpenAPI JSON      | http://localhost:8080/v3/api-docs             | 配置中心                           |
| 系统服务转发           | http://localhost:8080/api/system/**           | → `lb://eagle-system-server`   |
| OAuth2 转发        | http://localhost:8080/oauth2/**               | → `lb://eagle-system-server`   |
| Actuator         | http://localhost:8080/actuator/{health,info,gateway} | 健康检查 / 路由列表                 |

## 关键配置

### 路由（`application.yml: spring.cloud.gateway.routes`）

```yaml
routes:
  - id: system-server-api-docs
    uri: lb://eagle-system-server
    predicates: [Path=/v3/api-docs/eagle-system-server]
    filters: [SetPath=/v3/api-docs]

  - id: system-server
    uri: lb://eagle-system-server
    predicates: [Path=/api/system/**,/oauth2/**,/login,/logout]
    filters: [StripPrefix=0]

  - id: default
    uri: lb://eagle-system-server
    predicates: [Path=/**]                    # 兜底
```

新增下游服务时按 `id / uri / predicates / filters` 追加；请将更具体的路由放在兜底之前。

### 公开路径白名单（`eagle.gateway.security.public-paths`）

不参与 JWT 鉴权的请求路径（Ant 风格）。默认包含 `/public/**`、Actuator 健康端点、Swagger 资源、`/oauth2/**`、`/login`、
`/error`。

### HTTP 客户端超时（`spring.cloud.gateway.httpclient`）

| 配置                 | 默认    | 说明                |
|--------------------|-------|-------------------|
| `connect-timeout`  | 3000  | 连接超时（毫秒），下游不可达时快速失败 |
| `response-timeout` | `30s` | 响应超时，防止慢请求拖垮网关    |

### Sentinel

控制台地址通过 `SENTINEL_DASHBOARD` 环境变量注入，默认 `localhost:8858`；网关使用 `SentinelGatewayFilter`，已关闭 Servlet
Filter（`spring.cloud.sentinel.filter.enabled: false`）。

### Nacos

通过 `NACOS_SERVER_ADDR` / `NACOS_NAMESPACE` / `NACOS_GROUP` 环境变量配置；网关通过服务名 `lb://eagle-system-server`
访问下游，无需硬编码 IP。

## 鉴权流程

```
Client ──Authorization: Bearer <JWT>──→ Gateway
                                          │
                                          ├─ 1. 公开路径白名单匹配 → 直接放行
                                          ├─ 2. JwtAuthenticationGlobalFilter
                                          │     ├─ JWK Set URI = ${eagle.gateway.security.auth-server-url}/oauth2/jwks
                                          │     └─ 校验签名 / 过期 / 黑名单
                                          ├─ 3. SentinelGatewayFilter（限流）
                                          ├─ 4. SeataXidFilter（注入 TX_XID）
                                          ├─ 5. RequestLoggingGlobalFilter（日志）
                                          └─→ 转发到下游服务（lb://...）
```

## 常见运维场景

### 新增下游服务

1. 下游服务注册到同一 Nacos `namespace + group`
2. 在 `spring.cloud.gateway.routes` 追加路由
3. 如需聚合 OpenAPI，确保下游 `/v3/api-docs` 可访问；`GatewayOpenApiConfig` 自动按 Nacos 服务列表注册 SpringDoc Group

### 灰度 / 金丝雀

通过 `X-Eagle-Gray` 头（已在 `eagle-common-starter` 标准化）+ Nacos 元数据 + 自定义 LoadBalancer 实现；详见
`.claude/rules/24-deployment.md`。

### Token 黑名单

服务端登出时将 `jti` 写入 Redis（`eagle:auth:revoked:{jti}`），`JwtAuthenticationGlobalFilter` 在校验签名后查询黑名单。

## 容器化

`Dockerfile` 已就绪：

```bash
./gradlew :eagle-services:eagle-gateway-service:bootJar
docker build -t eagle/gateway-service:1.0.0 eagle-services/eagle-gateway-service
```

需注入的环境变量：`NACOS_SERVER_ADDR` / `NACOS_NAMESPACE` / `AUTH_SERVER_URL` / `SENTINEL_DASHBOARD` 等。

## 注意事项

- **WebFlux 服务**：禁止在过滤器或路由中调用阻塞 API（JDBC / JPA / 同步 Feign）；如必需用 `Schedulers.boundedElastic()`
- 生产 `allowedOriginPatterns` 必须显式枚举具体域名，禁止 `*` + `allowCredentials: true` 同时开启
- `springdoc.api-docs.enabled` / `springdoc.swagger-ui.enabled` 在生产环境**必须关闭**（详见 `.claude/rules/18-openapi.md`）
