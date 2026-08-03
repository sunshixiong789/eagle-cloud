---
name: eagle-sentinel
description: Use when implementing rate limiting/circuit breaking in eagle-cloud projects — @RateLimit(qps/threads/behavior/warmUpPeriodSec/maxQueueingTimeMs), FlowControlBehavior enum (FAST_FAIL/WARM_UP/RATE_LIMITER, NOT REJECT), Sentinel dashboard integration
---

# eagle-sentinel-starter — 限流 / 熔断（Alibaba Sentinel）

## 何时使用

- 接口限流（防刷、保护后端）
- 服务熔断（下游故障快速失败）
- 流量整形 / 系统过载保护

## 何时不要使用

- 业务级幂等 → 用 `eagle-idempotency-starter`
- 单 IP / 单用户限流 → 用 `eagle-redis-starter` 的 `RedisRateLimiter`

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-sentinel-starter')
```

```yaml
spring.cloud.sentinel: # Sentinel Spring Cloud 标准配置
  transport:
    dashboard: ${SENTINEL_DASHBOARD:127.0.0.1:8858}
    port: 8719
  eager: true

eagle.sentinel:
  dashboard: 127.0.0.1:8858
  heartbeat-interval-ms: 10000
  origin-parser-enabled: true
  url-cleaner: true
```

## 核心 API

| 注解 / 类                                                                             | 说明                                            |
|------------------------------------------------------------------------------------|-----------------------------------------------|
| `@RateLimit(resource, qps, threads, behavior, warmUpPeriodSec, maxQueueingTimeMs)` | 限流注解，标在方法/类上                                  |
| `FlowControlBehavior`                                                              | 行为枚举：**`FAST_FAIL / WARM_UP / RATE_LIMITER`** |
| `RateLimitAspect`                                                                  | 切面（自动注册流控规则）                                  |
| `EagleSentinelBlockExceptionHandler`                                               | 限流异常 → 统一 HTTP 响应                             |
| `EagleSentinelRequestOriginParser`                                                 | 请求来源解析（`X-Application-Name`），用于授权规则           |
| `SentinelRuleManager`                                                              | 编程式规则管理（动态推送场景）                               |

也可用 Sentinel 原生 `@SentinelResource` 注解。

## 最小示例

```java

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    /** 默认 FAST_FAIL：超 100 QPS 直接拒绝 */
    @RateLimit(qps = 100)
    @PostMapping
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest req) {
        return orderService.create(req);
    }

    /** 预热：启动 20s 内阈值从低到 50 QPS 渐进 */
    @RateLimit(qps = 50, behavior = FlowControlBehavior.WARM_UP, warmUpPeriodSec = 20)
    @PostMapping("/express")
    public OrderResponse expressCreate(@Valid @RequestBody CreateOrderRequest req) {
        return orderService.expressCreate(req);
    }

    /** 匀速排队：超出 10/s 排队，最长等 1000ms */
    @RateLimit(qps = 10,
            behavior = FlowControlBehavior.RATE_LIMITER,
            maxQueueingTimeMs = 1000)
    @PostMapping("/sms")
    public void sendSms(String phone) { ...}

    /** QPS + 并发线程数双重限制 */
    @RateLimit(qps = 200, threads = 50)
    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id) {
        return orderService.findById(id);
    }

    /** 类级：所有方法共享 */
    @RateLimit(resource = "OrderController", qps = 500)
    @RestController
    public static class Group { /* ... */
    }
}
```

## 配置项

| key                                    | 类型      | 默认               | 说明                                                    |
|----------------------------------------|---------|------------------|-------------------------------------------------------|
| `eagle.sentinel.dashboard`             | String  | `localhost:8858` | Dashboard 地址                                          |
| `eagle.sentinel.heartbeat-interval-ms` | int     | `10000`          | 心跳间隔                                                  |
| `eagle.sentinel.origin-parser-enabled` | boolean | `true`           | 解析 `X-Application-Name` 识别调用方                         |
| `eagle.sentinel.url-cleaner`           | boolean | `true`           | URL 路径变量合并（`/users/1` 与 `/users/2` 合并为 `/users/{id}`） |

⚠️ Sentinel 完整配置走 `spring.cloud.sentinel.*`。

## @RateLimit 字段

| 字段                  | 类型     | 默认              | 说明                  |
|---------------------|--------|-----------------|---------------------|
| `resource`          | String | `""`（自动取类名.方法名） | 资源名                 |
| `qps`               | double | `100`           | QPS 阈值              |
| `threads`           | int    | `0`（不限）         | 并发线程数               |
| `behavior`          | enum   | `FAST_FAIL`     | 流控行为                |
| `warmUpPeriodSec`   | int    | `10`            | WARM_UP 预热时长        |
| `maxQueueingTimeMs` | int    | `500`           | RATE_LIMITER 最长排队时间 |

## FlowControlBehavior

| 值              | 行为          |
|----------------|-------------|
| `FAST_FAIL`    | 默认，超阈值直接拒绝  |
| `WARM_UP`      | 启动期阈值从低到高渐进 |
| `RATE_LIMITER` | 漏桶 / 匀速排队   |

## 常见错误

- ❌ 行为名 `REJECT` → ✅ **`FAST_FAIL`**
- ❌ 限流后无降级 → ✅ 提供 fallback 或友好错误响应
- ❌ 用 Sentinel 做用户级限流 → ✅ 用 `RedisRateLimiter`
- ❌ 阈值拍脑袋 → ✅ 压测后根据 P95/P99 推导
- ❌ 业务异常被计入熔断 → ✅ 用 `exceptionsToIgnore` 排除

## 关联规则

- `.claude/rules/05-security.md` — 防刷
- `.claude/rules/04-data.md` — 容量规划
