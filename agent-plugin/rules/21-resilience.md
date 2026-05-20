# 容错与弹性规范（Resilience）

技术栈：`eagle-resilience-starter`（基于 Resilience4J 2.2.0），提供熔断器、重试、超时三大能力。

## 何时使用容错机制

| 场景                        | 选型                           |
|---------------------------|------------------------------|
| 外部服务调用（Feign、HTTP Client） | CircuitBreaker + TimeLimiter |
| 数据库 / Redis 瞬时故障          | Retry（指数退避）                  |
| 批处理 / 非核心后台任务             | Retry + Fallback             |
| 核心支付 / 下单流程               | 不使用容错（快速失败 + 告警）             |
| 读操作（查询接口）                 | CircuitBreaker + Fallback    |

**核心原则**：容错不能替代正确设计——它只是"最后防线"。优先修复根本原因，而非无限添加重试。

## 命名实例

`eagle-resilience-starter` 自动注册 `eagle-default` 实例，使用约定配置：

```java
// ✅ 直接引用默认实例
@CircuitBreaker(name = "eagle-default", fallbackMethod = "fallback")
public String callInventory(Long productId) {
    return inventoryClient.getStock(productId);
}

// ✅ 自定义实例（更细粒度控制）
@CircuitBreaker(name = "payment-gateway")
public PaymentResult pay(PaymentRequest request) { ... }
```

自定义实例通过 `application.yml` 的 Resilience4J 原生配置覆盖：

```yaml
resilience4j:
  circuitbreaker:
    instances:
      payment-gateway:
        failure-rate-threshold: 30
        wait-duration-in-open-state: 60s
```

## CircuitBreaker（熔断器）

```java
// ✅ 完整示例：熔断 + 降级
@Service
@RequiredArgsConstructor
public class InventoryApplicationService {

    private final InventoryClient inventoryClient;

    @CircuitBreaker(name = "eagle-default", fallbackMethod = "getStockFallback")
    @TimeLimiter(name = "eagle-default")
    public CompletableFuture<Integer> getStock(Long productId) {
        return CompletableFuture.supplyAsync(() -> inventoryClient.getStock(productId));
    }

    // fallback 方法签名：参数列表 + Throwable
    private CompletableFuture<Integer> getStockFallback(Long productId, Throwable t) {
        log.warn("circuit open for inventory, productId={}, reason={}", productId, t.getMessage());
        return CompletableFuture.completedFuture(0);  // 降级默认值
    }
}
```

**Fallback 规范：**

- fallback 方法**必须**有与原方法相同的参数 + `Throwable` 尾参数
- fallback 中**禁止**再次调用同一外部依赖（无限循环）
- fallback 返回降级默认值或抛出 `ServiceException`（不吞掉）

## Retry（重试）

```java
// ✅ 非幂等操作禁止 Retry
// ✅ 幂等查询适合 Retry
@Retry(name = "eagle-default", fallbackMethod = "findUserFallback")
public UserResponse findUser(Long userId) {
    return userClient.findById(userId);
}

private UserResponse findUserFallback(Long userId, Throwable t) {
    throw UserErrorCode.USER_SERVICE_UNAVAILABLE.toServiceException(t);
}
```

**重试规范：**

- **禁止**对非幂等操作（POST/PUT/DELETE）使用 `@Retry`（会造成重复操作）
- 重试 + 指数退避（默认 2x），防止雪崩冲击下游
- 重试次数 ≤ 3，单次最大等待 ≤ 10s（`eagle.resilience.retry.*`）
- `4xx` 客户端错误**不重试**（配置 `ignoreExceptions`）

```java
// ✅ 忽略客户端错误，只重试服务端/网络错误
@Retry(name = "eagle-default")
@CircuitBreaker(name = "eagle-default")
public String call() { ... }

// application.yml 排除 4xx
resilience4j:
  retry:
    instances:
      eagle-default:
        ignore-exceptions:
          - com.eagle.common.exception.DomainException
          - com.eagle.common.exception.NotFoundException
```

## TimeLimiter（超时控制）

```java
// ✅ 配合 CompletableFuture 使用
@TimeLimiter(name = "eagle-default")
public CompletableFuture<String> asyncCall() {
    return CompletableFuture.supplyAsync(() -> heavyOperation());
}
```

- 默认超时 5s（`eagle.resilience.time-limiter.timeout-duration`）
- **禁止**在同步方法（非 Future）上使用 `@TimeLimiter`（无效）
- 超时触发 `TimeoutException`，应在 fallback 中处理

## 注解组合顺序

多个注解组合时，执行顺序（从外到内）：
`TimeLimiter → CircuitBreaker → Bulkhead → RateLimiter → Retry`

```java
// ✅ 正确组合顺序
@TimeLimiter(name = "eagle-default")
@CircuitBreaker(name = "eagle-default", fallbackMethod = "fallback")
@Retry(name = "eagle-default")
public CompletableFuture<String> call() { ... }
```

## 配置参考

```yaml
eagle:
  resilience:
    enabled: true
    circuit-breaker:
      failure-rate-threshold: 50        # 错误率阈值（%）
      slow-call-rate-threshold: 80      # 慢调用阈值（%）
      wait-duration-in-open-state: 30s  # 开路等待时间
      sliding-window-size: 100
      minimum-number-of-calls: 10
    retry:
      max-attempts: 3
      wait-duration: 500ms
      exponential-backoff-multiplier: 2.0
      exponential-max-wait-duration: 10s
    time-limiter:
      timeout-duration: 5s
      cancel-running-future: true
```

## 监控指标

`eagle-resilience-starter` 集成 Micrometer，自动暴露：

| 指标                                        | 含义      |
|-------------------------------------------|---------|
| `resilience4j_circuitbreaker_state`       | 熔断器状态   |
| `resilience4j_circuitbreaker_calls_total` | 调用次数/结果 |
| `resilience4j_retry_calls_total`          | 重试次数    |
| `resilience4j_timelimiter_calls_total`    | 超时统计    |

告警阈值：熔断器状态为 OPEN 持续 > 1 分钟 → 立即告警。

## 禁止清单

- 禁止对非幂等操作使用 `@Retry`
- 禁止 fallback 中再次调用同一下游（无限递归）
- 禁止 fallback 静默吞掉异常（至少 `log.warn`）
- 禁止对核心支付 / 下单流程用 CircuitBreaker 掩盖问题（应直接告警）
- 禁止 `@TimeLimiter` 标注同步方法（无效且会产生误导）
- 禁止在事务方法内使用 `@Retry`（事务已提交的数据无法回滚）
