# eagle-gateway-service

Eagle 平台 **API 网关**，基于 Spring Cloud Gateway（WebFlux），承担流量入口、路由、限流、CORS、链路追踪、请求增强、OpenAPI 文档聚合。

默认监听 `:8080`，Consul 注册名是 **`gateway-server`**（不是 compose 服务名 `gateway`）。

## 定位

- **统一入口**：浏览器 / 移动端 / 第三方调用的唯一入口
- **纯路由 + Header 透传**：网关 **不做** JWT 验签 / 用户身份提取（已下沉到下游 `eagle-resource-server-starter`）
- **动态路由**：Consul 服务发现 + `discovery.locator`，按服务名生成 `/auth/**`、`/system/**`；WebSocket 走 `lb:ws://`
- **集群限流**：Spring Cloud Gateway `RequestRateLimiter` + Redis 令牌桶（取代 Sentinel）
- **CORS 统一处理**：由 `GatewayCorsConfig` 注册的唯一 `CorsWebFilter` 落地，避免下游重复配置
- **请求增强**：注入 `X-Request-Id` / `X-Real-IP`
- **内部路径拦截**：外部访问任何含 `/internal/` 的路径一律 403
- **链路追踪**：`eagle-tracing-starter`（B3 / Brave / Zipkin）
- **OpenAPI 聚合**：按 Consul 实例 metadata `spring-doc` 动态发现下游 `/v3/api-docs`
- **统一错误响应**：路由失败 / 超时 / 下游不可达返回与业务一致的 JSON

## 为什么网关不做 JWT 鉴权

历史上曾经在网关层做 JWT 验签，导致：

1. 概念错位：网关并非 OAuth2 Resource Server，自己没有受保护的领域资源
2. 双重鉴权：Spring Security 默认 filter chain 覆盖自定义白名单，`/actuator/health` 也被 401

现状：**网关纯路由**。OAuth2 规范路径（`/oauth2/**`、`/.well-known/**`、`/userinfo`、`/login`）按原路径透传到 **auth-service**。

## 依赖

```
eagle-common-starter
eagle-tracing-starter

spring-cloud-starter-gateway-server-webflux
spring-cloud-starter-loadbalancer
springdoc-openapi-starter-webflux-ui

spring-cloud-starter-consul-discovery
spring-cloud-starter-consul-config
spring-boot-starter-data-redis-reactive   # RequestRateLimiter 后端
```

## 主要组件

| 组件 | 作用 |
|---|---|
| `RequestEnrichmentGlobalFilter` | 注入 `X-Request-Id`（优先复用上游）+ `X-Real-IP`；响应 `beforeCommit` 覆盖式写回 |
| `InternalPathBlockingGlobalFilter` | 拦截路径中含 `/internal/` 的外部请求，返回 403 |
| `RequestLoggingGlobalFilter` | 请求 / 响应日志，记录耗时与状态码 |
| `GatewayWebExceptionHandler` | `@Order(-2)` 统一异常 JSON，含 `requestId` |
| `GatewayRateLimitConfig` | Redis 令牌桶的 KeyResolver（默认客户端 IP，只信任最后一跳 XFF） |
| `GatewayOpenApiConfig` + `OpenApiRouteLocator` | 按 Consul metadata `spring-doc` 聚合文档，并生成 `/v3/api-docs/{alias}` 转发路由 |
| `GatewayCorsConfig` | 唯一 CORS 处理点 |
| `SeataXidFilter` | 若上游带了 `TX_XID` 头则原样透传（Seata starter 已移除，仅兼容遗留调用方） |

## 启动

```bash
export CONSUL_TOKEN=<service-token>   # local / 连远程 Consul 时必填
gradle :eagle-services:eagle-gateway-service:bootRun
```

| 端点 | 默认地址 | 说明 |
|---|---|---|
| Swagger UI（聚合） | http://localhost:8080/swagger-ui.html | 各服务接口聚合 |
| OpenAPI JSON | http://localhost:8080/v3/api-docs | 文档入口 |
| Auth 规范路径 | `/oauth2/**` `/.well-known/**` `/userinfo` `/login` `/logout` `/connect/**` | → `lb://auth` |
| Auth 业务 | `/accounts/**` `/sms/**` | → `lb://auth` |
| 服务发现路由 | `/auth/**` `/system/**` | Consul locator 自动生成 |
| WebSocket / STOMP | `ws://localhost:8080/system/ws-stomp` | → `lb:ws://system`，StripPrefix=1 |
| Actuator | http://localhost:8080/actuator/{health,info,gateway} | 健康检查 / 路由列表 |

## 关键配置

### 显式路由（`spring.cloud.gateway.server.webflux.routes`）

OAuth2 / OIDC 客户端必须按 discovery 里的字面 URL 发请求，这些路径不能加服务名前缀，也不能 rewrite：

```yaml
routes:
  - id: auth-oauth2
    uri: lb://auth
    predicates: [Path=/oauth2/**,/.well-known/**,/userinfo,/logout,/connect/**]

  - id: auth-userinfo-alias
    uri: lb://auth
    predicates: [Path=/auth/userinfo]
    filters: [StripPrefix=1]

  - id: auth-login
    uri: lb://auth
    predicates: [Path=/login,/login/**]

  - id: auth-account
    uri: lb://auth
    predicates: [Path=/accounts/**,/sms/**]

  - id: system-server-ws
    uri: lb:ws://system
    predicates: [Path=/system/ws-stomp,/system/ws-stomp/**]
    filters: [StripPrefix=1]
```

其余业务路径靠 `discovery.locator.enabled=true` 按服务名转发。`local` profile 会关掉 Consul discovery 与 OpenAPI 自动聚合。

### 响应压缩

只开 `server.compression`。不要开 `spring.cloud.gateway.server.webflux.httpclient.compression`，否则会「上游 → 网关解压 → 网关重压」。

### 限流

网关用 Redis 令牌桶，阈值是**集群真实阈值**，副本数变化不会漂移。Key 默认是客户端 IP（`XForwardedRemoteAddressResolver.maxTrustedIndex(1)`）。

### Consul

通过 `CONSUL_HOST` / `CONSUL_PORT` / `CONSUL_TOKEN` / `CONSUL_INSTANCE_IP` 配置。KV 路径是 `config/gateway-server,<profile>/data`，**不是** `config/gateway,...`。

配置中心热更新关闭，改完 KV 需重启。详见 [`../docs/consul-config.md`](../docs/consul-config.md)。

## 请求流转

```
Client ──→ Gateway
            ├─ 1. RequestEnrichmentGlobalFilter（HIGHEST_PRECEDENCE）
            │       ├─ 生成 / 复用 X-Request-Id
            │       ├─ 提取真实 IP → X-Real-IP
            │       └─ 响应 beforeCommit：set X-Request-Id
            ├─ 2. InternalPathBlockingGlobalFilter（拦截 /internal/**）
            ├─ 3. SeataXidFilter（若有 TX_XID 则透传）
            ├─ 4. RequestRateLimiter（Redis 令牌桶）
            ├─ 5. NettyRoutingFilter（lb:// 或 lb:ws://）
            ├─ 6. RequestLoggingGlobalFilter
            └─→ 下游
                    ├─ RequestIdMdcFilter 读 X-Request-Id → MDC
                    └─ eagle-resource-server-starter 校验 JWT
```

异常响应：

```json
{
  "timestamp": "2026-05-11T02:00:00Z",
  "status": 503,
  "error": "Service Unavailable",
  "message": "服务暂时不可用，请稍后重试",
  "path": "/system/users",
  "errorCode": null,
  "requestId": "4f9e8a12-3456-..."
}
```

## 新增下游服务

1. 下游注册到同一 Consul，并设置实例 metadata：`spring-doc=<alias>`、`spring-doc-name=<显示名>`
2. metadata 必须写 Consul 原生 Meta；写成 tags 时网关 `getMetadata()` 取不到，聚合恒为空
3. 需要独立路径前缀时追加 `routes[]`；更具体的路由放在兜底之前
4. 内部 API 一律挂在 `/internal/**`，由网关统一拦截

## 容器化

```bash
gradle :eagle-services:eagle-gateway-service:bootJar
docker build -t eagle/gateway-service:1.9.0 eagle-services/eagle-gateway-service
```

compose 注入的引导变量：`SPRING_PROFILES_ACTIVE` / `CONSUL_*` / `REDIS_DATABASE` / `JAVA_OPTS`。其余配置进 Consul KV。

## 注意事项

- WebFlux 服务禁止在过滤器里调用阻塞 API（JDBC / JPA）
- 生产 `allowedOriginPatterns` 必须枚举具体域名，禁止 `*` + `allowCredentials: true`
- 生产关闭 `springdoc.api-docs.enabled` / `springdoc.swagger-ui.enabled`
- 网关不做鉴权：新增需登录的端点，确保下游未把它放进 `eagle.resource-server.permit-paths`
