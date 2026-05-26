# eagle-rocketmq-starter — RocketMQ 5.x 领域事件发布/事务消息/死信处理

## 何时使用

- 跨模块 / 跨服务的领域事件发布
- 本地事务 + 发消息原子保证（RocketMQ 事务消息）
- 死信兜底处理 + 告警
- MQ 模式分布式锁（备选；推荐 redis-starter Redisson 锁）

## 何时不要使用

- 同模块事件 → Spring `ApplicationEvent`
- 强一致性 RPC → Feign

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-rocketmq-starter')
```

```yaml
eagle.rocketmq:
  enabled: true
  endpoints: ${ROCKETMQ_ENDPOINTS:localhost:8081}
  producer-group: ${spring.application.name}-producer
  consumer-group: ${spring.application.name}-consumer
  topic-prefix: eagle-
  request-timeout-millis: 3000
  max-attempts: 2
  consumer:
    max-cached-message-count: 1024
    max-cached-message-size-in-bytes: 67108864     # 64MB
    retry-alert-threshold: 3
```

⚠️ 此 starter 用 **RocketMQ 5.x 原生客户端 API**（`ClientServiceProvider` / `MessageView` / `PushConsumer`），**不**使用
Spring Cloud Alibaba 的 `@RocketMQMessageListener` 注解和 `MessageExt`。

## 核心 API

| 类 / 接口                                          | 用途                                                                                           |
|-------------------------------------------------|----------------------------------------------------------------------------------------------|
| `DomainEventPublisher`                          | 同步/异步/延迟/顺序发布抽象                                                                              |
| `RocketMqDomainEventPublisher`                  | 默认实现                                                                                         |
| `TransactionalEventPublisher`                   | 事务消息发布抽象                                                                                     |
| `RocketMqTransactionalEventPublisher`           | 默认实现                                                                                         |
| `TransactionCallback`                           | 函数式：`boolean execute()`（true 提交 / false 回滚）                                                  |
| `AbstractRocketMqTransactionChecker`            | 事务回查模板：实现 `boolean isTransactionCommitted(MessageView)`                                      |
| `AbstractRocketMqListener<T extends BaseEvent>` | 消费者基类，实现 `getTopic() / getEventClass() / handle(T event)`                                    |
| `AbstractDlqListener<T>`                        | 死信消费者，实现 `getOriginalConsumerGroup() / getEventClass() / handleDeadLetter(T, totalAttempts)` |
| `RocketMqDistributedLock`                       | `DistributedLock` 的 MQ 实现（`eagle.lock.type=mq` 时启用）                                          |
| `RocketMqErrorCode`                             | 错误码                                                                                          |

### `DomainEventPublisher` 方法签名

```java
<T extends BaseEvent> void publish(T event);                     // Topic 自动推导

<T extends BaseEvent> void publish(String topic, T event);

<T extends BaseEvent> void publish(String topic, String tag, T event);

<T extends BaseEvent> CompletableFuture<Void> publishAsync(T event);

<T extends BaseEvent> CompletableFuture<Void> publishAsync(String topic, T event);

<T extends BaseEvent> void publishDelayed(T event, Duration delay);

<T extends BaseEvent> void publishDelayed(String topic, T event, Duration delay);

<T extends BaseEvent> void publishOrdered(T event, String messageGroup);     // 顺序消息

<T extends BaseEvent> void publishOrdered(String topic, T event, String messageGroup);
```

### `TransactionalEventPublisher` 方法签名

```java
<T extends BaseEvent> void publishInTransaction(T event, TransactionCallback callback);

<T extends BaseEvent> void publishInTransaction(String topic, T event, TransactionCallback callback);
```

## 最小示例

```java
// 1) 普通发布
@Service
@RequiredArgsConstructor
public class OrderApplicationService {
    private final DomainEventPublisher publisher;

    public void create(...) {
        Order order = orderRepository.save(Order.create(...));
        publisher.publish(new OrderCreatedEvent(order.getId(), ...));   // 自动推导 Topic
    }
}

// 2) 事务消息（本地事务 + 消息原子）
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final TransactionalEventPublisher txPublisher;
    private final OrderRepository orderRepository;

    public void pay(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        txPublisher.publishInTransaction(
                new OrderPaidEvent(orderId, ...),
        () -> {
            order.markPaid();
            orderRepository.save(order);
            return true;     // 本地事务成功
        }
        );
    }
}

// 3) 消费者（继承 AbstractRocketMqListener，构造器须 super(props) 透传配置）
@Component
public class OrderCreatedConsumer extends AbstractRocketMqListener<OrderCreatedEvent> {

    private final StockApplicationService stockService;

    public OrderCreatedConsumer(RocketMqProperties props,
                                StockApplicationService stockService) {
        super(props);
        this.stockService = stockService;
    }

    @Override
    protected String getTopic() {
        return "eagle-OrderCreatedEvent";
    }

    @Override
    protected Class<OrderCreatedEvent> getEventClass() {
        return OrderCreatedEvent.class;
    }

    @Override
    protected void handle(OrderCreatedEvent event) {
        // 务必幂等！用 event.getEventId() 去重
        stockService.lockStock(event.getOrderId(), event.getItems());
    }

    // 可选覆盖
    @Override
    protected String getConsumerGroup() {
        return "stock-service-order-created";
    }
}

// 4) 死信（继承 AbstractDlqListener，构造器须 super(props)）
@Component
public class OrderCreatedDlqListener extends AbstractDlqListener<OrderCreatedEvent> {

    private final AlertService alertService;

    public OrderCreatedDlqListener(RocketMqProperties props, AlertService alertService) {
        super(props);
        this.alertService = alertService;
    }

    @Override
    protected String getOriginalConsumerGroup() {
        return "stock-service-order-created";
    }

    @Override
    protected Class<OrderCreatedEvent> getEventClass() {
        return OrderCreatedEvent.class;
    }

    @Override
    protected void handleDeadLetter(OrderCreatedEvent event, int totalAttempts) {
        alertService.notifyOps("order_created_dlq", event);
        // 持久化到 t_dead_letter 等待人工/补偿
    }
}

// 5) 事务回查（必须有持久化标识可查询）
@Component
public class OrderTxChecker extends AbstractRocketMqTransactionChecker {
    private final OrderRepository orderRepository;

    @Override
    protected boolean isTransactionCommitted(MessageView msg) {
        String orderId = msg.getProperties().get("orderId");
        return orderRepository.existsById(Long.parseLong(orderId));
    }
}
```

## 配置项

| key                                                        | 类型      | 默认                     | 说明           |
|------------------------------------------------------------|---------|------------------------|--------------|
| `eagle.rocketmq.endpoints`                                 | String  | `localhost:8081`       | 接入点          |
| `eagle.rocketmq.producer-group`                            | String  | `eagle-producer-group` | 生产者组         |
| `eagle.rocketmq.consumer-group`                            | String  | `eagle-consumer-group` | 默认消费者组       |
| `eagle.rocketmq.topic-prefix`                              | String  | `eagle-`               | Topic 自动推导前缀 |
| `eagle.rocketmq.request-timeout-millis`                    | int     | `3000`                 | 客户端请求超时      |
| `eagle.rocketmq.max-attempts`                              | int     | `2`                    | 同步发送最大重试     |
| `eagle.rocketmq.consumer.max-cached-message-count`         | int     | `1024`                 | 本地缓存条数上限     |
| `eagle.rocketmq.consumer.max-cached-message-size-in-bytes` | int     | `64MB`                 | 本地缓存字节上限     |
| `eagle.rocketmq.consumer.retry-alert-threshold`            | int     | `3`                    | 重试告警阈值       |

## 常见错误

- ❌ `@RocketMQMessageListener` 注解 → ✅ **不用注解**，继承 `AbstractRocketMqListener` + 实现 3 个抽象方法
- ❌ `handle(event, MessageExt msg)` → ✅ **`handle(T event)`** 单参（5.x 用 `MessageView` 不是 `MessageExt`）
- ❌ `publishOrderly(...)` → ✅ 真名是 `publishOrdered(event, messageGroup)`
- ❌ 在 `DomainEventPublisher` 调 `publishInTransaction` → ✅ 注入 `TransactionalEventPublisher`
- ❌ 用 `MsgId` 做幂等 → ✅ 用 `BaseEvent.eventId`
- ❌ 死信不告警 → ✅ `AbstractDlqListener.handleDeadLetter` 必须告警 + 持久化
- ❌ 消息体放敏感字段 → ✅ 仅传 ID
- ❌ 配置写 `rocketmq.name-server` → ✅ 真实是 `eagle.rocketmq.endpoints`

## 关联规则

- `.claude/rules/15-messaging.md` — Topic 命名 / 幂等 / 死信
- `.claude/rules/16-transaction-distributed.md` — 事务消息选型
- `.claude/rules/03-architecture.md` — 跨域事件契约
