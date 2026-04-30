# eagle-common-starter — 核心基础设施（基类、异常、事件、锁接口）

## 何时使用

**所有业务模块都应依赖**——这是其他 starter 的依赖底座，提供以下能力：

- DDD 基类：`BaseAggregateRoot`、`BaseEntity`
- 异常体系：`AppException` + `ErrorCode` 枚举工厂
- 领域事件基类：`BaseEvent`
- 全局异常处理器：`GlobalExceptionHandler`（`@ControllerAdvice`）
- i18n 消息工具：`MessageSourceUtil`
- 异步线程池：`@Bean("eagleTaskExecutor")`
- 分布式锁抽象：`DistributedLock`（实现由 `eagle-redis-starter` / `eagle-rocketmq-starter` 提供）
- 压测上下文：`PressureTestContext`、`PressureTestFilter`
- 业务指标：`BusinessMetrics`（Micrometer）
- 通用 DTO：`Result<T>`、`ErrorResult`、`EagleUser`

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-common-starter')
```

无需任何配置，自动装配。

## 核心 API

### DDD 基类

| 类 | 用途 |
|---|---|
| `BaseAggregateRoot<T>` | 聚合根基类：`@Id IDENTITY` + 审计字段 + `@Version` + `registerEvent()` |
| `BaseEntity` | 子实体基类：审计字段 + `@Version`（无事件能力）|
| `BaseEvent` | 领域事件基类：time-ordered UUID `eventId` + `occurredOn` |

### 异常体系

| 类 | HTTP 状态 | 用途 |
|---|---|---|
| `AppException` | — | 抽象基类，所有业务异常的父类 |
| `NotFoundException` | 404 | 资源不存在 |
| `ConflictException` | 409 | 业务冲突 |
| `DomainException` | 400 | 领域规则违反 |
| `ServiceException` | 500 | 基础设施 / 外部服务异常 |
| `ErrorCode` | — | 错误码接口；各域定义枚举实现 |

通用 ErrorCode 枚举：`CommonErrorCode`、`DataErrorCode`、`FileErrorCode`、`OperationErrorCode`、`ExternalErrorCode`、`LockErrorCode`。

### 分布式锁

| 类 / 接口 | 用途 |
|---|---|
| `DistributedLock` | 锁抽象接口（`tryLock` / `unlock` / `executeWithLock`）|
| `LockProperties` | `eagle.lock.*` 配置 |

实现由 `eagle-redis-starter`（Redisson 实现）或 `eagle-rocketmq-starter` 提供，注入 `DistributedLock` 即可。

### 工具类

| 类 | 用途 |
|---|---|
| `MessageSourceUtil` | i18n 消息查询（封装 `MessageSource`） |
| `BusinessMetrics` | 业务指标埋点（Micrometer Counter / Timer） |

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
        registerEvent(new OrderCancelledEvent(getId()));
    }
}

// 领域事件
public record OrderCancelledEvent(Long orderId) extends BaseEvent { }

// 抛异常
throw OrderErrorCode.ORDER_NOT_FOUND.toNotFoundException();
throw OrderErrorCode.ORDER_TIMEOUT.toDomainException();

// 分布式锁
@RequiredArgsConstructor
public class OrderService {
    private final DistributedLock lock;

    public void process(Long orderId) {
        lock.executeWithLock("order:" + orderId, Duration.ofSeconds(10), () -> {
            // 业务逻辑
        });
    }
}
```

## 配置项

| key | 类型 | 默认 | 说明 |
|---|---|---|---|
| `eagle.lock.enabled` | boolean | `true` | 是否启用分布式锁 |
| `eagle.lock.default-wait-seconds` | int | `5` | 默认等待时间 |
| `eagle.lock.default-lease-seconds` | int | `30` | 默认持锁时间 |

## 常见错误

- ❌ JPA 实体加 `@Data` / `@Builder` → ✅ `@Getter` + `@NoArgsConstructor(PROTECTED)`，详见 `02-code-style.md`
- ❌ 应用服务手动调用事件发布 → ✅ 聚合根 `registerEvent()` + `@PostPersist`，详见 `03-architecture.md`
- ❌ 直接抛 `IllegalArgumentException` → ✅ 用 ErrorCode 工厂方法，详见 `07-exception.md`
- ❌ 自定义新异常子类 → ✅ 现有 4 类已覆盖，不再扩展

## 关联规则

- `.claude/rules/03-architecture.md` — DDD 分层
- `.claude/rules/07-exception.md` — 异常体系
- `.claude/rules/08-concurrency.md` — 事件 + 事务
- `.claude/rules/02-code-style.md` — 基类使用规则
