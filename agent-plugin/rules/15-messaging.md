# 消息队列规范（RocketMQ）

技术栈：`eagle-rocketmq-starter` 提供 `DomainEventPublisher`（同步发布）、`TransactionalEventPublisher`（事务消息）、
`AbstractRocketMqListener`（统一消费骨架）、`AbstractDlqListener`（死信处理）。

## 何时使用 MQ

| 场景         | 选型                               |
|------------|----------------------------------|
| 同模块内、同事务边界 | Spring `ApplicationEvent`（不走 MQ） |
| 跨模块、最终一致性  | 领域事件 → MQ（事务消息）                  |
| 跨服务、异步通知   | MQ（事务消息或同步发送 + 重试）               |
| 削峰、批处理、广播  | MQ                               |
| 强一致性 RPC   | Feign，**不**走 MQ                  |

## Topic / Tag 命名

**环境隔离靠不同 MQ 集群（或同集群不同 namespace）**，topic 名本身**不**带 env 前缀——
dev / test / staging / prod 各自连接独立的 RocketMQ 实例，部署期通过 Nacos 配置切换。
这样能避免"测试消息漏到生产" / "topic 改名要全环境联动"等问题。

格式：`eagle.{service}.{domain}.events`（点号分隔，全小写）：

```
eagle.auth.account.events       # auth-service 账号生命周期事件
eagle.order.order.events        # order-service 订单生命周期事件
eagle.payment.payment.events    # payment-service 支付事件
eagle.system.user.events        # system-service 用户域事件
```

- `eagle` — 平台前缀，与其他系统区分
- `service` — 发布方服务名（**必须**带，避免不同服务相同 domain 撞名）
- `domain` — 业务域（`account / order / payment / user`）
- `events` — 固定后缀，标识这是事件 topic

**Tag** 用过去时动词区分子事件类型：

```
topic: eagle.auth.account.events
  tag: registered      # AccountRegisteredIntegrationEvent
  tag: deleted         # AccountDeletedIntegrationEvent
```

一个聚合根的全部生命周期事件应**复用同一 topic + 不同 tag**——便于消费方按需订阅
（`tagsExpression = "registered || deleted"`）、统一监控同一聚合的吞吐与堆积。

**禁止**：
- topic 名带 `{env}_` 前缀（环境隔离应在基础设施层）
- topic 名不带 `service` 段（不同服务的 `order` 域必然撞名）
- 多个**不相关聚合**的事件复用同一 topic（独立扩缩容受限）

## 消息发布

```java
// ✅ 普通同步发布（用于不需要本地事务的场景）
@RequiredArgsConstructor
public class NotificationApplicationService {
    private final DomainEventPublisher publisher;

    public void notifyAdmin(AdminAlert event) {
        publisher.publish("eagle.notification.admin.events", "alert", event);
    }
}

// ✅ 事务消息（推荐用于聚合根写库 + 发消息的强保证场景）
publisher.

publishInTransaction(
    "eagle.order.order.events",
    "created",
    event,
    () ->orderRepository.

save(order)   // 本地事务回调
);
```

- **禁止**直接使用 `RocketMQTemplate` 裸调，必须走 starter 抽象
- **禁止**异步发送（`sendOneway`）业务关键消息
- 单条消息体 ≤ 4MB；超过的拆分或走 OSS（消息体只放 URL）

## 领域事件 → MQ 转换

跨服务事件遵循"内部事件 + 外部集成事件"两层模型，**两侧各自在自己模块内独立声明，不共享 Java 类**：

```java
// === 生产方模块: order/infrastructure/messaging/ ===

// 1) 内部领域事件（聚合根注册，不出域）
public record OrderCreatedEvent(Long orderId, ...) {
}

// 2) 生产方的 RocketMQ 发布载荷（紧贴 Producer，infra 级，后缀 IntegrationEvent）
@Getter @NoArgsConstructor @AllArgsConstructor
public class OrderCreatedIntegrationEvent extends BaseEvent {
    private Long orderId;
    private String orderNo;
    private BigDecimal amount;
    // ...
}

// 3) 处理器转换并发布（topic + tag）
@Async
@TransactionalEventListener(phase = AFTER_COMMIT)
public void onOrderCreated(OrderCreatedEvent e) {
    publisher.publish("eagle.order.order.events", "created",
            new OrderCreatedIntegrationEvent(e.orderId(), e.orderNo(), ...));
}
```

```java
// === 消费方模块: stock/infrastructure/messaging/ ===
// 独立声明本地反序列化 DTO，可仅取需要的字段子集

@Getter @NoArgsConstructor
public class OrderCreatedMessage extends BaseEvent {
    private Long orderId;
    private String orderNo;
    // amount 等本模块用不到的字段可以不声明
}
```

**核心原则:**

- **不**把集成事件类放在跨模块的"共享 messaging 包"里，**不**让消费方 `import` 生产方的类。
- 两侧的耦合面 = **JSON 字段名 + 类型**（消费方对未知字段宽容，缺失非必需字段宽容）。
- 同一事件被 N 个消费方订阅 → 各消费方各写一份 `XxxMessage`，按各自需要裁剪字段。
- 字段演进无需"跨服务联动改类"：生产方加字段，旧消费方继续工作；消费方加字段，等生产方真的发了再读。

**新增字段**必须可向后兼容（默认值 / Optional）。**禁止**删除或重命名已发布字段——靠新字段 + 灰度迁移。

## 消费者（继承 AbstractRocketMqListener）

`eagle-rocketmq-starter` 使用 RocketMQ 5.x **原生客户端 API**（`PushConsumer` + `MessageView`），消费者**不使用**
`@RocketMQMessageListener` 注解，而是继承 `AbstractRocketMqListener<T>` 实现 3 个抽象方法：

```java
// ✅ 标准消费者：泛型指向**本模块**声明的 OrderCreatedMessage（不是生产方的 IntegrationEvent）
@Component
public class OrderCreatedConsumer
        extends AbstractRocketMqListener<OrderCreatedMessage> {

    private final StockApplicationService stockService;
    private final IdempotencyChecker idempotency;

    public OrderCreatedConsumer(RocketMqProperties props,
                                StockApplicationService stockService,
                                IdempotencyChecker idempotency) {
        super(props);
        this.stockService = stockService;
        this.idempotency = idempotency;
    }

    @Override
    protected String getTopic() {
        return "eagle.order.order.events";
    }

    /** 仅订阅 created tag（同 topic 其他 tag 的事件由别的 consumer 处理）。 */
    @Override
    protected String getTagExpression() {
        return "created";
    }

    @Override
    protected Class<OrderCreatedMessage> getEventClass() {
        return OrderCreatedMessage.class;
    }

    @Override
    protected void handle(OrderCreatedMessage event) {
        // 必须幂等！用 event.getEventId() 去重
        if (!idempotency.firstTime(event.getEventId())) return;
        stockService.lockStock(event.getOrderId(), event.getItems());
    }

    // 可选覆盖：自定义消费者组（默认走 eagle.rocketmq.consumer-group）
    @Override
    protected String getConsumerGroup() {
        return "stock_service_order_created";
    }
}
```

**ConsumerGroup 命名**：`{消费方服务}_{topic 简写}`，确保独立扩缩容。

## 幂等（必须）

消费者**必须**实现幂等，防止重复消费。**推荐做法是把幂等埋进业务表本身**，避免引入独立 inbox 表 —— 同一份"是否处理过"的真相只存一处(业务表)，且自动复用业务事务边界。

### 主调：业务表自带幂等（推荐）

**Mode A — 创建型(入账、消息、关系映射)**

```java
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "source_type", "source_ref_id"}))

// ApplicationService:
try {
    repo.save(Entity.create(userId, sourceType, sourceRefId, ...));
} catch (DataIntegrityViolationException e) {
    log.info("idempotent skip");
    return;
}
// 其它必要写入(同事务)...
```

**Mode B — 状态机推进**

```java
int updated = repo.updateStatusIfCurrentlyEquals(id, OLD, NEW);
if (updated == 0) return;   // 已转换过
```

**Mode C — 累加型(计数器、累计金额、会员天数)**

直接 `counter += 1` 无法幂等。新建独立**事实表**挂幂等键 + 同事务更新累加器：

```java
// 1) FactLog 实体：(user_id, source, source_ref_id) UNIQUE
// 2) ApplicationService:
try {
    factLogRepo.save(FactLog.create(userId, source, sourceRefId, ...));
} catch (DataIntegrityViolationException e) {
    return;
}
// 同事务内更新累加器
aggregate.accumulate(...);
aggregateRepo.save(aggregate);
```

**Consumer 编码约定:** 幂等下沉到 ApplicationService 后，Consumer/DlqListener **不再**写 `idempotency.firstTime(...)`：

```java
@Override
protected void handle(SomeMessage event) {
    appService.doSomething(event.getUserId(), event.getSourceRefId(), ...);
}
```

### 罕见兜底：Redis SETNX

仅在事件**真的没有自然业务键**(纯事实通知)时使用。

```java
Boolean first = redisTemplate.opsForValue()
        .setIfAbsent("eagle:mq:idempotent:" + event.getEventId(), "1", Duration.ofDays(1));
if (Boolean.FALSE.equals(first)) return;
```

幂等 Key **必须**用消息自带的 `eventId`（`BaseEvent.eventId`），**不**用 MQ 自动生成的 `MsgId`（重投递会变）。

### 反模式：独立 inbox 表

旧做法是建一张 `xxx_mq_inbox(event_id, consumer_group)` 表挡重复。**不推荐** —— 两份真相(inbox + 业务表)、跨表事务复杂、Consumer 多一层 boilerplate。仅在业务表方案不可行时考虑(几乎不会发生)。

## 死信处理（DLQ）

RocketMQ 默认重试 16 次后进入 `%DLQ%{ConsumerGroup}` 队列。继承 `AbstractDlqListener` 处理（实现 3 个方法：原消费者组、事件类、死信处理逻辑）：

```java

@Component
public class OrderCreatedDlqListener
        extends AbstractDlqListener<OrderCreatedMessage> {

    private final AlarmService alarmService;
    private final DeadLetterRepository deadLetterRepository;

    public OrderCreatedDlqListener(RocketMqProperties props,
                                   AlarmService alarmService,
                                   DeadLetterRepository deadLetterRepository) {
        super(props);
        this.alarmService = alarmService;
        this.deadLetterRepository = deadLetterRepository;
    }

    @Override
    protected String getOriginalConsumerGroup() {
        return "stock_service_order_created";   // 与原消费者一致；DLQ Topic 自动 = %DLQ%xxx
    }

    @Override
    protected Class<OrderCreatedMessage> getEventClass() {
        return OrderCreatedMessage.class;
    }

    @Override
    protected void handleDeadLetter(OrderCreatedMessage event, int totalAttempts) {
        log.error("DLQ event arrived, eventId={}, attempts={}", event.getEventId(), totalAttempts);
        deadLetterRepository.save(DeadLetterRecord.of(event, totalAttempts));
        alarmService.notifyOps("order_created_dlq", event);
    }
}
```

死信**必须**告警（钉钉/企微/邮件），**禁止**静默吞掉。

## 顺序消息

仅在严格需要时使用（同一聚合根的状态机事件）：

```java
// ✅ 同一 orderId 的消息按序消费（messageGroup 相同则保序）
publisher.publishOrdered(topic, event, orderId.toString());
```

性能代价显著（消费并发降至 1），优先用"事件版本号 + 幂等"替代。

## 重试与超时

- 生产者：内部 Retry 3 次（500ms / 1s / 2s 退避）
- 消费者：失败抛 `RuntimeException` 触发 RocketMQ 重投递；**不**自己 try-catch 吞掉
- 消费超时：方法内 `@Transactional` 不超过 5s；长任务异步交给 `@Async` 池

## 监控

每个 Topic 必须监控：堆积量、消费延迟、失败率、DLQ 数。指标接入 Grafana。

## 禁止清单

- 禁止在消息体中放敏感数据（密码、身份证、Token）— 即使临时存在也不允许
- 禁止用 MQ 做"分布式锁"或"事务"
- 禁止 `@Transactional` 内同步等待 MQ 发送结果（事务持有时间过长）
- 禁止跨服务直接消费对方"内部领域事件"，必须走"集成事件"
- 禁止生产环境 ConsumerGroup 与开发环境共用
