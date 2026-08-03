# eagle-gateway-service

Eagle 平台 **API 网关**，基于 Spring Cloud Gateway（WebFlux 响应式栈），承担流量入口、路由、限流、CORS、链路追踪、
请求增强、OpenAPI 文档聚合等职责。

## 定位

- **统一入口**：浏览器 / 移动端 / 第三方调用唯一入口（默认 `:8080`）
- **纯路由 + Header 透传**：网关 **不做** JWT 验签 / 用户身份提取（已下沉到下游 `eagle-resource-server-starter`），
  网关仅负责将请求与必要的 Header 转发给业务服务
- **动态路由**：基于 Nacos 服务发现，`lb://` 自动负载均衡；WebSocket 走 `lb:ws://`
- **限流熔断**：Sentinel Gateway 流控规则 + Nacos 规则持久化（重启不丢规则）
- **CORS 统一处理**：避免每个下游服务重复配置
- **请求增强**：注入 `X-Request-Id` / `X-Real-IP`，全链路定位
- **Seata XID 透传**：分布式事务上下文传递
- **链路追踪**：`eagle-tracing-starter`（B3 / Brave / Zipkin）
- **OpenAPI 聚合**：通过 SpringDoc 动态发现各微服务的 `/v3/api-docs`，统一 Swagger UI
- **统一错误响应**：路由失败 / 超时 / 下游不可达统一返回 `ErrorResult` 格式（与业务异常 JSON 一致）

## 为什么网关不做 JWT 鉴权

历史上曾经在网关层做 JWT 验签（引入 `spring-boot-starter-oauth2-resource-server`），导致：

1. 概念错位：网关并非 OAuth2 Resource Server，自己没有受保护的领域资源
2. 双重鉴权：Spring Security 默认 `SecurityWebFilterChain` 在 WebFilter 层强制要求 Token，
   覆盖了网关自定义的 `JwtAuthenticationGlobalFilter` 白名单，`/actuator/health` 等也被 401 拦截

现状：**网关纯路由**，所有 JWT 验签 + 用户身份提取由下游 `eagle-resource-server-starter` 负责，
OAuth2 端点（`/oauth2/**`、`/login`）由 `eagle-system-service` 的 OAuth2 Authorization Server SecurityFilterChain 自带放行规则处理。

## 依赖（build.gradle）

```
eagle-common-starter                              # 基础设施 + RequestIdMdcFilter（Servlet 环境用）
eagle-tracing-starter                             # 链路追踪 + traceId 注入

spring-cloud-starter-gateway-server-webflux       # 响应式网关核心
springdoc-openapi-starter-webflux-ui              # OpenAPI 聚合

spring-cloud-starter-alibaba-nacos-discovery      # 服务发现
spring-cloud-alibaba-sentinel-gateway             # 网关限流
spring-cloud-starter-alibaba-sentinel             # Sentinel 客户端
sentinel-datasource-nacos                         # 规则持久化到 Nacos
```

## 主要组件

| 组件                              | 作用                                                                                                           |
|---------------------------------|--------------------------------------------------------------------------------------------------------------|
| `RequestEnrichmentGlobalFilter` | 注入 `X-Request-Id`（UUID v4，优先复用上游已传）+ `X-Real-IP`，写入 exchange.attribute 供异常处理器复用，响应头 `beforeCommit` 覆盖式注入     |
| `SeataXidFilter`                | 解析 / 注入 `TX_XID` 头，跨服务透传分布式事务上下文                                                                             |
| `RequestLoggingGlobalFilter`    | 请求/响应日志埋点，记录耗时与状态码                                                                                           |
| `GatewayWebExceptionHandler`    | `@Order(-2)` 统一异常响应，覆盖超时 / 连接拒绝 / PrematureClose / ResponseStatusException，输出 `ErrorResult` JSON 含 requestId |
| `SentinelGatewayConfig`         | 流控规则 BlockHandler 与默认拦截响应                                                                                    |
| `GatewayOpenApiConfig`          | 通过 Nacos 实例列表动态注册 SpringDoc Group                                                                            |

## 启动

```bash
./gradlew :eagle-services:eagle-gateway-service:bootRun
```

| 端点                | 默认地址                                                 | 说明                              |
|-------------------|------------------------------------------------------|---------------------------------|
| Swagger UI（聚合）    | http://localhost:8080/swagger-ui                     | 各服务接口聚合视图                       |
| OpenAPI JSON      | http://localhost:8080/v3/api-docs                    | 配置中心                            |
| 系统服务转发            | http://localhost:8080/api/system/**                  | → `lb://eagle-system-server`    |
| OAuth2 转发         | http://localhost:8080/oauth2/**                      | → `lb://eagle-system-server`    |
| WebSocket / STOMP | ws://localhost:8080/ws-stomp                         | → `lb:ws://eagle-system-server` |
| Actuator          | http://localhost:8080/actuator/{health,info,gateway} | 健康检查 / 路由列表                     |

## 关键配置

### 路由（`spring.cloud.gateway.server.webflux.routes`）

```yaml
routes:
  - id: system-server-ws                    # WebSocket / STOMP（需独立路由）
    uri: lb:ws://eagle-system-server
    predicates: [Path=/ws-stomp/**]

  - id: system-server-api-docs              # 文档拉取（10s 超时）
    uri: lb://eagle-system-server
    predicates: [Path=/v3/api-docs/eagle-system-server]
    filters:    [SetPath=/v3/api-docs]
    metadata:   { response-timeout: 10000, connect-timeout: 3000 }

  - id: system-server                       # 业务接口（GET 幂等重试 2 次）
    uri: lb://eagle-system-server
    predicates: [Path=/api/system/**,/oauth2/**,/login,/logout]
    filters:
      - StripPrefix=0
      - name: Retry
        args:
          retries: 2
          methods:  [GET]
          statuses: [BAD_GATEWAY, GATEWAY_TIMEOUT, SERVICE_UNAVAILABLE]
          backoff:  { firstBackoff: 100ms, maxBackoff: 500ms, factor: 2 }
    metadata: { response-timeout: 30000, connect-timeout: 3000 }

  - id: default                             # 兜底
    uri: lb://eagle-system-server
    predicates: [Path=/**]
    metadata: { response-timeout: 30000, connect-timeout: 3000 }
```

新增下游服务时按 `id / uri / predicates / filters` 追加；将更具体的路由放在兜底之前。

### 请求体大小限制

```yaml
spring.codec.max-in-memory-size: 2MB
```

WebFlux 在内存中暂存请求体的最大字节数，防止超大 Body 拖垮网关。

### 响应压缩

```yaml
server.compression:
  enabled: true
  mime-types: application/json,application/xml,text/html,text/xml,text/plain,application/javascript,text/css
  min-response-size: 1024
```

> 注意：**不开启** `spring.cloud.gateway.server.webflux.httpclient.compression`。
> 否则链路变成"上游 → 网关解压 → 网关重压"，CPU 浪费。仅在 `server.compression` 层做一次压缩。

### 路由级超时（`routes[].metadata`）

| 字段                 | 单位 | 说明                              |
|--------------------|----|---------------------------------|
| `connect-timeout`  | 毫秒 | 连接超时，下游不可达时快速失败                 |
| `response-timeout` | 毫秒 | 响应超时，路由级覆盖 httpclient 全局默认（30s） |

### Sentinel

| 配置                                                  | 说明                                                                                  |
|-----------------------------------------------------|-------------------------------------------------------------------------------------|
| `SENTINEL_DASHBOARD`                                | Dashboard 地址，docker-compose 默认 `sentinel-dashboard:8858`；留空禁用上报                     |
| `spring.cloud.sentinel.datasource.ds-flow.nacos`    | 流控规则从 Nacos 加载并热刷新（dataId `eagle-gateway-server-flow-rules`，group `SENTINEL_GROUP`） |
| `spring.cloud.sentinel.datasource.ds-degrade.nacos` | 降级规则（dataId `eagle-gateway-server-degrade-rules`）                                   |
| `spring.cloud.sentinel.filter.enabled: false`       | Gateway 用 `SentinelGatewayFilter`，关闭 Servlet Filter                                 |

**Nacos 中需预先创建对应 dataId**（首次启动会 warn 一次，可忽略）：

```bash
# 控制台手动创建,或通过 SDK 推送空数组占位
dataId: eagle-gateway-server-flow-rules    group: SENTINEL_GROUP   content: []
dataId: eagle-gateway-server-degrade-rules group: SENTINEL_GROUP   content: []
```

### Nacos 服务发现

通过 `NACOS_SERVER_ADDR` / `NACOS_NAMESPACE` / `NACOS_GROUP` 环境变量配置；网关通过服务名 `lb://eagle-system-server`
访问下游，无需硬编码 IP。

## 请求流转

```
Client ──→ Gateway
            │
            ├─ 1. RequestEnrichmentGlobalFilter（HIGHEST_PRECEDENCE）
            │       ├─ 生成/复用 X-Request-Id（UUID v4）
            │       ├─ 提取真实 IP → X-Real-IP（XFF 首段 / RemoteAddress）
            │       └─ 响应 beforeCommit：set X-Request-Id（覆盖式，避免上游同名头叠加）
            ├─ 2. SeataXidFilter（HIGHEST_PRECEDENCE + 99，透传 TX_XID）
            ├─ 3. SentinelGatewayFilter（限流）
            ├─ 4. NettyRoutingFilter（路由到 lb://...）
            ├─ 5. RequestLoggingGlobalFilter（LOWEST_PRECEDENCE - 100，日志）
            └─→ 转发到下游服务（lb:// 或 lb:ws://）
                    │
                    └─ 下游 RequestIdMdcFilter 从 X-Request-Id 读 → MDC
                       下游 eagle-resource-server-starter 校验 JWT → SecurityContext
```

异常时由 `GatewayWebExceptionHandler` 返回 JSON：

```json
{
  "timestamp": "2026-05-11T02:00:00Z",
  "status": 503,
  "error": "Service Unavailable",
  "message": "服务暂时不可用，请稍后重试",
  "path": "/api/system/users",
  "errorCode": null,
  "requestId": "4f9e8a12-3456-..."
}
```

## Request ID 全链路追溯

| 阶段            | 行为                                                                    |
|---------------|-----------------------------------------------------------------------|
| 客户端 → 网关      | 可主动带 `X-Request-Id` 头（无则网关生成 UUID）                                    |
| 网关入口          | `RequestEnrichmentGlobalFilter` 写入 exchange.attribute + 透传给下游请求       |
| 网关响应          | `beforeCommit` 阶段 set 响应头 `X-Request-Id`                              |
| 下游 Servlet 服务 | `RequestIdMdcFilter`（在 `eagle-common-starter` 自动装配）从请求头读取 → MDC + 响应头 |
| 下游异常响应        | `ErrorResult.of(...)` 自动从 MDC 取 requestId 写入 JSON                     |
| 网关异常响应        | `GatewayWebExceptionHandler` 从 exchange.attribute 取 requestId 写入 JSON |

前端可同时通过响应头 `X-Request-Id` 与 body `requestId` 字段定位整条链路的日志/堆栈。

## 常见运维场景

### 新增下游服务

1. 下游服务注册到同一 Nacos `namespace + group`
2. 在 `spring.cloud.gateway.server.webflux.routes` 追加路由
3. 如需聚合 OpenAPI，确保下游 `/v3/api-docs` 可访问；`GatewayOpenApiConfig` 自动按 Nacos 服务列表注册 SpringDoc Group

### 灰度 / 金丝雀

通过 Nacos 元数据 + 自定义 LoadBalancer 实现。

### Sentinel 规则修改

通过 Sentinel Dashboard 推送的修改会通过 sentinel-datasource-nacos 异步同步回 Nacos，保证客户端重启后规则不丢。

## 容器化

`Dockerfile` 已就绪：

```bash
./gradlew :eagle-services:eagle-gateway-service:bootJar
docker build -t eagle/gateway-service:1.0.0 eagle-services/eagle-gateway-service
```

需注入的环境变量：`SPRING_PROFILES_ACTIVE` / `NACOS_SERVER_ADDR` / `NACOS_NAMESPACE` / `SENTINEL_DASHBOARD` /
`CORS_ALLOWED_ORIGINS` 等。

## 注意事项

- **WebFlux 服务**：禁止在过滤器或路由中调用阻塞 API（JDBC / JPA / 同步 Feign）；如必需用 `Schedulers.boundedElastic()`
- 生产 `allowedOriginPatterns` 必须显式枚举具体域名，禁止 `*` + `allowCredentials: true` 同时开启
- `springdoc.api-docs.enabled` / `springdoc.swagger-ui.enabled` 在生产环境**必须关闭**（详见
  `.claude/rules/03-api-error.md`）
- **网关不做鉴权**：所有 JWT 校验在下游 `eagle-resource-server-starter`；如新增需要鉴权的端点，确保下游路径未在
  `eagle.resource-server.permit-paths` 中放行
