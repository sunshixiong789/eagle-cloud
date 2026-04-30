---
name: eagle-idempotency
description: Use when implementing API idempotency in eagle-cloud projects — @Idempotent annotation, TOKEN/BUSINESS_KEY/RESULT_CACHE modes, IdempotencyKeyExtractor
---

# eagle-idempotency-starter — 接口幂等（Token / BUSINESS_KEY / RESULT_CACHE）

## 何时使用

- 防重复下单 / 重复支付 / 重复提交
- 客户端 / 网关重试导致重复请求
- 第三方异步通知防重（结果缓存模式对幂等重试友好）

## 何时不要使用

- 查询接口（天然幂等）
- 消息消费幂等（用 `eventId` + 唯一约束）

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-idempotency-starter')
implementation project(':eagle-starter:eagle-redis-starter')   // Token 存储依赖 Redis
```

```yaml
eagle.idempotency:
  enabled: true
  token-expire-seconds: 300        # Token 模式有效期 5 min
  result-cache-seconds: 86400      # BUSINESS_KEY / RESULT_CACHE 缓存 24h
  key-prefix: "eagle:idempotency:"
```

`IdempotencyTokenController` 自动暴露 Token 申请接口。

## 三种模式

| 模式             | 工作机制                                                                 | 重复请求行为              |
|----------------|----------------------------------------------------------------------|---------------------|
| `TOKEN`        | 客户端先 `POST /idempotency/token` 取 Token，请求时 `X-Idempotency-Token` 头携带 | Token 已消费 → 返回 409  |
| `BUSINESS_KEY` | 用 SpEL 提取业务键，Redis SetNX 防重                                          | 24h 内同 key → 返回 409 |
| `RESULT_CACHE` | 同 TOKEN，但**返回首次执行结果**（对重试友好）                                         | 重试直接命中缓存返回          |

## 核心 API

| 注解 / 类                                                       | 用途                                    |
|--------------------------------------------------------------|---------------------------------------|
| `@Idempotent(mode, key, tokenHeader, message, keyExtractor)` | 方法注解                                  |
| `IdempotencyMode`                                            | `TOKEN / BUSINESS_KEY / RESULT_CACHE` |
| `IdempotencyKeyExtractor`                                    | 自定义键提取扩展点（实现 + 注册 Bean，注解填 Bean 名）    |
| `IdempotencyAspect`                                          | 切面                                    |
| `IdempotencyTokenController`                                 | Token 申请接口（路径由 starter 决定）            |
| `IdempotencyErrorCode`                                       | 错误码（重复请求 → 409）                       |

## 最小示例

```java
// 模式 1：TOKEN（默认）
// 步骤 a) 客户端先调 POST /idempotency/token 拿 Token
// 步骤 b) 请求时 Header X-Idempotency-Token: <token>
@PostMapping("/orders")
@Idempotent
public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
    return orderService.create(request);
}

// 模式 2：BUSINESS_KEY（基于业务唯一标识）
@PostMapping("/payments")
@Idempotent(mode = IdempotencyMode.BUSINESS_KEY, key = "#request.orderNo")
public PaymentResponse pay(@Valid @RequestBody PayRequest request) {
    return paymentService.pay(request);
}

// 模式 3：RESULT_CACHE（重试友好，第三方回调推荐）
@PostMapping("/notify/wechat")
@Idempotent(mode = IdempotencyMode.RESULT_CACHE, key = "#request.transactionId")
public String wechatNotify(@RequestBody WechatNotifyRequest request) {
    return paymentNotifyService.handle(request);
}

// 自定义键提取器（复杂键）
@Component("orderKeyExtractor")
public class OrderKeyExtractor implements IdempotencyKeyExtractor {
    @Override
    public String extract(Object[] args) {
        CreateOrderRequest req = (CreateOrderRequest) args[0];
        return req.getUserId() + ":" + req.getProductId() + ":" + req.getRequestId();
    }
}

@PostMapping("/orders/v2")
@Idempotent(mode = IdempotencyMode.BUSINESS_KEY, keyExtractor = "orderKeyExtractor")
public OrderResponse createV2(...) { ...}
```

## 配置项

| key                                      | 类型      | 默认                   | 说明             |
|------------------------------------------|---------|----------------------|----------------|
| `eagle.idempotency.enabled`              | boolean | `true`               | 总开关            |
| `eagle.idempotency.token-expire-seconds` | long    | `300`                | Token 有效期      |
| `eagle.idempotency.result-cache-seconds` | long    | `86400`              | 业务键 / 结果缓存 TTL |
| `eagle.idempotency.key-prefix`           | String  | `eagle:idempotency:` | Redis Key 前缀   |

## 常见错误

- ❌ 模式名 `KEY` → ✅ 真实是 **`BUSINESS_KEY`**
- ❌ 注解写 `ttl="5m"` → ✅ 真实没有 ttl 字段，用全局 `token-expire-seconds`
- ❌ Header 名编造 → ✅ 默认 **`X-Idempotency-Token`**（可在注解 `tokenHeader` 覆盖）
- ❌ 第三方支付回调用 TOKEN 模式 → ✅ 用 **`RESULT_CACHE`** 对重试友好
- ❌ 查询接口加 `@Idempotent` → ✅ 仅写操作

## 关联规则

- `.claude/rules/05-api.md`
- `.claude/rules/15-messaging.md` — 消息侧幂等不同方案
