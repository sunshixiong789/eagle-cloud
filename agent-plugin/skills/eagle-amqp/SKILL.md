---
name: eagle-amqp
description: Use when working with RabbitMQ/Spring AMQP in eagle-cloud projects — DomainEventPublisher (publish), AbstractAmqpListener (extend with getTopic/getEventClass/handle, NOT @RabbitListener), AbstractDlqListener, ExchangeNaming. Takes over from the removed eagle-rocketmq-starter for GraalVM native-image compatibility
---

# eagle-amqp-starter — RabbitMQ 领域事件发布/死信处理

## 何时使用

- 跨模块 / 跨服务的领域事件发布
- 死信兜底处理 + 告警

## 何时不要使用

- 同模块事件 → Spring `ApplicationEvent`
- 强一致性 RPC → RestClient / `@HttpExchange`
- 本地事务 + 发消息原子保证 → 本项目现网零调用，未提供；用
  `@TransactionalEventListener(phase = AFTER_COMMIT)` + DB 唯一约束幂等替代
- 延迟消息 / 顺序消息 / MQ 分布式锁 → 现网零调用，未提供；分布式锁用 `eagle-redis-starter` 的 `DistributedLock`

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-amqp-starter')
```

```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}
    virtual-host: ${RABBITMQ_VHOST:/}
    publisher-confirm-type: correlated
    publisher-returns: true

eagle:
  amqp:
    exchange-prefix: dev_          # exchange 名的环境前缀，跨环境共享 topic（如 eagle_auth_events）留空
    consumer-group: eagle_default  # 每个消费者必须覆盖 getConsumerGroup() 给唯一值，否则退化为竞争消费
    consumer:
      prefetch: 32
      retry-alert-threshold: 3
      max-attempts: 4
      initial-backoff: 1s
      max-backoff: 30s
      multiplier: 2.0
```

⚠️ 此 starter 用 **Spring AMQP 原生 API**（`RabbitTemplate` / `RabbitAdmin` / `DirectMessageListenerContainer`），**不**使用
`@RabbitListener` 注解——队列名是运行时由 `getTopic()` 推导的，编译期常量注解表达不了。

## 核心 API

| 类 / 接口                                     | 用途                                                                             |
|---------------------------------------------|--------------------------------------------------------------------------------|
| `DomainEventPublisher`                      | 发布抽象，只有两个重载（无 async/delayed/ordered/transactional，现网零调用已删除）                       |
| `RabbitDomainEventPublisher`                | 默认实现，直接用注入的 `ObjectMapper` 序列化为 JSON bytes 发送，不经 `MessageConverter`               |
| `AbstractAmqpListener<T extends BaseEvent>` | 消费者基类，实现 `getTopic() / getEventClass() / handle(T event)`                        |
| `AbstractDlqListener<T>`                    | 死信消费者，实现 `getOriginalTopic() / getOriginalConsumerGroup() / getEventClass() / handleDeadLetter(T, totalAttempts)` |
| `ExchangeNaming`                            | 拓扑命名工具：`exchange()` / `queue()` / `deadLetterExchange()` / `deadLetterQueue()`   |
| `AmqpListenerRegistrar`                     | `SmartInitializingSingleton`，启动期遍历所有 `AbstractAmqpListener` bean 声明拓扑 + 建监听容器     |
| `AmqpErrorCode`                             | 错误码（16001-16003，沿用原 RocketMQ 号段）                                                |

## AMQP 拓扑（与 RocketMQ 语义的关键差异）

```
exchange   {exchangePrefix}{topicName}          TopicExchange, durable
queue      {exchange}.{consumerGroup}           durable，每个消费者独立 queue（同 exchange 多消费者各收全量）
DLX        {exchange}.dlx                       TopicExchange
DLQ        {queue}.dlq                          主 queue 声明 x-dead-letter-exchange/routing-key 指向它
```

⚠️ **routing key 语义与 RocketMQ tag 不同**：RocketMQ 的 `"*"` = 全部 tag 匹配；AMQP 的 `*` = 恰好一个词、`#` = 零或多个词。
`getRoutingKey()` 默认值是 `"#"`（全订阅），**不是** `"*"`。子类要按 tag 分流时（如单 exchange 多 routing key 广播），显式覆盖
`getRoutingKey()` 返回具体值，如 `"account.registered"`。

⚠️ **每个消费者必须显式覆盖 `getConsumerGroup()`** 给唯一值——共用默认值 `eagle_default` 会让多个消费者绑到同一 queue，
退化为竞争消费（一条消息只会被其中一个处理，这正是 RocketMQ 时代的线上 bug 根因）。

## 最小示例

```java
// 1) 普通发布
@Service
@RequiredArgsConstructor
public class OrderApplicationService {
    private final DomainEventPublisher publisher;

    public void create(...) {
        Order order = orderRepository.save(Order.create(...));
        publisher.publish("order_created", new OrderCreatedIntegrationEvent(order.getId(), ...));
    }

    // 单 exchange 多 routing key 分流场景（如 eagle_auth_events）
    public void registerAccount(...) {
        publisher.publish("eagle_auth_events", "account.registered", new AccountRegisteredIntegrationEvent(...));
    }
}

// 2) 消费者（继承 AbstractAmqpListener，构造器须 super(props) 透传配置）
@Component
public class OrderCreatedConsumer extends AbstractAmqpListener<OrderCreatedMessage> {

    static final String CONSUMER_GROUP = "stock-service-order-created";

    private final StockApplicationService stockService;

    public OrderCreatedConsumer(AmqpProperties props, StockApplicationService stockService) {
        super(props);
        this.stockService = stockService;
    }

    @Override
    protected String getTopic() {
        return "order_created";
    }

    @Override
    protected Class<OrderCreatedMessage> getEventClass() {
        return OrderCreatedMessage.class;
    }

    @Override
    protected void handle(OrderCreatedMessage event) {
        // 务必幂等！用 event.getEventId() 去重
        stockService.lockStock(event.getOrderId(), event.getItems());
    }

    // 必须覆盖，避免与其他消费者竞争消费
    @Override
    protected String getConsumerGroup() {
        return CONSUMER_GROUP;
    }
}

// 3) 死信（继承 AbstractDlqListener，构造器须 super(props)）
@Component
public class OrderCreatedDlqListener extends AbstractDlqListener<OrderCreatedMessage> {

    private final AlertService alertService;

    public OrderCreatedDlqListener(AmqpProperties props, AlertService alertService) {
        super(props);
        this.alertService = alertService;
    }

    @Override
    protected String getOriginalTopic() {
        return "order_created";
    }

    @Override
    protected String getOriginalConsumerGroup() {
        return OrderCreatedConsumer.CONSUMER_GROUP;
    }

    @Override
    protected Class<OrderCreatedMessage> getEventClass() {
        return OrderCreatedMessage.class;
    }

    @Override
    protected void handleDeadLetter(OrderCreatedMessage event, int totalAttempts) {
        alertService.notifyOps("order_created_dlq", event);
        // 持久化到 dead_letter 表等待人工/补偿
    }
}
```

## 重试与 DLQ 语义

RabbitMQ 没有 RocketMQ Broker 侧的递增退避重试，本 starter 用**手写指数退避**（非 Spring Retry 拦截器）：

- `eagle.amqp.consumer.max-attempts`（默认 4）耗尽后投递到 DLX，`x-eagle-attempts` header 记录真实尝试次数
- `handleDeadLetter(event, totalAttempts)` 拿到的是真实次数，不再像 RocketMQ 时代恒为 0
- 反序列化失败直接进 DLQ（不重试），DLQ 侧仍可查原始消息

## 配置项

| key                                     | 类型       | 默认            | 说明                              |
|------------------------------------------|----------|----------------|---------------------------------|
| `eagle.amqp.exchange-prefix`              | String   | `""`           | exchange 名环境前缀                  |
| `eagle.amqp.consumer-group`               | String   | `eagle_default`| 消费者默认分组，**必须被每个消费者覆盖**          |
| `eagle.amqp.consumer.prefetch`            | int      | `32`           | `basic.qos` 预取数量                |
| `eagle.amqp.consumer.retry-alert-threshold`| int     | `3`            | 重试告警阈值                         |
| `eagle.amqp.consumer.max-attempts`        | int      | `4`            | 最大尝试次数（含首次），耗尽后进 DLQ           |
| `eagle.amqp.consumer.initial-backoff`     | Duration | `1s`           | 首次重试退避时长                       |
| `eagle.amqp.consumer.max-backoff`         | Duration | `30s`          | 退避时长上限                         |
| `eagle.amqp.consumer.multiplier`          | double   | `2.0`          | 退避倍率                            |

## 常见错误

- ❌ `@RabbitListener` 注解 → ✅ **不用注解**，继承 `AbstractAmqpListener` + 实现 3 个抽象方法
- ❌ 覆盖 `getTagExpression()` → ✅ 方法已改名 **`getRoutingKey()`**，默认 `"#"`（不是 RocketMQ 语义的 `"*"`）
- ❌ 消费者不覆盖 `getConsumerGroup()` → ✅ 必须覆盖为唯一值，否则同 exchange 多消费者竞争消费
- ❌ `publisher.publishAsync/publishDelayed/publishOrdered(...)` → ✅ 接口已删除这些方法（现网零调用）；只有 `publish(topic, event)` / `publish(topic, routingKey, event)`
- ❌ DLQ 子类只覆盖 `getOriginalConsumerGroup()` → ✅ 还必须覆盖 **`getOriginalTopic()`**（新增的抽象方法）
- ❌ 用 `MsgId` 做幂等 → ✅ 用 `BaseEvent.eventId`
- ❌ 死信不告警 → ✅ `AbstractDlqListener.handleDeadLetter` 必须告警 + 持久化
- ❌ 消息体放敏感字段 → ✅ 仅传 ID
- ❌ 配置写 `eagle.rocketmq.*` → ✅ 真实前缀是 `eagle.amqp.*`；broker 连接走 Spring Boot 原生 `spring.rabbitmq.*`

## 关联规则

- `.claude/rules/02-architecture.md` — 跨域事件契约
