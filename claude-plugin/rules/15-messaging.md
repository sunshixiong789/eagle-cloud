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

格式：`{env}_{domain}_{event}`，全小写，下划线分隔：

```
prod_order_created
prod_order_paid
prod_account_registered
test_user_updated
```

- `env`：`dev / test / staging / prod`
- `domain`：业务域（`order / account / user / payment`）
- `event`：事件名（过去时动词，如 `created / paid / cancelled`）
- Tag 用于子分类：`prod_order_status:paid` / `prod_order_status:cancelled`

**禁止**多个不相关事件复用同一 Topic（无法独立监控/限流/扩缩容）。

## 消息发布

```java
// ✅ 普通同步发布（用于不需要本地事务的场景）
@RequiredArgsConstructor
public class NotificationApplicationService {
    private final DomainEventPublisher publisher;

    public void notifyAdmin(AdminAlert event) {
        publisher.publish("prod_admin_alert", event);
    }
}

// ✅ 事务消息（推荐用于聚合根写库 + 发消息的强保证场景）
publisher.

publishInTransaction(
    "prod_order_created",
    event,
    () ->orderRepository.

save(order)   // 本地事务回调
);
```

- **禁止**直接使用 `RocketMQTemplate` 裸调，必须走 starter 抽象
- **禁止**异步发送（`sendOneway`）业务关键消息
- 单条消息体 ≤ 4MB；超过的拆分或走 OSS（消息体只放 URL）

## 领域事件 → MQ 转换

跨服务事件遵循"内部事件 + 外部集成事件"两层模型：

```java
// 内部领域事件（聚合根注册，不出域）
public record OrderCreatedEvent(Long orderId, ...) {
}

// 处理器转换为外部集成事件并发到 MQ
@Async
@TransactionalEventListener(phase = AFTER_COMMIT)
public void onOrderCreated(OrderCreatedEvent e) {
    publisher.publish("prod_order_created",
            new OrderCreatedIntegrationEvent(e.orderId(), e.orderNo(), ...));
}
```

**集成事件 schema 必须稳定**——它是跨服务契约。新增字段必须可向后兼容（默认值 / Optional）。

## 消费者（继承 AbstractRocketMqListener）

`eagle-rocketmq-starter` 使用 RocketMQ 5.x **原生客户端 API**（`PushConsumer` + `MessageView`），消费者**不使用**
`@RocketMQMessageListener` 注解，而是继承 `AbstractRocketMqListener<T>` 实现 3 个抽象方法：

```java
// ✅ 标准消费者：构造器须把 RocketMqProperties 透传给 super
@Component
public class OrderCreatedConsumer
        extends AbstractRocketMqListener<OrderCreatedIntegrationEvent> {

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
        return "prod_order_created";
    }

    @Override
    protected Class<OrderCreatedIntegrationEvent> getEventClass() {
        return OrderCreatedIntegrationEvent.class;
    }

    @Override
    protected void handle(OrderCreatedIntegrationEvent event) {
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

消费者**必须**实现幂等，防止重复消费：

```java
// ✅ 方案一：唯一约束（推荐，DB 强一致）
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "event_id"))

try{
        inboxRepository.

save(new InboxRecord(event.eventId()));

process(event);
}catch(
DataIntegrityViolationException ignore){
        // 重复消息直接跳过
        }

// ✅ 方案二：Redis SETNX（高吞吐场景）
Boolean first = redisTemplate.opsForValue()
        .setIfAbsent("eagle:mq:idempotent:" + event.eventId(), "1", Duration.ofDays(1));
if(Boolean.FALSE.

equals(first))return;
```

幂等 Key **必须**用消息自带的 `eventId`（`BaseEvent.eventId`），**不**用 MQ 自动生成的 `MsgId`（重投递会变）。

## 死信处理（DLQ）

RocketMQ 默认重试 16 次后进入 `%DLQ%{ConsumerGroup}` 队列。继承 `AbstractDlqListener` 处理（实现 3 个方法：原消费者组、事件类、死信处理逻辑）：

```java

@Component
public class OrderCreatedDlqListener
        extends AbstractDlqListener<OrderCreatedIntegrationEvent> {

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
    protected Class<OrderCreatedIntegrationEvent> getEventClass() {
        return OrderCreatedIntegrationEvent.class;
    }

    @Override
    protected void handleDeadLetter(OrderCreatedIntegrationEvent event, int totalAttempts) {
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
