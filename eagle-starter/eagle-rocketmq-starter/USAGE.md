# eagle-rocketmq-starter — RocketMQ 5.x 事件发布、事务消息、死信处理、分布式锁

## 何时使用

- 跨模块 / 跨服务的领域事件发布
- 需要"本地事务 + 发消息原子性"的场景（事务消息）
- 死信队列处理 + 告警
- 分布式锁（RocketMQ SimpleConsumer 实现，备选；推荐用 redis-starter 的 Redisson 锁）

## 何时不要使用

- 同模块内事件 → 用 Spring `ApplicationEvent`
- 强一致性 RPC → 用 Feign（`http-client-starter`）
- 削峰单一目的 → 直接 RocketMQTemplate 也可以，但失去 starter 抽象

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-rocketmq-starter')
```

```yaml
rocketmq:
  name-server: ${ROCKETMQ_NS:127.0.0.1:9876}
  producer:
    group: ${spring.application.name}_producer

eagle.rocketmq:
  enabled: true
  producer.send-timeout-ms: 3000
  producer.retry-times-when-send-failed: 2
```

## 核心 API

| 类 / 接口 | 用途 |
|---|---|
| `DomainEventPublisher` | 领域事件发布抽象接口 |
| `RocketMqDomainEventPublisher` | 普通同步发送实现 |
| `TransactionalEventPublisher` | 事务消息发布抽象 |
| `RocketMqTransactionalEventPublisher` | 事务消息实现（半消息 + 回查） |
| `AbstractRocketMqTransactionChecker` | 事务回查模板 |
| `AbstractRocketMqListener<T>` | 消费者基类（自动幂等 + 反序列化）|
| `AbstractDlqListener<T>` | 死信消费者基类 |
| `RocketMqDistributedLock` | `DistributedLock` 的 RocketMQ 实现 |
| `LockTokenInitializer` | 锁 token 启动初始化 |
| `RocketMqErrorCode` | 错误码 |

## 最小示例

```java
// 1) 普通发布
@RequiredArgsConstructor
@Service
public class OrderApplicationService {
    private final DomainEventPublisher publisher;

    public void create(...) {
        Order order = orderRepository.save(Order.create(...));
        publisher.publish("prod_order_created",
            new OrderCreatedIntegrationEvent(order.getId(), ...));
    }
}

// 2) 事务消息（本地事务 + 发消息原子性）
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final TransactionalEventPublisher publisher;

    @Transactional(rollbackFor = Exception.class)
    public void pay(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(...);
        order.markPaid();
        publisher.publishInTransaction(
            "prod_order_paid",
            new OrderPaidIntegrationEvent(orderId, ...),
            () -> { /* 本地事务回调 */ }
        );
    }
}

// 3) 消费者
@Component
@RocketMQMessageListener(topic = "prod_order_created",
    consumerGroup = "stock_service_order_created")
public class OrderCreatedConsumer
    extends AbstractRocketMqListener<OrderCreatedIntegrationEvent> {

    @Override
    protected void handle(OrderCreatedIntegrationEvent event, MessageExt msg) {
        stockService.lockStock(event.orderId(), event.items());
    }
}

// 4) 死信处理
@Component
public class OrderCreatedDlqListener
    extends AbstractDlqListener<OrderCreatedIntegrationEvent> {

    @Override
    protected void onDeadLetter(OrderCreatedIntegrationEvent event, MessageExt msg) {
        alarmService.notifyOps("order_created_dlq", event);
    }
}
```

## 配置项

| key | 类型 | 默认 | 说明 |
|---|---|---|---|
| `eagle.rocketmq.enabled` | boolean | `true` | 总开关 |
| `eagle.rocketmq.producer.send-timeout-ms` | int | `3000` | 同步发送超时 |
| `eagle.rocketmq.producer.retry-times-when-send-failed` | int | `2` | 内部重试次数 |
| `eagle.rocketmq.consumer.max-reconsume-times` | int | `16` | 进入死信前最大重试 |

## 常见错误

- ❌ 直接用 `RocketMQTemplate` 裸调 → ✅ 用 `DomainEventPublisher`（统一抽象 + 监控）
- ❌ 异步发送 (`sendOneway`) 业务关键消息 → ✅ 同步或事务消息
- ❌ 消息体含密码 / Token → ✅ 仅传 ID，详见 `12-security.md`
- ❌ 消费者用 `MsgId` 做幂等 → ✅ 用消息中的 `eventId`
- ❌ 死信不处理（静默吞掉）→ ✅ `AbstractDlqListener` + 告警
- ❌ Topic 命名不带环境前缀 → ✅ `{env}_{domain}_{event}`

## 关联规则

- `.claude/rules/15-messaging.md` — Topic 命名 / 幂等 / 死信 / 顺序消息
- `.claude/rules/16-transaction-distributed.md` — 事务消息选型
- `.claude/rules/03-architecture.md` — 跨域事件契约（内部 vs 集成）
