# eagle-rocketmq-starter

基于 RocketMQ 5.x 轻量客户端（gRPC）的消息队列封装模块，提供领域事件的同步发布、异步发布、延迟消息、顺序消息（FIFO）、Tag
过滤以及事务消息（Outbox Pattern）能力。

## 目录

- [模块概述](#模块概述)
- [引入依赖](#引入依赖)
- [自动配置说明](#自动配置说明)
- [配置参考](#配置参考)
- [使用规范](#使用规范)
- [功能使用说明](#功能使用说明)
    - [同步发布](#同步发布)
    - [异步发布](#异步发布)
    - [延迟消息](#延迟消息)
    - [顺序消息（FIFO）](#顺序消息fifo)
    - [Tag 过滤](#tag-过滤)
    - [消费者](#消费者)
    - [事务消息（Outbox Pattern）](#事务消息outbox-pattern)
- [消费端必达保障](#消费端必达保障)
    - [第一层：RocketMQ 自动重试](#第一层rocketmq-自动重试)
    - [第二层：重试告警](#第二层重试告警)
    - [第三层：死信队列处理](#第三层死信队列处理)
    - [消费幂等性](#消费幂等性)
- [与 DDD 架构集成](#与-ddd-架构集成)
- [消息发布方式选型](#消息发布方式选型)
- [常见问题](#常见问题)

---

## 模块概述

### 提供的能力

| 分类   | 组件                                                   | 说明                                 |
|------|------------------------------------------------------|------------------------------------|
| 同步发布 | `DomainEventPublisher.publish()`                     | 阻塞发送，支持自动推导 Topic 和指定 Tag          |
| 异步发布 | `DomainEventPublisher.publishAsync()`                | 非阻塞发送，返回 `CompletableFuture<Void>` |
| 延迟消息 | `DomainEventPublisher.publishDelayed()`              | 指定延迟时长后消费者才可见                      |
| 顺序消息 | `DomainEventPublisher.publishOrdered()`              | 相同 `messageGroup` 内严格 FIFO         |
| 事务消息 | `TransactionalEventPublisher.publishInTransaction()` | 本地事务与消息发布原子，Outbox Pattern         |
| 消费者  | `AbstractRocketMqListener<T>`                        | 推模式消费，自动反序列化为领域事件，支持 Tag 过滤        |

### 依赖关系

```
eagle-rocketmq-starter
├── eagle-common-starter          ← BaseEvent / ErrorCode / ServiceException
└── rocketmq-v5-client-spring-boot-starter ← RocketMQ 5.x gRPC 轻量客户端
```

---

## 引入依赖

在需要使用 RocketMQ 的服务模块 `build.gradle` 中添加：

```gradle
dependencies {
    implementation project(':eagle-starter:eagle-rocketmq-starter')
}
```

引入后自动生效，`DomainEventPublisher` 和 `TransactionalEventPublisher` 可直接注入使用。

---

## 自动配置说明

`RocketMqAutoConfiguration` 在以下条件满足时生效：

- 类路径存在 `org.apache.rocketmq.client.apis.ClientServiceProvider`
- 配置项 `eagle.rocketmq.enabled=true`（默认为 `true`）

| Bean                          | 类型                                    | 条件                                                     |
|-------------------------------|---------------------------------------|--------------------------------------------------------|
| `DomainEventPublisher`        | `RocketMqDomainEventPublisher`        | 默认注册，可覆盖                                               |
| `TransactionalEventPublisher` | `RocketMqTransactionalEventPublisher` | 默认注册，有 `AbstractRocketMqTransactionChecker` Bean 时自动绑定 |

**Bean 覆盖：** 若需自定义发布逻辑，声明同名 Bean 即可：

```java
@Bean
@Primary
public DomainEventPublisher domainEventPublisher(RocketMqProperties properties) {
    return new MyCustomEventPublisher(properties);
}
```

---

## 配置参考

```yaml
eagle:
  rocketmq:
    enabled: true                        # 是否启用（默认 true）
    endpoints: ${ROCKETMQ_ENDPOINTS:localhost:8081}  # NameServer 接入点
    producer-group: eagle-producer-group # 生产者组
    consumer-group: eagle-consumer-group # 消费者组（Listener 未覆盖时使用）
    topic-prefix: eagle-                 # 自动推导 Topic 的前缀
    request-timeout-millis: 3000         # 请求超时（毫秒，默认 3000）
    max-attempts: 2                      # 同步发送失败重试次数（默认 2）
    consumer:
      max-cached-message-count: 1024     # 本地缓存最大消息条数（控制消费速率）
      max-cached-message-size-in-bytes: 67108864  # 本地缓存最大字节数（默认 64MB）
```

### 配置项说明

| 配置项                                                | 默认值              | 说明                                                                                 |
|----------------------------------------------------|------------------|------------------------------------------------------------------------------------|
| `eagle.rocketmq.enabled`                           | `true`           | 设为 `false` 时所有发布操作降级为打印警告日志，不发送                                                    |
| `eagle.rocketmq.endpoints`                         | `localhost:8081` | RocketMQ 5.x Proxy 地址（不是 NameServer 端口 9876）                                       |
| `eagle.rocketmq.topic-prefix`                      | `eagle-`         | `publish(event)` 自动推导 Topic 时的前缀，如 `OrderCreatedEvent` → `eagle-OrderCreatedEvent` |
| `eagle.rocketmq.max-attempts`                      | `2`              | 同步发送失败后的额外重试次数，总计 `maxAttempts + 1` 次尝试                                            |
| `eagle.rocketmq.consumer.max-cached-message-count` | `1024`           | 消费者本地积压上限，超过后暂停从 Broker 拉取，防止内存溢出                                                  |

---

## 使用规范

### Topic 命名规范

| 规则         | 示例                                           |
|------------|----------------------------------------------|
| 自动推导（推荐）   | `publish(event)` → `eagle-OrderCreatedEvent` |
| 显式指定       | `publish("order-created", event)`            |
| Topic 命名格式 | `{prefix}{EventClassName}` 或 kebab-case      |
| **禁止**     | 不同业务复用同一 Topic；Topic 名包含特殊字符                 |

### 消费者幂等性要求

RocketMQ **至少一次（at-least-once）** 投递，消费者必须实现幂等处理：

```java
// ✅ 使用 eventId 做幂等检查
@Override
protected void handle(OrderCreatedEvent event) {
    if (eventLogRepository.existsByEventId(event.getEventId())) {
        log.warn("Duplicate event, skip. eventId: {}", event.getEventId());
        return;
    }
    // 业务处理
    eventLogRepository.save(event.getEventId());
}
```

### 消费者事务规范

消费者 `handle()` 内需要写 DB 时，必须加 `@Transactional`（在应用服务或 Repository 层）。若 `handle()` 抛出异常，消息将被重新投递（
`ConsumeResult.FAILURE`）。

---

## 功能使用说明

### 同步发布

`publish()` 阻塞直到 Broker 确认收到消息。适合业务主流程中需要**感知发布结果**的场景。

```java
@Service
@RequiredArgsConstructor
public class OrderApplicationService {

    private final DomainEventPublisher eventPublisher;

    @Transactional(rollbackFor = Exception.class)
    public void createOrder(CreateOrderRequest request) {
        Order order = Order.create(request.getOrderNo());
        orderRepository.save(order);

        // 方式一：自动推导 Topic → eagle-OrderCreatedEvent
        eventPublisher.publish(new OrderCreatedEvent(order.getId(), order.getOrderNo()));

        // 方式二：显式指定 Topic
        eventPublisher.publish("order-created", new OrderCreatedEvent(order.getId(), order.getOrderNo()));

        // 方式三：指定 Topic + Tag（便于消费侧精细过滤）
        eventPublisher.publish("order-events", "CREATED", new OrderCreatedEvent(order.getId(), order.getOrderNo()));
    }
}
```

> **注意：** 同步发布在 `@Transactional` 方法内调用时，消息在事务提交前就已发送到
> Broker。若事务回滚，消息不会撤回。需要原子性保证时，请使用[事务消息](#事务消息outbox-pattern)。

---

### 异步发布

`publishAsync()` 立即返回 `CompletableFuture<Void>`，不阻塞调用方线程。适合**对延迟敏感**的接口，或发布失败不影响主流程的场景。

```java
@Service
@RequiredArgsConstructor
public class NotificationApplicationService {

    private final DomainEventPublisher eventPublisher;

    public void sendNotification(NotificationRequest request) {
        // 主流程不等待消息发布结果
        eventPublisher.publishAsync(new NotificationSentEvent(request.getUserId(), request.getContent()))
                .exceptionally(ex -> {
                    // 发布失败时告警，不影响主流程返回
                    log.error("Failed to publish notification event", ex);
                    alertService.sendAlert("RocketMQ publish failed: " + ex.getMessage());
                    return null;
                });

        // 主流程继续执行
        log.info("Notification request accepted");
    }
}
```

---

### 延迟消息

`publishDelayed()` 指定延迟时长，消费者在延迟到期后才能收到消息。常用于**超时自动处理**场景。

```java
@Service
@RequiredArgsConstructor
public class OrderApplicationService {

    private final DomainEventPublisher eventPublisher;

    @Transactional(rollbackFor = Exception.class)
    public void createOrder(CreateOrderRequest request) {
        Order order = Order.create(request.getOrderNo());
        orderRepository.save(order);

        // 30 分钟后触发超时检查（未支付则自动取消）
        eventPublisher.publishDelayed(
                new OrderTimeoutCheckEvent(order.getId()),
                Duration.ofMinutes(30)
        );

        log.info("Order created, timeout check scheduled in 30 minutes");
    }
}
```

**消费者：**

```java
@Component
public class OrderTimeoutCheckListener extends AbstractRocketMqListener<OrderTimeoutCheckEvent> {

    private final OrderApplicationService orderApplicationService;

    public OrderTimeoutCheckListener(OrderApplicationService orderApplicationService) {
        this.orderApplicationService = orderApplicationService;
    }

    @Override
    protected String getTopic() {
        return "eagle-OrderTimeoutCheckEvent";
    }

    @Override
    protected Class<OrderTimeoutCheckEvent> getEventClass() {
        return OrderTimeoutCheckEvent.class;
    }

    @Override
    protected void handle(OrderTimeoutCheckEvent event) {
        orderApplicationService.cancelIfUnpaid(event.getOrderId());
    }
}
```

---

### 顺序消息（FIFO）

`publishOrdered()` 保证相同 `messageGroup` 的消息严格按发送顺序被消费。适合**状态机流转**、**账户流水**等需要有序处理的场景。

```java
@Service
@RequiredArgsConstructor
public class AccountApplicationService {

    private final DomainEventPublisher eventPublisher;

    /**
     * 账户流水必须按顺序消费，避免余额计算错乱。
     * messageGroup 使用 accountId，保证同一账户的消息有序。
     */
    @Transactional(rollbackFor = Exception.class)
    public void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        // 扣款事件 → 同一账户（fromAccountId）内严格有序
        eventPublisher.publishOrdered(
                new AccountDebitedEvent(fromAccountId, amount),
                String.valueOf(fromAccountId)  // messageGroup = 账户 ID
        );

        // 入账事件
        eventPublisher.publishOrdered(
                new AccountCreditedEvent(toAccountId, amount),
                String.valueOf(toAccountId)
        );
    }
}
```

> **注意：** 顺序消息要求 Topic 在 Broker 上创建为 **FIFO 类型**，普通 Topic 不保证顺序。

---

### Tag 过滤

通过 Tag 将同一 Topic 的消息按业务语义分类，消费者按需订阅，减少无效消息处理。

**发布侧（生产者）：**

```java
// 同一 Topic "order-events"，用 Tag 区分事件类型
eventPublisher.publish("order-events", "CREATED",   new OrderCreatedEvent(...));
eventPublisher.publish("order-events", "PAID",      new OrderPaidEvent(...));
eventPublisher.publish("order-events", "CANCELLED", new OrderCancelledEvent(...));
```

**消费侧（消费者）— 只订阅支付事件：**

```java
@Component
public class OrderPaidNotifyListener extends AbstractRocketMqListener<OrderPaidEvent> {

    @Override
    protected String getTopic() {
        return "order-events";
    }

    @Override
    protected String getTagExpression() {
        return "PAID";  // 只接收 Tag = PAID 的消息
    }

    // 同时订阅多个 Tag
    // protected String getTagExpression() {
    //     return "PAID || CANCELLED";
    // }

    @Override
    protected Class<OrderPaidEvent> getEventClass() {
        return OrderPaidEvent.class;
    }

    @Override
    protected void handle(OrderPaidEvent event) {
        notificationService.sendPaymentConfirmation(event.getOrderId());
    }
}
```

---

### 消费者

继承 `AbstractRocketMqListener<T>`，声明为 `@Component`，只需实现三个抽象方法：

```java
@Component
@RequiredArgsConstructor
public class OrderCreatedEventListener extends AbstractRocketMqListener<OrderCreatedEvent> {

    private final InventoryApplicationService inventoryService;

    /**
     * 监听的 Topic。
     * 若使用 publish(event) 自动推导，格式为 {topicPrefix}{EventClassName}。
     */
    @Override
    protected String getTopic() {
        return "eagle-OrderCreatedEvent";
    }

    /** 消息体反序列化的目标类型。 */
    @Override
    protected Class<OrderCreatedEvent> getEventClass() {
        return OrderCreatedEvent.class;
    }

    /**
     * 业务处理逻辑。
     * 方法抛出异常时返回 FAILURE，RocketMQ 自动重试。
     */
    @Override
    protected void handle(OrderCreatedEvent event) {
        inventoryService.lockInventory(event.getOrderId(), event.getItems());
    }
}
```

**可选覆盖项：**

```java
// 覆盖消费者组（不同消费者组可以各自消费同一条消息）
@Override
protected String getConsumerGroup() {
    return "inventory-consumer-group";
}

// 覆盖接入点（跨集群消费时使用）
@Override
protected String getEndpoints() {
    return "other-cluster:8081";
}

// Tag 过滤（默认 "*" 接收所有）
@Override
protected String getTagExpression() {
    return "HIGH_PRIORITY";
}
```

**多消费者组消费同一 Topic：**

```java
// 消费者 A：库存服务扣减库存
@Component
public class InventoryOrderListener extends AbstractRocketMqListener<OrderCreatedEvent> {
    @Override protected String getTopic() { return "eagle-OrderCreatedEvent"; }
    @Override protected String getConsumerGroup() { return "inventory-consumer-group"; }
    // ...
}

// 消费者 B：积分服务发放积分（独立消费，互不干扰）
@Component
public class PointsOrderListener extends AbstractRocketMqListener<OrderCreatedEvent> {
    @Override protected String getTopic() { return "eagle-OrderCreatedEvent"; }
    @Override protected String getConsumerGroup() { return "points-consumer-group"; }
    // ...
}
```

---

### 事务消息（Outbox Pattern）

**解决问题：** 数据库写入与消息发布的原子性。普通 `publish()` 在 `@Transactional` 内调用时，若事务回滚，消息已发出且无法撤回，导致
**消费方执行了一个未实际发生的操作**。

**流程：**

```
① 发送半消息（Half Message）→ Broker 接收，消费者不可见
② 执行本地事务（写 DB）
③ 本地成功 → commit 消息（消费者可见）
   本地失败 → rollback 消息（消费者永不可见）
④ 若 Broker 未收到 commit/rollback → 触发回查（TransactionChecker）
```

#### 步骤一：实现 TransactionChecker（回查器）

回查器用于在网络故障等异常情况下，Broker 主动询问本地事务是否已提交。

```java
/**
 * 订单事务回查器：通过查询 DB 判断本地事务是否已提交。
 * 注册为 @Component 后自动绑定到 TransactionalEventPublisher。
 */
@Component
@RequiredArgsConstructor
public class OrderTransactionChecker extends AbstractRocketMqTransactionChecker {

    private final OrderRepository orderRepository;

    /**
     * Broker 回查时调用：判断对应的本地事务是否已提交。
     *
     * @param messageView 回查消息（含发布时设置的属性，如 orderId）
     * @return true → commit 消息；false → UNKNOWN（Broker 继续重试回查）
     */
    @Override
    protected boolean isTransactionCommitted(MessageView messageView) {
        // 从消息属性中取出业务 ID（发布时通过 setProperties 写入，此处为示意）
        String orderId = messageView.getProperties().get("orderId");
        if (orderId == null) {
            return false;
        }
        // 查询 DB：订单存在则说明本地事务已提交
        return orderRepository.existsById(Long.parseLong(orderId));
    }
}
```

#### 步骤二：使用 TransactionalEventPublisher 发布

```java
@Service
@RequiredArgsConstructor
public class OrderApplicationService {

    private final OrderRepository orderRepository;
    private final TransactionalEventPublisher transactionalEventPublisher;

    /**
     * 创建订单：本地 DB 写入与消息发布原子。
     *
     * 不需要 @Transactional，事务由 TransactionCallback 内部控制。
     */
    public void createOrder(CreateOrderRequest request) {
        OrderCreatedEvent event = new OrderCreatedEvent(
                null,  // ID 此时未知，在 callback 内赋值
                request.getOrderNo()
        );

        transactionalEventPublisher.publishInTransaction(event, () -> {
            // 本地事务：写入数据库
            Order order = Order.create(request.getOrderNo());
            orderRepository.save(order);

            // 更新事件中的业务 ID（回查器用到）
            event.setOrderId(order.getId());

            log.info("Order saved, orderId: {}", order.getId());
            return true;   // true → commit 消息
        });
    }
}
```

#### 使用事务消息 vs 普通发布的对比

```java
// ❌ 危险：事务回滚后消息已发出，消费方执行了未实际发生的操作
@Transactional(rollbackFor = Exception.class)
public void createOrder(CreateOrderRequest request) {
    Order order = Order.create(request.getOrderNo());
    orderRepository.save(order);
    eventPublisher.publish(new OrderCreatedEvent(order.getId()));  // 已发出！
    someService.doSomething();  // 若此处抛异常，事务回滚，但消息已发
}

// ✅ 正确：事务回滚时消息也回滚
public void createOrder(CreateOrderRequest request) {
    transactionalEventPublisher.publishInTransaction(
        new OrderCreatedEvent(null, request.getOrderNo()),
        () -> {
            Order order = Order.create(request.getOrderNo());
            orderRepository.save(order);
            return true;
        }
    );
}
```

---

## 消费端必达保障

框架通过三层机制保证消息最终被消费，对应 `AbstractRocketMqListener` 中的三个阶段：

```
消息到达
   │
   ├─① 反序列化
   │    ├─ 成功 → 进入业务处理
   │    └─ 失败 → ACK 丢弃（格式错误，重试无意义）+ onDeserializationFailed() 告警
   │
   ├─② 业务处理 handle()
   │    ├─ 成功 → ACK，消息消费完毕
   │    └─ 失败 → FAILURE，Broker 重试
   │
   └─③ 超过重试次数
        ├─ attempt >= retryAlertThreshold → onRetryAlert() 告警（仍继续重试）
        └─ 超过 Broker 最大重试 → 进入 DLQ → AbstractDlqListener 兜底处理
```

### 第一层：RocketMQ 自动重试

`handle()` 抛出任何异常时，框架返回 `ConsumeResult.FAILURE`，Broker 按**指数退避**间隔重新投递：

| 重试次数 | 延迟时间 |
|------|------|
| 1    | 10s  |
| 2    | 30s  |
| 3    | 1min |
| 4    | 2min |
| 5    | 3min |
| ...  | ...  |
| 16   | 2h   |

最大重试次数由 Broker 订阅组配置控制（`subscriptionGroupConfig.retryMaxTimes`，默认 16）。**无需客户端代码做任何额外配置**
，失败即自动重试。

> **注意：** 下游依赖（DB、第三方接口）临时不可用时，重试天然会等待恢复。但业务逻辑 bug 导致的失败会持续重试直到进入
> DLQ，因此幂等性和 DLQ 处理缺一不可。

### 第二层：重试告警

默认重试 3 次后调用 `onRetryAlert()`，输出 ERROR 日志。**覆盖此方法接入告警平台**：

```java
@Component
public class InventoryOrderListener extends AbstractRocketMqListener<OrderCreatedEvent> {

    private final AlertService alertService;  // 钉钉/企微/邮件告警

    @Override
    protected void onRetryAlert(MessageView messageView, String rawBody,
                                OrderCreatedEvent event, Exception cause) {
        // 超过 3 次还没消费成功，主动告警
        alertService.sendDingTalk(
            "🔴 消息消费异常\n" +
            "Topic: " + messageView.getTopic() + "\n" +
            "MessageId: " + messageView.getMessageId() + "\n" +
            "重试次数: " + messageView.getDeliveryAttempt() + "\n" +
            "订单ID: " + (event != null ? event.getOrderId() : "未知") + "\n" +
            "错误: " + cause.getMessage()
        );
    }

    // 调整告警阈值（默认 3，此处改为 5）
    @Override
    protected int getRetryAlertThreshold() {
        return 5;
    }

    // ... 其他方法
}
```

**反序列化失败也可以接入告警**（消息格式异常时）：

```java
@Override
protected void onDeserializationFailed(MessageView messageView, String rawBody, Exception cause) {
    // 默认只打 ERROR 日志，覆盖后可持久化到异常消息表
    badMessageRepository.save(BadMessageRecord.of(
        messageView.getMessageId(),
        messageView.getTopic(),
        rawBody,
        cause.getMessage()
    ));
    alertService.sendAlert("消息格式异常，需排查生产者: " + messageView.getMessageId());
}
```

### 第三层：死信队列处理

消息超过最大重试次数后，Broker 自动将其投入死信 Topic（`%DLQ%{consumerGroup}`）。继承 `AbstractDlqListener<T>` 即可订阅：

```java
/**
 * 订单创建事件死信处理器。
 * 订阅 Topic: %DLQ%inventory-consumer-group
 */
@Component
@RequiredArgsConstructor
public class OrderCreatedDlqListener extends AbstractDlqListener<OrderCreatedEvent> {

    private final DeadLetterRepository deadLetterRepository;
    private final AlertService alertService;
    private final InventoryCompensationService compensationService;

    /** 与原消费者的 consumerGroup 保持一致 */
    @Override
    protected String getOriginalConsumerGroup() {
        return "inventory-consumer-group";
    }

    @Override
    protected Class<OrderCreatedEvent> getEventClass() {
        return OrderCreatedEvent.class;
    }

    /**
     * 死信处理：持久化 + 告警 + 自动补偿（三选一或组合）。
     */
    @Override
    protected void handleDeadLetter(OrderCreatedEvent event, int totalAttempts) {
        // 策略一：持久化，供运营平台人工重试
        deadLetterRepository.save(DeadLetterRecord.builder()
                .eventId(event.getEventId())
                .topic(getTopic())
                .consumerGroup(getOriginalConsumerGroup())
                .payload(JSON.toJSONString(event))
                .build());

        // 策略二：告警通知
        alertService.sendUrgentAlert(
            "【死信消息】库存扣减失败需人工介入，订单: " + event.getOrderId());

        // 策略三：降级补偿（如取消订单、回滚预占）
        compensationService.rollbackInventoryLock(event.getOrderId());
    }
}
```

**死信消息表设计参考：**

```sql
CREATE TABLE t_dead_letter (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id    VARCHAR(64)  NOT NULL COMMENT '事件 ID（幂等 key）',
    topic       VARCHAR(128) NOT NULL COMMENT 'DLQ Topic',
    group_name  VARCHAR(128) NOT NULL COMMENT '原消费者组',
    payload     TEXT         NOT NULL COMMENT '原始消息体（JSON）',
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RESOLVED',
    created_at  DATETIME     NOT NULL,
    resolved_at DATETIME,
    INDEX idx_event_id (event_id),
    INDEX idx_status   (status)
);
```

### 消费幂等性

RocketMQ **至少一次（at-least-once）** 投递，重试必然带来重复消费，消费者必须实现幂等。推荐以 `eventId` 为去重 key，存入
Redis（短期幂等）或 DB（长期幂等）：

**方案一：Redis 幂等（推荐，低成本）**

```java
@Override
protected void handle(OrderCreatedEvent event) {
    String idempotentKey = "mq:idempotent:" + event.getEventId();

    // SET NX EX：原子性设置，10 分钟内重复消费直接跳过
    Boolean isFirstConsume = redisTemplate.opsForValue()
            .setIfAbsent(idempotentKey, "1", Duration.ofMinutes(10));

    if (!Boolean.TRUE.equals(isFirstConsume)) {
        log.warn("Duplicate message skipped, eventId: {}", event.getEventId());
        return;
    }

    // 业务处理
    inventoryService.lockInventory(event.getOrderId(), event.getItems());
}
```

**方案二：DB 幂等（强一致，适合财务类）**

```java
@Override
@Transactional(rollbackFor = Exception.class)
protected void handle(OrderPaidEvent event) {
    // 唯一索引保证：eventId 已存在时 insert 抛异常
    if (consumedEventRepository.existsByEventId(event.getEventId())) {
        log.warn("Duplicate event skipped, eventId: {}", event.getEventId());
        return;
    }
    consumedEventRepository.save(new ConsumedEvent(event.getEventId()));

    // 业务处理
    accountService.credit(event.getUserId(), event.getAmount());
}
```

**完整消费保障总结：**

| 层次   | 机制                           | 覆盖场景                        |
|------|------------------------------|-----------------------------|
| 第一层  | RocketMQ 自动重试（最多 16 次，指数退避）  | 临时故障：DB 不可用、网络抖动、下游超时       |
| 第二层  | 重试告警（`onRetryAlert`）         | 持续失败早发现，人工介入                |
| 第三层  | 死信队列（`AbstractDlqListener`）  | 代码 bug / 无法自动恢复的场景，持久化待人工处理 |
| 幂等校验 | Redis/DB 去重（`eventId` 为 key） | 重试带来的重复消费                   |

---

## 与 DDD 架构集成

### 发布者放在基础设施层

`DomainEventPublisher` / `TransactionalEventPublisher` 属于消息基础设施，注入位置：

```
infrastructure/
├── event/                        # 领域事件处理器（监听本域 Spring 事件 → 发到 MQ）
│   └── OrderEventHandler.java    # @TransactionalEventListener → eventPublisher.publish()
└── messaging/                    # MQ 消费者（跨服务事件入口）
    └── InventoryOrderListener.java
```

### 典型场景：事件从 Spring 本地事件桥接到 MQ

在模块化单体中，领域事件先通过 Spring 事件机制在本地传播，再由基础设施层桥接到 MQ 供其他服务消费：

```java
/**
 * 订单事件桥接器：将 Spring 本地领域事件转发到 RocketMQ。
 * 放在 order 模块的 infrastructure/event/ 下。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventBridge {

    private final DomainEventPublisher eventPublisher;

    /**
     * 事务提交后异步发布到 RocketMQ，解耦领域层与消息基础设施。
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        eventPublisher.publish(event);
        log.info("OrderCreatedEvent bridged to RocketMQ, orderId: {}", event.getOrderId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPaid(OrderPaidEvent event) {
        // 支付事件带 Tag，便于不同消费者按需订阅
        eventPublisher.publish("order-events", "PAID", event);
    }
}
```

### 跨服务消费者

消费者放在 `infrastructure/messaging/` 下，调用应用服务处理业务：

```java
/**
 * 库存服务消费订单创建事件：锁定库存。
 * 放在 inventory 模块的 infrastructure/messaging/ 下。
 */
@Component
@RequiredArgsConstructor
public class OrderCreatedMessageListener extends AbstractRocketMqListener<OrderCreatedEvent> {

    private final InventoryApplicationService inventoryService;

    @Override
    protected String getTopic() { return "eagle-OrderCreatedEvent"; }

    @Override
    protected String getConsumerGroup() { return "inventory-consumer-group"; }

    @Override
    protected Class<OrderCreatedEvent> getEventClass() { return OrderCreatedEvent.class; }

    @Override
    protected void handle(OrderCreatedEvent event) {
        // 幂等校验 + 业务处理（由应用服务负责）
        inventoryService.lockInventoryForOrder(event.getOrderId(), event.getOrderItems());
    }
}
```

---

## 消息发布方式选型

| 需求            | 推荐方式                                  | 说明                           |
|---------------|---------------------------------------|------------------------------|
| 通知类、允许少量丢失    | `publishAsync()`                      | 非阻塞，主流程不等待结果                 |
| 关键业务事件、需要确认   | `publish()`                           | 阻塞直到 Broker 确认，失败自动重试        |
| 超时处理、定时任务     | `publishDelayed()`                    | 延迟到期后消费者可见                   |
| 账户流水、状态机流转    | `publishOrdered()`                    | 同 Group 内严格 FIFO             |
| DB 写入与消息必须原子  | `publishInTransaction()`              | Two-Phase Commit，防止事务回滚后消息已发 |
| 同 Topic 多消费逻辑 | `publish(topic, tag, event)` + Tag 过滤 | 减少无效消息，精细路由                  |

---

## 常见问题

**Q: `publish()` 在 `@Transactional` 方法内，事务回滚后消息发出去了怎么办？**

A: 这是普通发布的设计限制。解决方案：

1. 使用 `publishInTransaction()` 事务消息，本地事务回滚时消息自动回滚
2. 或将 `publish()` 改为 `@TransactionalEventListener(phase = AFTER_COMMIT)` 中调用，确保事务提交后才发送（但此方式不保证原子性，Broker
   宕机时可能丢失）

---

**Q: 消费者 `handle()` 抛出异常，消息会重复消费吗？**

A: 会。`handle()` 抛异常时返回 `ConsumeResult.FAILURE`，RocketMQ 将按配置的重试间隔重新投递。消费者**必须实现幂等**，用
`eventId` 作去重 key 即可：

```java
@Override
protected void handle(OrderCreatedEvent event) {
    if (processedEventRepository.exists(event.getEventId())) {
        return;  // 已处理，幂等跳过
    }
    // 业务处理
    processedEventRepository.save(event.getEventId());
}
```

---

**Q: 消费者 `getEndpoints()` 和 `getConsumerGroup()` 需要覆盖吗？**

A: 不需要。默认从 `eagle.rocketmq.endpoints` 和 `eagle.rocketmq.consumerGroup` 读取。只有在需要连接不同集群或使用独立消费组时才需要覆盖。

---

**Q: `publishOrdered()` 和普通 `publish()` 有什么区别？**

A: 顺序消息要求：

1. Topic 必须在 Broker 上创建为 **FIFO 类型**
2. 相同 `messageGroup` 的消息路由到同一队列，保证顺序
3. 消费者是单线程顺序消费，吞吐量低于普通消息

不需要全局有序，只需同一业务对象有序时，用聚合根 ID 作 `messageGroup` 即可。

---

**Q: 服务 A 发布消息，服务 B 消费，事件类如何共享？**

A: 推荐将跨服务共用的事件类放在独立的接口模块（`eagle-api` / `{domain}-api`）中，两个服务都依赖该模块。避免直接依赖对方服务的内部包。

---

**Q: `TransactionChecker` 的回查间隔是多少？**

A: 由 Broker 端配置控制，默认首次 6 秒后开始回查，最多回查 15 次，之后消息进入死信队列。生产环境需在 Broker 配置文件中合理设置
`transactionTimeOut` 和 `transactionCheckMax`。
