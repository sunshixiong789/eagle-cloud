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

⚠️ 此 starter **不**使用 `@RabbitListener` 注解——队列名是运行时由 `getTopic()` 推导的，编译期常量注解表达不了。
改走框架为这个场景准备的扩展点：`AmqpListenerRegistrar` 实现 `RabbitListenerConfigurer`，
把每个 listener 注册成 `SimpleRabbitListenerEndpoint`，容器由 Boot 的
`SimpleRabbitListenerContainerFactoryConfigurer` 创建。

⚠️ **不要改回手动 `new DirectMessageListenerContainer(...)`** —— 那会绕开 Boot 的装配路径，
让 `spring.rabbitmq.listener.*` 整片配置静默失效（配了不生效、也不报错）。这是踩过的坑，
`AmqpAutoConfigurationTest` 守着它。

## 核心 API

| 类 / 接口                                     | 用途                                                                             |
|---------------------------------------------|--------------------------------------------------------------------------------|
| `DomainEventPublisher`                      | 发布抽象，只有两个重载（无 async/delayed/ordered/transactional，现网零调用已删除）                       |
| `RabbitDomainEventPublisher`                | 默认实现，直接用注入的 `ObjectMapper` 序列化为 JSON bytes 发送，不经 `MessageConverter`               |
| `AbstractAmqpListener<T extends BaseEvent>` | 消费者基类，实现 `getTopic() / getEventClass() / handle(T event)`                        |
| `AbstractDlqListener<T>`                    | 死信消费者，实现 `getOriginalTopic() / getOriginalConsumerGroup() / getEventClass() / handleDeadLetter(T, totalAttempts)` |
| `ExchangeNaming`                            | 拓扑命名工具：`exchange()` / `queue()` / `deadLetterExchange()` / `deadLetterQueue()`   |
| `AmqpListenerRegistrar`                     | `RabbitListenerConfigurer`，启动期遍历所有 `AbstractAmqpListener` bean 声明拓扑 + 注册 endpoint |
| `EagleRepublishRecoverer`                   | 唯一的 `MessageRecoverer` bean，重试耗尽后投 DLX；DLX 与 routing key 按 SpEL 逐条消息求值       |
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

RabbitMQ 没有 RocketMQ Broker 侧的递增退避重试，改由消费线程内的退避重试实现。
**这一整套都是框架的**：retry advice 由 Boot 的容器工厂按
`spring.rabbitmq.listener.simple.retry.*` 构建，starter 一行重试代码都不写，
只提供一个 `MessageRecoverer` bean 告诉框架「投到哪」。

- `retry.max-retries`（默认 3，即含首次共 4 次）耗尽后由 `EagleRepublishRecoverer` 投递到 DLX，`x-eagle-attempts` header 记录总尝试次数
- `handleDeadLetter(event, totalAttempts)` 拿到的是真实次数，不再像 RocketMQ 时代恒为 0
- 反序列化失败直接进 DLQ（不重试）：`AmqpMessageDispatcher` 抛 `AmqpRejectAndDontRequeueException`，报文有问题重试多少次都一样
- DLQ listener 走**不挂 retry advice** 的独立容器工厂 —— 死信重试会在 DLQ 里打转

### DLQ 保留策略走 policy，不写进队列声明

DLQ 声明**不带任何 arguments**。保留策略（`x-message-ttl` / `x-max-length`）由 broker 侧 policy 承担，
设置方式见 [docs/rabbitmq-dlq-policy.md](../../../docs/rabbitmq-dlq-policy.md)。

原因：队列 arguments 创建后不可变，客户端每次启动都会重声明，broker 做全等比较，不一致就回
`406 PRECONDITION_FAILED` 关掉 channel —— 在启动期就是**整个服务起不来**。写进声明等于让「调一次 TTL」
变成「停服务、删光所有 DLQ、再启动」，且每个环境各做一遍。**不要往 DLQ 声明里加 arguments。**

## 配置项

`eagle.amqp.*` **只管拓扑命名**，消费行为一律用 Spring Boot 的标准键 ——
迁移前那套逐一重复的 `eagle.amqp.consumer.*` 已删除。

| key                            | 类型     | 默认             | 说明                      |
|--------------------------------|--------|----------------|-------------------------|
| `eagle.amqp.exchange-prefix`   | String | `""`           | exchange 名环境前缀          |
| `eagle.amqp.consumer-group`    | String | `eagle_default`| 消费者默认分组，**必须被每个消费者覆盖** |

消费行为（下面这些默认值由 starter 的 `EagleAmqpDefaultsEnvironmentPostProcessor` 提供，
优先级最低，yml / 环境变量 / Consul KV 配了都能盖掉）：

| key                                                     | starter 默认 | 说明                          |
|---------------------------------------------------------|------------|-----------------------------|
| `spring.rabbitmq.listener.simple.prefetch`               | `32`       | `basic.qos` 预取数量            |
| `spring.rabbitmq.listener.simple.retry.enabled`          | `true`     | **Boot 原生默认是 false**，不开重试会静默消失 |
| `spring.rabbitmq.listener.simple.retry.max-retries`      | `3`        | 重试次数（**不含**首次），耗尽后进 DLQ     |
| `spring.rabbitmq.listener.simple.retry.initial-interval` | `1s`       | 首次重试退避时长                    |
| `spring.rabbitmq.listener.simple.retry.max-interval`     | `30s`      | 退避时长上限                      |
| `spring.rabbitmq.listener.simple.retry.multiplier`       | `2.0`      | 退避倍率                        |
| `spring.rabbitmq.listener.simple.default-requeue-rejected`| `false`    | 由 starter 设在工厂上，显式配置仍优先     |

## 常见错误

- ❌ `@RabbitListener` 注解 → ✅ **不用注解**，继承 `AbstractAmqpListener` + 实现 3 个抽象方法
- ❌ 覆盖 `getTagExpression()` → ✅ 方法已改名 **`getRoutingKey()`**，默认 `"#"`（不是 RocketMQ 语义的 `"*"`）
- ❌ 消费者不覆盖 `getConsumerGroup()` → ✅ 必须覆盖为唯一值，否则同 exchange 多消费者竞争消费
- ❌ `publisher.publishAsync/publishDelayed/publishOrdered(...)` → ✅ 接口已删除这些方法（现网零调用）；只有 `publish(topic, event)` / `publish(topic, routingKey, event)`
- ❌ DLQ 子类只覆盖 `getOriginalConsumerGroup()` → ✅ 还必须覆盖 **`getOriginalTopic()`**（新增的抽象方法）
- ❌ 用 `MsgId` 做幂等 → ✅ 用 `BaseEvent.eventId`
- ❌ 死信不告警 → ✅ `AbstractDlqListener.handleDeadLetter` 必须告警 + 持久化
- ❌ 给 DLQ 声明加 `ttl()` / `maxLength()` 等 arguments → ✅ 队列 arguments 不可变，重声明不一致会 406 让**服务起不来**；保留策略走 broker policy
- ❌ 消息体放敏感字段 → ✅ 仅传 ID
- ❌ 配置写 `eagle.rocketmq.*` → ✅ 真实前缀是 `eagle.amqp.*`；broker 连接走 Spring Boot 原生 `spring.rabbitmq.*`

## 关联规则

- `.claude/rules/02-architecture.md` — 跨域事件契约
