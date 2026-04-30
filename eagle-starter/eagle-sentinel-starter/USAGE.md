# eagle-sentinel-starter — 限流 / 熔断 / 降级（Alibaba Sentinel）

## 何时使用

- 接口限流（防刷、保护后端）
- 服务熔断（下游故障时快速失败）
- 流量整形（突发流量削峰）
- 系统过载保护（CPU / Load 自适应限流）

## 何时不要使用

- 业务级幂等（用 `eagle-idempotency-starter`）
- 单 IP / 单用户限流（用 `eagle-redis-starter` 的 `RedisRateLimiter`）

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-sentinel-starter')
```

```yaml
spring.cloud.sentinel:
  transport:
    dashboard: ${SENTINEL_DASHBOARD:127.0.0.1:8858}
    port: 8719
  datasource:
    nacos:
      server-addr: ${NACOS_SERVER:127.0.0.1:8848}
      data-id: ${spring.application.name}-sentinel-rules.json
      group-id: SENTINEL_GROUP
      rule-type: flow
  eager: true

eagle.sentinel:
  enabled: true
  default-rate-limit: 100               # 默认 QPS
```

## 核心 API

| 类 / 注解 | 用途 |
|---|---|
| `@RateLimit` | 方法级限流注解（含 QPS、阈值、降级方法） |
| `FlowControlBehavior` | 流控行为：`REJECT` / `WARM_UP` / `RATE_LIMITER` |
| `RateLimitAspect` | 切面（注解处理） |
| `SentinelRuleManager` | 编程式规则管理（动态推送） |
| `EagleSentinelBlockExceptionHandler` | 限流异常 → HTTP 429 + 错误码 |
| `EagleSentinelRequestOriginParser` | 请求来源解析（按 IP / 用户 / 租户限流） |

或用 Sentinel 原生 `@SentinelResource`。

## 最小示例

```java
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    /** 注解式：100 QPS，超限拒绝 */
    @RateLimit(qps = 100, behavior = FlowControlBehavior.REJECT)
    @PostMapping
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest req) {
        return orderService.create(req);
    }

    /** 含降级：限流时走 fallback */
    @RateLimit(qps = 50, fallback = "createFallback")
    @PostMapping("/express")
    public OrderResponse expressCreate(@Valid @RequestBody CreateOrderRequest req) {
        return orderService.expressCreate(req);
    }

    public OrderResponse createFallback(CreateOrderRequest req, Throwable ex) {
        // 降级逻辑：返回提示 / 走简化流程
        return OrderResponse.placeholder();
    }

    /** Sentinel 原生：更细粒度控制 */
    @SentinelResource(value = "orderQuery",
        blockHandler = "blockHandler",
        fallback = "queryFallback")
    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id) {
        return orderService.findById(id);
    }
}
```

## 规则配置（推荐 Nacos 动态推送）

```json
[
  {
    "resource": "POST:/api/v1/orders",
    "count": 100,
    "grade": 1,
    "controlBehavior": 0
  },
  {
    "resource": "orderQuery",
    "count": 500,
    "grade": 1
  }
]
```

`SentinelRuleManager` 启动时拉取，Nacos 推送变更实时生效。

## 配置项

| key | 类型 | 默认 | 说明 |
|---|---|---|---|
| `eagle.sentinel.enabled` | boolean | `true` | 总开关 |
| `eagle.sentinel.default-rate-limit` | int | `100` | 默认 QPS |
| `spring.cloud.sentinel.transport.dashboard` | String | — | Sentinel 控制台 |
| `spring.cloud.sentinel.eager` | boolean | `false` | 启动即上报 |

## 限流维度对比

| 工具 | 维度 | 场景 |
|------|------|------|
| Sentinel `@RateLimit` | 接口级总 QPS | 全局保护 |
| `RedisRateLimiter`（redis-starter）| IP / 用户 / 手机号 | 防刷 |
| 网关 Sentinel 规则 | 入口流量 | 边缘保护 |
| 数据库连接池 | 连接数 | 资源保护 |

通常**网关层 + 接口层**双重保护。

## 常见错误

- ❌ 限流后无降级 → ✅ 提供 fallback 或友好错误响应
- ❌ 用 Sentinel 做用户级限流 → ✅ 用 Redis 限流器
- ❌ 阈值拍脑袋 → ✅ 压测后根据 P95/P99 + 容量推导
- ❌ 仅生产开启 → ✅ staging 也开（提前发现规则错误）
- ❌ 业务异常被 Sentinel 计入熔断 → ✅ 通过 `exceptionsToIgnore` 排除业务异常

## 关联规则

- `.claude/rules/12-security.md` — 防刷限流
- `.claude/rules/23-performance.md` — 容量规划
- `.claude/rules/24-deployment.md` — 网关 Sentinel 接入
