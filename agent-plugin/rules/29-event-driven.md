# 事件驱动架构规范（Event-Driven）

本规则覆盖领域事件（Domain Event）与集成事件（Integration Event）的设计、发布与消费规范。与 `08-concurrency.md`（事务）、
`15-messaging.md`（MQ 实现）互补。

## 事件分类

| 类型              | 作用域              | 传输载体                      | 包位置                      |
|-----------------|------------------|---------------------------|--------------------------|
| **领域事件**        | 单域内（聚合根 → 同域处理器） | Spring `ApplicationEvent` | `{module}/domain/event/` |
| **集成事件**        | 跨域 / 跨服务         | Spring Event（单体）→ MQ（微服务） | `{module}/domain/event/` |
| **命令（Command）** | 外部意图输入           | HTTP / MQ                 | `{module}/application/`  |

**禁止**跨域直接消费对方内部领域事件——必须发布为集成事件。

## 领域事件设计

```java
// ✅ 领域事件：不可变 record，使用 UUID v7（时间有序）eventId
public record OrderPaidEvent(
        String eventId,           // BaseEvent 继承字段
        LocalDateTime occurredOn, // 事件发生时间
        Long orderId,
        BigDecimal amount,
        String channel
) implements BaseEvent { }

// ❌ 禁止：事件持有完整聚合根对象（跨域耦合 + 序列化风险）
public record OrderPaidEvent(Order order) { }
```

**领域事件规范：**

- 类名：`{聚合根}{动作}Event`，过去时（`Paid`、`Created`、`Cancelled`）
- 字段：只含**跨域消费所需**的最小字段（id + 关键业务属性）
- 不可变（`record` 或 `@Value`），无 setter
- `eventId` 使用 `BaseEvent.eventId`（UUID v7 时间有序，天然可去重）

## 发布时机

```java
// ✅ 聚合根业务方法内注册，@PostPersist 后自动发布
public void pay(BigDecimal amount, String channel) {
    this.status = OrderStatus.PAID;
    this.paidAmount = amount;
    registerEvent(new OrderPaidEvent(getId(), amount, channel, ...));
}

// ✅ 创建型事件（ID 尚未分配）用 @PostPersist 延迟发布（详见 03-architecture.md）
@PostPersist
private void onPostPersist() {
    if (profileHints != null) {
        registerEvent(new OrderCreatedEvent(getId(), orderNo, ...));
        profileHints = null;
    }
}
```

**禁止**在应用服务层手动调用 `publishEvent()`——事件应由聚合根自己发出，保证业务规则与事件发布的原子性。

## 事件处理器

### 同域处理器（AFTER_COMMIT 异步）

```java
// ✅ 同域：事务提交后异步处理，失败不影响主流程
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onOrderPaid(OrderPaidEvent event) {
    notificationService.sendPaymentConfirmation(event.orderId());
}
```

### 跨域处理器（独立事务 + 转为集成事件）

```java
// ✅ 跨域：独立事务，将领域事件转换为集成事件发往 MQ
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void onOrderPaid(OrderPaidEvent event) {
    // 领域事件 → 集成事件 → MQ
    publisher.publish("prod_order_paid",
            new OrderPaidIntegrationEvent(event.orderId(), event.amount(), event.channel()));
}
```

**处理器规范：**

- `@Async`：异步，不阻塞主事务
- `@TransactionalEventListener(phase = AFTER_COMMIT)`：主事务提交后才触发
- 跨域额外加 `@Transactional(propagation = REQUIRES_NEW)`：独立事务，避免级联失败

## 集成事件契约

集成事件是**跨服务的接口契约**，修改必须向后兼容：

```java
// ✅ 集成事件：稳定字段 + 版本字段
public record OrderPaidIntegrationEvent(
        String eventId,
        String eventVersion,    // "1.0"，破坏性变更升版本
        LocalDateTime occurredOn,
        Long orderId,
        String orderNo,
        BigDecimal amount,
        String paymentChannel
) { }

// ✅ 新增字段必须有默认值（向后兼容）
// ❌ 禁止删除或重命名已发布的集成事件字段
```

**版本管理：**

- 新增字段（Optional / 有默认值）→ 保持版本，消费方按需读取
- 字段类型变更 / 删除 → 升版本（`eventVersion: "2.0"`），旧版本消费方继续消费 v1
- 版本过渡期：同时发布两个版本，至少维护 3 个月再下线旧版本

## Saga / 编排式事件流

长流程跨多个聚合根，使用 Saga 编排：

```
Order.created → lock inventory → deduct wallet → confirm order
     ↓ 失败补偿
Order.cancel ← release inventory ← refund wallet
```

```java
// ✅ 编排式 Saga：统一 SagaOrchestrator 控制步骤
@Component
public class CreateOrderSaga {

    @SagaStart
    public void start(OrderCreatedEvent event) {
        sagaManager.startSaga("create-order", event.orderId())
                .step("lock-stock", () -> inventoryPort.lockStock(...))
                .step("deduct-wallet", () -> walletPort.deduct(...))
                .onError("compensate-stock", () -> inventoryPort.releaseStock(...))
                .execute();
    }
}
```

**禁止**用链式 `@EventListener` 实现 Saga（难以追踪状态 + 补偿无法保证）。

## 事件溯源（Event Sourcing）

仅在**需要完整历史审计**的聚合（如账务流水、合同变更）中使用：

```java
// ✅ Event Sourcing 聚合：通过事件重建状态
public class AccountLedger {
    private BigDecimal balance = BigDecimal.ZERO;

    public void apply(DepositEvent event) {
        this.balance = balance.add(event.amount());
    }

    public void apply(WithdrawalEvent event) {
        this.balance = balance.subtract(event.amount());
    }
}
```

- Event Store 使用独立表（`t_domain_event_store`）
- 快照（Snapshot）：每 100 个事件生成一次，避免重建慢
- **禁止**全业务使用 Event Sourcing（复杂度高，仅少数核心域适用）

## 幂等性

事件处理器**必须幂等**（MQ 保证至少一次投递）：

```java
// ✅ 用 eventId 去重（唯一约束 or Redis SETNX）
@Async
@TransactionalEventListener(phase = AFTER_COMMIT)
@Transactional(propagation = REQUIRES_NEW)
public void onOrderPaid(OrderPaidIntegrationEvent event) {
    if (idempotencyChecker.isDuplicate(event.getEventId())) return;
    // 处理...
}
```

幂等 Key 使用 `eventId`（不使用 MQ 自带的 `MsgId`，重投递会变）。

## 事件顺序

大多数场景**不保证顺序**，通过幂等 + 版本号处理乱序：

```java
// ✅ 乐观锁版本号防止旧事件覆盖新状态
@Modifying
@Query("UPDATE Order o SET o.status = :status, o.version = :newVersion " +
       "WHERE o.id = :id AND o.version < :newVersion")
int updateStatusIfNewer(Long id, OrderStatus status, long newVersion);
```

若**必须顺序消费**（如账户状态机），使用 RocketMQ 顺序消息（`messageGroup = orderId`）。

## 死信与补偿

事件处理失败后进入 DLQ，通过补偿任务收敛：

```java
// ✅ 每小时扫描"处理中"超时事件，触发补偿
@XxlJob("eventCompensationJob")
public void compensate() {
    List<SagaState> stuckSagas = sagaRepository
            .findByStatusAndUpdatedBefore(SagaStatus.IN_PROGRESS, Instant.now().minus(1, HOURS));
    stuckSagas.forEach(saga -> sagaManager.compensate(saga.getId()));
}
```

## 禁止清单

- 禁止在聚合根外（应用服务、Controller）手动 `publishEvent()`
- 禁止跨域直接消费内部领域事件（必须通过集成事件）
- 禁止集成事件持有完整聚合根对象
- 禁止事件处理器内再次触发同一事件（无限循环）
- 禁止删除或修改已发布的集成事件字段（向后兼容）
- 禁止用链式 `@EventListener` 实现多步骤 Saga（用编排器替代）
- 禁止事件处理器非幂等（MQ 至少一次投递必须幂等）
- 禁止在 `@Transactional` 中使用 `ApplicationEventPublisher.publishEvent()`（应用 `registerEvent()`，让 Spring Data 在
  `save()` 后自动发布）
