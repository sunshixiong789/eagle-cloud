# eagle-idempotency-starter — 接口幂等（Token 模式 / Key 模式）

## 何时使用

- 支付、下单、提交表单等"重复点击/重试不应重复执行"的接口
- 网络抖动 / 客户端重试导致重复请求
- 跨服务调用的幂等保证（与消息消费幂等区分）

## 何时不要使用

- 查询接口（天然幂等）
- 服务内消息消费幂等（用 `eventId` + 唯一约束，详见 `15-messaging.md`）

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-idempotency-starter')
implementation project(':eagle-starter:eagle-redis-starter')   // Token 存储依赖 Redis
```

```yaml
eagle.idempotency:
  enabled: true
  default-mode: KEY                    # TOKEN / KEY
  default-ttl: 5m                      # 防重时间窗
  key-prefix: eagle:idempotent:
  token-endpoint: /api/idempotency/token
```

## 核心 API

| 类 / 接口 / 注解 | 用途 |
|---|---|
| `@Idempotent` | 方法注解：声明幂等模式 |
| `@IdempotencyKey` | 参数注解：指定生成 key 的字段 |
| `IdempotencyMode` | 模式：`TOKEN`（前置申请）/ `KEY`（业务主键）|
| `IdempotencyAspect` | 切面 |
| `IdempotencyTokenController` | Token 申请接口（`POST /api/idempotency/token`） |
| `IdempotencyKeyExtractor` | Key 提取扩展点（业务可自定义实现） |
| `IdempotencyErrorCode` | 错误码（`DUPLICATE_REQUEST` 409） |

## 最小示例

```java
// 模式 1：KEY 模式（推荐，业务参数即天然 key）
@PostMapping("/orders")
@Idempotent(mode = IdempotencyMode.KEY, ttl = "5m")
public OrderResponse create(@RequestBody @Valid CreateOrderRequest request) {
    return orderService.create(request);
}

public class CreateOrderRequest {
    @IdempotencyKey                      // 这个字段拼成 idempotency key
    private String requestId;            // 客户端生成 UUID
    private List<Long> productIds;
}

// 模式 2：TOKEN 模式（敏感场景，先申请再使用）
// 1) 客户端先调 POST /api/idempotency/token 获得 token
// 2) 业务接口带上 Token 调用
@PostMapping("/payment")
@Idempotent(mode = IdempotencyMode.TOKEN, ttl = "10m")
public PayResponse pay(@RequestHeader("Idempotency-Token") String token,
                       @RequestBody PayRequest request) {
    return paymentService.pay(request);
}
```

重复请求返回 HTTP 409 + `IdempotencyErrorCode.DUPLICATE_REQUEST`。

## 配置项

| key | 类型 | 默认 | 说明 |
|---|---|---|---|
| `eagle.idempotency.enabled` | boolean | `true` | 总开关 |
| `eagle.idempotency.default-mode` | enum | `KEY` | 默认模式 |
| `eagle.idempotency.default-ttl` | Duration | `5m` | 默认 TTL |
| `eagle.idempotency.key-prefix` | String | `eagle:idempotent:` | Redis Key 前缀 |
| `eagle.idempotency.token-endpoint` | String | `/api/idempotency/token` | Token 申请路径 |

## 常见错误

- ❌ 查询接口加 `@Idempotent` → ✅ 仅写操作
- ❌ KEY 模式不声明 `@IdempotencyKey` → ✅ 必须指定生成 Key 的字段
- ❌ TTL 过短（< 1 分钟）→ ✅ 至少覆盖客户端重试窗口
- ❌ 用此 starter 做消息消费幂等 → ✅ 消息侧用 `eventId` + 唯一约束
- ❌ TOKEN 模式不申请就调用 → ✅ 客户端先 `POST /token`

## 关联规则

- `.claude/rules/05-api.md` — 接口规范
- `.claude/rules/15-messaging.md` — 消息消费幂等（不同方案）
- `.claude/rules/16-transaction-distributed.md` — TCC 幂等
