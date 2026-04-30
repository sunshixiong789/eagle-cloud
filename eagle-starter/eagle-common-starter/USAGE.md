# eagle-common-starter — 核心基础设施（基类、异常、事件、锁接口）

## 何时使用

**所有业务模块的依赖底座**——其他 starter 都依赖此模块。提供：

- DDD 基类：`BaseAggregateRoot<T>`、`BaseEntity`
- 领域事件基类：`BaseEvent`
- 异常体系：`AppException` + `ErrorCode` 接口（4 个工厂方法）
- 通用错误码枚举：
  `CommonErrorCode / DataErrorCode / FileErrorCode / OperationErrorCode / ExternalErrorCode / LockErrorCode`
- 全局异常处理器：`GlobalExceptionHandler`
- 异步线程池：`@Bean("taskExecutor")`，启用 `@EnableAsync` + `@EnableScheduling`
- i18n 静态工具：`MessageSourceUtil`
- 业务指标：`BusinessMetrics`（Micrometer 封装）
- 分布式锁抽象：`DistributedLock`（实现由 redis / rocketmq starter 提供）
- 通用 DTO：`Result<T>`、`ErrorResult`、`EagleUser`
- 全链路压测上下文：`PressureTestContext` / `PressureTestFilter`

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-common-starter')
```

无需额外配置即生效。

## 核心 API

### DDD 基类

| 类                      | 说明                                                                                                      |
|------------------------|---------------------------------------------------------------------------------------------------------|
| `BaseAggregateRoot<T>` | 聚合根：`Long id` + 审计字段 + `@Version Long version` + `registerEvent()`（Spring Data `AbstractAggregateRoot`） |
| `BaseEntity`           | 子实体：同上但**无事件能力**，`@Getter @Setter`（聚合根级联管理）                                                             |
| `BaseEvent`            | 领域事件基类：`String eventId`(time-ordered UUID) + `LocalDateTime occurredOn` + `getEventType()`              |

**审计字段名**（注意——不是 createdAt/updatedAt）：

```java
private Long createBy;           // 创建人 ID
private Long updateBy;           // 更新人 ID
private LocalDateTime createTime;
private LocalDateTime updateTime;
```

### 异常体系

| 类                   | HTTP | 用途             |
|---------------------|------|----------------|
| `AppException`      | —    | 抽象基类           |
| `NotFoundException` | 404  | 资源不存在          |
| `ConflictException` | 409  | 业务冲突           |
| `DomainException`   | 400  | 领域规则违反         |
| `ServiceException`  | 500  | 基础设施 / 外部服务故障  |
| `ErrorCode`         | —    | 错误码接口（业务方实现枚举） |

### `ErrorCode` 工厂方法

```java
errorCode.toNotFoundException(args...)
errorCode.

toConflictException(args...)
errorCode.

toDomainException(args...)
errorCode.

toServiceException(args...)
errorCode.

toServiceException(Throwable cause)   // 异常链
```

### 分布式锁

```java
public interface DistributedLock {
    <T> T tryLock(String lockKey, long waitTime, long leaseTime, Supplier<T> supplier);

    default void tryLock(String lockKey, long waitTime, long leaseTime, Runnable runnable);

    default <T> T tryLock(String lockKey, Supplier<T> supplier);     // 默认 wait=3s, lease=30s

    default void tryLock(String lockKey, Runnable runnable);
}
```

参数单位是 **`long` 秒**，**不是 `Duration`**。失败抛 `ServiceException(LockErrorCode.LOCK_ACQUIRE_FAILED)`。

### 工具类

| 类                       | 用途                                                                                                                                                                                                                                                                                                               |
|-------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `MessageSourceUtil`（静态） | `getMessage(code [, args, defaultMessage, locale])`                                                                                                                                                                                                                                                              |
| `BusinessMetrics`       | `incrementOrderCreated(channel)` / `incrementPaymentSuccess(method)` / `incrementPaymentFailed(reason)` / `incrementInventoryDeducted(warehouseId)` / `incrementInventoryInsufficient()` / `incrementRateLimited(resource)` / `incrementCircuitBreaker(service)` / `startTimer()` + `recordDuration(op, sample)` |

### DTO

| 类           | 用途                                                                                              |
|-------------|-------------------------------------------------------------------------------------------------|
| `Result<T>` | `static success(data)` / `success()` / `error(msg)` / `error(code, msg)`，字段 `code/message/data` |
| `EagleUser` | 继承 Spring Security `User`，含 `id/name/deptId/deptName/phone`，Long ID 自动序列化为 String               |

## 最小示例

```java
// 聚合根
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseAggregateRoot<Order> {
    private String orderNo;

    public static Order create(String orderNo) {
        Order o = new Order();
        o.orderNo = orderNo;
        return o;
    }

    public void cancel() {
        registerEvent(new OrderCancelledEvent(getId(), orderNo));
    }
}

// 领域事件（继承 BaseEvent）
public class OrderCancelledEvent extends BaseEvent {
    private final Long orderId;
    private final String orderNo;

    public OrderCancelledEvent(Long orderId, String orderNo) {
        super();
        this.orderId = orderId;
        this.orderNo = orderNo;
    }
    // getters
}

// 抛异常
throw OrderErrorCode.ORDER_NOT_FOUND.

toNotFoundException();
throw OrderErrorCode.ORDER_TIMEOUT.

toDomainException();

// 分布式锁（Supplier）
return lock.

tryLock("order:"+orderId, 5L,30L,() ->{
        return orderService.

process(orderId);
});

// 默认参数（wait=3s, lease=30s）
        lock.

tryLock("user:update:"+userId, () ->userService.

update(userId));

// 业务指标
        metrics.

incrementOrderCreated("app");

Timer.Sample sample = metrics.startTimer();
try{
        inventory.

deduct(...);
    metrics.

recordDuration("inventory.deduct",sample);
}catch(
Exception e){
        metrics.

incrementInventoryInsufficient();
    throw e;
}
```

## 配置项

| key                                     | 类型      | 默认                    | 说明                                  |
|-----------------------------------------|---------|-----------------------|-------------------------------------|
| `eagle.lock.type`                       | enum    | `REDIS`               | 锁实现：`REDIS` / `MQ`                  |
| `eagle.lock.granularity`                | enum    | `PER_KEY`             | MQ 模式锁粒度：`PER_KEY` / `SHARED_TOPIC` |
| `eagle.lock.topic-prefix`               | String  | `eagle-lock-`         | MQ PER_KEY 模式 topic 前缀              |
| `eagle.lock.shared-topic`               | String  | `eagle-lock-shared`   | MQ SHARED_TOPIC 模式共用 topic          |
| `eagle.lock.invisible-duration-seconds` | int     | `30`                  | MQ 锁不可见时长（≈最大持锁）                    |
| `eagle.lock.consumer-group`             | String  | `eagle-lock-consumer` | MQ 锁消费者组                            |
| `eagle.lock.keys`                       | List    | `[]`                  | MQ 锁需要管理的 lockKey 列表                |
| `eagle.lock.auto-init-token`            | boolean | `false`               | 启动时自动发布 token（生产建议 false）           |
| `eagle.lock.poll-interval-seconds`      | int     | `1`                   | MQ 锁轮询超时                            |

## 线程池配置

`@Bean("taskExecutor")` 由 `AsyncConfig` 注册：

| 参数            | 值                                                    |
|---------------|------------------------------------------------------|
| corePoolSize  | CPU 核心数                                              |
| maxPoolSize   | CPU × 2                                              |
| queueCapacity | 200                                                  |
| keepAlive     | 60s                                                  |
| 拒绝策略          | `CallerRunsPolicy`（背压）                               |
| 优雅关闭          | `waitForTasksToComplete=true`，`awaitTermination=30s` |

`@Async` 默认使用此池；指定其他池用 `@Async("xxx")`。

## 常见错误

- ❌ JPA 实体加 `@Data` / `@Builder` → ✅ `@Getter` + `@NoArgsConstructor(PROTECTED)`
- ❌ 应用服务手动调 publishEvent → ✅ 聚合根 `registerEvent()` + Spring Data 自动收集
- ❌ 直接抛 `IllegalArgumentException` → ✅ `errorCode.toXxxException()`
- ❌ 自定义新 Exception 子类 → ✅ 用现有 4 类
- ❌ DistributedLock 传 `Duration` → ✅ 传 `long` 秒
- ❌ 引用 Bean 名 `eagleTaskExecutor` → ✅ 实际名为 `taskExecutor`
- ❌ 字段写 `createdAt/updatedAt` → ✅ `createTime/updateTime`

## 关联规则

- `.claude/rules/03-architecture.md` — DDD 分层
- `.claude/rules/07-exception.md` — 异常体系
- `.claude/rules/08-concurrency.md` — 事件 + 事务
- `.claude/rules/02-code-style.md` — 基类使用规则
