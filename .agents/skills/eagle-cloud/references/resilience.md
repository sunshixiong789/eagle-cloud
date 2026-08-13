---
name: eagle-resilience
description: Use when adding fault tolerance to eagle-cloud services — Resilience4J CircuitBreaker / Retry / TimeLimiter with pre-configured eagle-default instance, @CircuitBreaker fallbackMethod signature requirements, annotation composition order (TimeLimiter→CircuitBreaker→Retry), AI-specific eagle-ai-default instance
---

# eagle-resilience-starter — 熔断 / 重试 / 超时（Resilience4J）

## 何时使用

- 调用外部服务（Feign / RestClient / WebClient）需要熔断保护
- 数据库 / Redis 瞬时故障需要重试
- 异步任务或 AI 调用需要超时控制

## 何时不要使用

- 核心支付 / 扣库存 → 快速失败 + 告警，**不**加熔断掩盖问题
- 事务方法内 `@Retry`（事务已提交无法回滚）
- 非幂等操作（POST / PUT / DELETE）加 `@Retry`

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-resilience-starter')
```

starter 自动注册 `eagle-default` 实例（CircuitBreaker + Retry + TimeLimiter 三合一）。

```yaml
eagle:
  resilience:
    circuit-breaker:
      failure-rate-threshold: 50          # 错误率触发阈值（%）
      slow-call-rate-threshold: 100       # 慢调用率阈值（%）
      slow-call-duration-threshold: 2s    # 慢调用判定时长
      wait-duration-in-open-state: 30s    # 开路等待时长
      sliding-window-size: 100
      minimum-number-of-calls: 10
      permitted-number-of-calls-in-half-open-state: 10
    retry:
      max-attempts: 3                     # 最大重试次数（含首次）
      wait-duration: 500ms
      exponential-backoff-multiplier: 2.0
      exponential-max-wait-duration: 10s
    time-limiter:
      timeout-duration: 5s
      cancel-running-future: true
```

自定义实例通过 Resilience4J 原生配置叠加：

```yaml
resilience4j:
  circuitbreaker:
    instances:
      payment-gateway:
        failure-rate-threshold: 30
        wait-duration-in-open-state: 60s
```

## 注解用法

### `@CircuitBreaker`

```java
@CircuitBreaker(name = "eagle-default", fallbackMethod = "getStockFallback")
public StockResponse getStock(Long productId) {
    return inventoryClient.getStock(productId);
}

// fallback 签名：相同参数 + Throwable 尾参数（必须完全匹配）
private StockResponse getStockFallback(Long productId, Throwable t) {
    log.warn("circuit open for inventory, productId={}, reason={}", productId, t.getMessage());
    return StockResponse.unavailable();  // 降级默认值
}
```

### `@Retry`（仅幂等查询）

```java
@Retry(name = "eagle-default", fallbackMethod = "findUserFallback")
@Transactional(readOnly = true)
public UserResponse findUser(Long userId) {
    return userClient.findById(userId);
}

private UserResponse findUserFallback(Long userId, Throwable t) {
    throw UserErrorCode.USER_SERVICE_UNAVAILABLE.toServiceException(t);
}
```

### `@TimeLimiter`（配合 CompletableFuture）

```java
@TimeLimiter(name = "eagle-default")
@CircuitBreaker(name = "eagle-default", fallbackMethod = "fallback")
public CompletableFuture<String> asyncCall() {
    return CompletableFuture.supplyAsync(() -> heavyOperation());
}

private CompletableFuture<String> fallback(Throwable t) {
    return CompletableFuture.completedFuture("降级默认值");
}
```

## 注解组合顺序

多注解叠加时执行顺序（从外到内）：

```
TimeLimiter → CircuitBreaker → Bulkhead → RateLimiter → Retry
```

```java
// ✅ 正确顺序
@TimeLimiter(name = "eagle-default")
@CircuitBreaker(name = "eagle-default", fallbackMethod = "fallback")
@Retry(name = "eagle-default")
public CompletableFuture<String> call() { ... }
```

## AI 专属实例（`eagle-ai-default`）

引入 `eagle-ai-starter` 时自动注册 `eagle-ai-default` 实例（慢调用阈值宽松为 10s）：

```java
@CircuitBreaker(name = "eagle-ai-default", fallbackMethod = "aiFallback")
@TimeLimiter(name = "eagle-ai-default")
public CompletableFuture<String> callAi(String prompt) { ... }
```

## Micrometer 指标

自动暴露至 Prometheus：

| 指标                                        | 说明     |
|-------------------------------------------|--------|
| `resilience4j_circuitbreaker_state`       | 熔断器状态  |
| `resilience4j_circuitbreaker_calls_total` | 调用次数分类 |
| `resilience4j_retry_calls_total`          | 重试统计   |
| `resilience4j_timelimiter_calls_total`    | 超时统计   |

告警阈值：熔断器 `OPEN` 状态持续 > 1 分钟 → 立即告警。

## 常见错误

- ❌ fallback 方法签名不含 `Throwable` 参数 → ✅ 必须是最后一个参数
- ❌ fallback 中再调同一外部依赖 → ✅ 禁止递归降级
- ❌ 非幂等操作加 `@Retry` → ✅ 仅幂等查询或业务上允许重复调用时才加
- ❌ `@TimeLimiter` 标注同步方法 → ✅ 仅对返回 `CompletableFuture` 的方法有效
- ❌ 事务方法内 `@Retry` → ✅ 事务已提交后重试无意义，抛出让调用方决策

## 关联规则

- `.claude/rules/04-data.md` — 超时配置
