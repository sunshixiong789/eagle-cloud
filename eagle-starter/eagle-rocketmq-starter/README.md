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
- [支付级强一致消费策略](#支付级强一致消费策略)
    - [问题边界：消费"必达"到底要保证什么](#问题边界消费必达到底要保证什么)
    - [核心原则：本地消息表 + 状态机 + 唯一约束](#核心原则本地消息表--状态机--唯一约束)
    - [模式 A：本地消息表（Inbox）+ 业务事务原子](#模式-a本地消息表inbox-业务事务原子)
    - [模式 B：状态机驱动的幂等消费](#模式-b状态机驱动的幂等消费)
    - [模式 C：TCC 与预留资源](#模式-c-tcc-与预留资源)
    - [系统异常 vs 业务失败 vs 永久失败](#系统异常-vs-业务失败-vs-永久失败)
    - [人工介入闭环：DLQ 重投与对账](#人工介入闭环dlq-重投与对账)
    - [完整支付通知消费样例](#完整支付通知消费样例)
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

无总开关 — 引入 starter 即生效。本地无 broker 时让 `eagle.rocketmq.endpoints` 指向不可达地址即可，
publish 失败会打日志但不阻断启动；消费者会持续重连。

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

    public OrderTimeoutCheckListener(RocketMqProperties props,
                                     OrderApplicationService orderApplicationService) {
        super(props);
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

    private final NotificationService notificationService;

    public OrderPaidNotifyListener(RocketMqProperties props,
                                   NotificationService notificationService) {
        super(props);
        this.notificationService = notificationService;
    }

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
public class OrderCreatedEventListener extends AbstractRocketMqListener<OrderCreatedEvent> {

    private final InventoryApplicationService inventoryService;

    public OrderCreatedEventListener(RocketMqProperties props,
                                     InventoryApplicationService inventoryService) {
        super(props);
        this.inventoryService = inventoryService;
    }

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
    public InventoryOrderListener(RocketMqProperties props) { super(props); }
    @Override protected String getTopic() { return "eagle-OrderCreatedEvent"; }
    @Override protected String getConsumerGroup() { return "inventory-consumer-group"; }
    // ...
}

// 消费者 B：积分服务发放积分（独立消费，互不干扰）
@Component
public class PointsOrderListener extends AbstractRocketMqListener<OrderCreatedEvent> {
    public PointsOrderListener(RocketMqProperties props) { super(props); }
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

    public InventoryOrderListener(RocketMqProperties props, AlertService alertService) {
        super(props);
        this.alertService = alertService;
    }

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
public class OrderCreatedDlqListener extends AbstractDlqListener<OrderCreatedEvent> {

    private final DeadLetterRepository deadLetterRepository;
    private final AlertService alertService;
    private final InventoryCompensationService compensationService;

    public OrderCreatedDlqListener(RocketMqProperties props,
                                   DeadLetterRepository deadLetterRepository,
                                   AlertService alertService,
                                   InventoryCompensationService compensationService) {
        super(props);
        this.deadLetterRepository = deadLetterRepository;
        this.alertService = alertService;
        this.compensationService = compensationService;
    }

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

## 支付级强一致消费策略

> 适用场景：**支付成功通知、扣款、记账、退款、库存扣减、积分发放**等"消费一旦失败，业务就处于不可逆的不一致状态"的关键链路。
>
> 普通业务用上一节的三层保障 + Redis 幂等已足够；本节面向**金融级 / 资金类**消费，要求"
> 消费方原子提交、不重不漏、可对账、可补偿"。

### 问题边界:消费"必达"到底要保证什么

支付场景的"必达"是四个独立属性的乘积，缺一会引起资损：

| 属性                          | 含义                     | 反例（典型事故）           |
|-----------------------------|------------------------|--------------------|
| **不丢失**（at-least-once）      | 消息一定会被消费方收到至少一次        | 消费方 ACK 后处理崩溃 → 丢失 |
| **幂等**（at-most-once-effect） | 同一条消息无论被处理几次，业务结果只发生一次 | 重试导致用户被扣款两次        |
| **原子**（all-or-nothing）      | 业务变更与"已消费"标记必须同事务提交    | 已扣款但状态没标记 → 重试再扣一次 |
| **可观察**                     | 失败/堆积/卡住能被发现并能补偿       | 消息进 DLQ 没人看 → 永久丢失 |

RocketMQ 自身只提供 at-least-once。**幂等、原子、可观察必须靠消费方代码 + DB 约束 + 运营平台共同实现。**

### 核心原则:本地消息表 + 状态机 + 唯一约束

支付级消费要做到"必达且只做一次"，核心三件套：

```
┌────────────────────────────────────────────────────────────┐
│  ① 入库表（Inbox / Consumed Event Log）                       │
│     - 唯一索引 event_id：保证同一消息只能被记录一次               │
│     - 与业务变更同事务提交：要么都成功，要么都回滚                  │
├────────────────────────────────────────────────────────────┤
│  ② 业务聚合根的状态机                                           │
│     - 只有"前置状态正确"时业务方法才会执行（领域层守护不变量）         │
│     - 重复消息进入时,状态已切换 → 业务方法直接 no-op,自然幂等        │
├────────────────────────────────────────────────────────────┤
│  ③ 唯一约束兜底                                                │
│     - 资金流水表 (out_trade_no, channel) 唯一,DB 层最后一道防线  │
│     - 即使应用层判断错了,DB 也不会让重复入账                        │
└────────────────────────────────────────────────────────────┘
```

任何一层单独都不够：

- 只有 Redis 幂等 → Redis 闪断会双扣
- 只有状态机 → 一致性靠代码良心，难以审计
- 只有唯一约束 → 应用层抛异常时无法判断是否真的入账

**三层一起,才能实现"消费必达"。**

### 模式 A:本地消息表(Inbox)+ 业务事务原子

将"消息已消费"和"业务变更"绑在同一个 DB 事务内提交。

#### Inbox 表设计

```sql
CREATE TABLE t_consumed_event (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    event_id        VARCHAR(64)  NOT NULL COMMENT '事件 ID（即 BaseEvent.eventId）',
    consumer_group  VARCHAR(128) NOT NULL COMMENT '消费者组，区分多组消费同一消息',
    topic           VARCHAR(128) NOT NULL,
    payload_md5     CHAR(32)              COMMENT '消息体 MD5,可用于检测内容变化',
    consumed_at     DATETIME(3)  NOT NULL,
    biz_ref         VARCHAR(64)           COMMENT '业务对象 ID(订单号/支付单号),便于排查',
    PRIMARY KEY (id),
    UNIQUE KEY uk_event_consumer (event_id, consumer_group),  -- ★ 关键唯一索引
    KEY idx_biz_ref (biz_ref),
    KEY idx_consumed_at (consumed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息消费记录(Inbox)';
```

**为何按 `(event_id, consumer_group)` 联合唯一**:同一消息可能被多个消费者组(库存、积分、通知)各自消费一次,组内幂等而不影响其他组。

#### 消费实现

```java
@Component
public class PaymentSucceededListener extends AbstractRocketMqListener<PaymentSucceededEvent> {

    private final PaymentApplicationService paymentService;
    private final ConsumedEventRepository consumedEventRepository;

    public PaymentSucceededListener(RocketMqProperties props,
                                    PaymentApplicationService paymentService,
                                    ConsumedEventRepository consumedEventRepository) {
        super(props);
        this.paymentService = paymentService;
        this.consumedEventRepository = consumedEventRepository;
    }

    @Override protected String getTopic()                 { return "eagle-PaymentSucceededEvent"; }
    @Override protected String getConsumerGroup()         { return "ledger-payment-succeeded"; }
    @Override protected Class<PaymentSucceededEvent> getEventClass() { return PaymentSucceededEvent.class; }

    @Override
    @Transactional(rollbackFor = Exception.class)         // ★ 关键:Inbox 与业务同事务
    protected void handle(PaymentSucceededEvent event) {
        // ① 先记录 Inbox—— 唯一索引会拦住重复消息
        try {
            consumedEventRepository.save(ConsumedEvent.builder()
                    .eventId(event.getEventId())
                    .consumerGroup(getConsumerGroup())
                    .topic(getTopic())
                    .bizRef(event.getOutTradeNo())
                    .consumedAt(LocalDateTime.now())
                    .build());
        } catch (DataIntegrityViolationException duplicate) {
            // 重复消息:Inbox 唯一约束阻止了二次插入,业务方法一定不会重复执行
            log.warn("Duplicate payment event skipped, eventId: {}, outTradeNo: {}",
                    event.getEventId(), event.getOutTradeNo());
            return;
        }

        // ② 业务变更与 ① 在同一事务,要么一起成功,要么一起回滚
        paymentService.confirmPayment(event.getOutTradeNo(), event.getPaidAmount(), event.getChannel());
    }
}
```

**为什么必须把 Inbox 写在业务变更前**:

- 唯一索引冲突会立刻抛异常,事务还没产生业务变更,代价小
- 业务变更若放前面,重复消费时已经做了一半工作,需要靠状态机/Redisson 锁额外保护

**为什么必须用 `@Transactional`**:Inbox 写入和业务写入分别在两个事务时,若 Inbox 提交后业务回滚,下次重投会因 Inbox
已存在而被跳过,**业务永远不会做** —— 资损。

### 模式 B:状态机驱动的幂等消费

聚合根的状态字段守护"业务最多发生一次"的不变量,即便没有 Inbox 也能幂等。

```java
// 聚合根 — 状态机不变量
public class Payment extends BaseAggregateRoot<Payment> {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    /**
     * 确认支付成功。仅 PENDING → SUCCEEDED 合法,其他状态直接 no-op(幂等)。
     */
    public void markSucceeded(BigDecimal paidAmount, String channel) {
        if (status == PaymentStatus.SUCCEEDED) {
            return;                                       // ★ 已确认过,幂等跳过
        }
        if (status != PaymentStatus.PENDING) {
            throw PaymentErrorCode.ILLEGAL_STATE_TRANSITION
                    .toDomainException("Cannot mark succeeded from " + status);
        }
        this.status        = PaymentStatus.SUCCEEDED;
        this.paidAmount    = paidAmount;
        this.channel       = channel;
        this.paidAt        = LocalDateTime.now();
        registerEvent(new PaymentConfirmedEvent(getId(), getOutTradeNo()));
    }
}

// 应用服务 — 配合乐观锁
@Service
@RequiredArgsConstructor
public class PaymentApplicationService {

    private final PaymentRepository paymentRepository;

    @Transactional(rollbackFor = Exception.class)
    public void confirmPayment(String outTradeNo, BigDecimal amount, String channel) {
        Payment payment = paymentRepository.findByOutTradeNoForUpdate(outTradeNo)   // ★ 行锁
                .orElseThrow(PaymentErrorCode.PAYMENT_NOT_FOUND::toNotFoundException);
        payment.markSucceeded(amount, channel);                                     // 状态机决定是否真做
        paymentRepository.save(payment);                                            // @Version 乐观锁兜底
    }
}
```

**为什么状态机比 Inbox 更接近"领域驱动"**:幂等是业务规则的自然结果("已支付的订单不能再被支付"),而不是基础设施层的额外检查。Inbox
是兜底,**两者通常一起用**。

### 模式 C: TCC 与预留资源

当业务**横跨多个聚合或多个服务**(扣款 + 加积分 + 发优惠券)时,单纯的状态机不够。此时用 **Try-Confirm-Cancel** 三阶段:

| 阶段          | 动作                         | 失败后果                             |
|-------------|----------------------------|----------------------------------|
| **Try**     | 各方资源**预留**(冻结余额、预扣库存、预占积分) | 整体放弃 → Cancel                    |
| **Confirm** | Try 全部成功后**确认**(实际扣款、扣库存)  | 必须可重试至成功(已 Try 过资源,Confirm 一定成功) |
| **Cancel**  | Try 失败或超时,**释放**预留资源       | 必须可重试至成功(空回滚 / 悬挂检测)             |

详见 `.claude/rules/16-transaction-distributed.md`。Eagle 提供 `eagle-seata-starter` 集成 TCC 模式,RocketMQ 仅作为
Confirm/Cancel 阶段的事件广播通道。

**适用判断**:

| 业务场景                   | 推荐                                  |
|------------------------|-------------------------------------|
| 单聚合状态变更(订单状态、用户余额变更)   | 模式 A + 模式 B                         |
| 跨服务最终一致(用户支付 → 异步发积分)  | 模式 A + 事务消息(`publishInTransaction`) |
| 跨服务必须强一致(秒级)(扣款 + 扣库存) | 模式 C(TCC) + 模式 A                    |

### 系统异常 vs 业务失败 vs 永久失败

`handle()` 抛出的异常**必须分类处理**,否则永远重试或永远不重试都是灾难:

```java
@Override
@Transactional(rollbackFor = Exception.class)
protected void handle(PaymentSucceededEvent event) {
    try {
        recordInbox(event);
        paymentService.confirmPayment(event.getOutTradeNo(), event.getPaidAmount(), event.getChannel());

    } catch (DataIntegrityViolationException duplicate) {
        // ↳ 业务"幂等通过":Inbox 唯一约束已拦截,什么都不做
        log.warn("Duplicate event, skip. eventId: {}", event.getEventId());

    } catch (DomainException businessFail) {
        // ↳ 业务规则不允许(如订单状态非 PENDING):不再重试,记录到告警表
        unprocessableEventRepository.save(UnprocessableRecord.of(event, businessFail));
        // 不抛出 → ConsumeResult.SUCCESS,避免无意义重试 16 次最终进 DLQ
        // 业务侧通过运营平台决定如何处理(取消订单 / 客服介入 / 强制确认)

    } catch (TransientException transient) {
        // ↳ 系统暂时不可用(下游 RPC、DB 慢):抛出 → 触发 RocketMQ 重试
        throw transient;

    } catch (Exception unknown) {
        // ↳ 未知异常,默认抛出走重试链路。重试到阈值会告警,最终进 DLQ
        throw unknown;
    }
}
```

| 异常类                                         | 应对           | 原因                 |
|---------------------------------------------|--------------|--------------------|
| `DataIntegrityViolationException`(Inbox 冲突) | 吞掉,SUCCESS   | 幂等命中,业务已生效         |
| `DomainException` / `IllegalState`(业务规则失败)  | 持久化告警表后吞掉    | 重试无意义,等待人工         |
| `OptimisticLockingFailureException`(乐观锁冲突)  | 抛出 → 重试      | 并发短暂冲突,下次重试有效      |
| `CannotAcquireLockException`(行锁等待超时)        | 抛出 → 重试      | DB 临时压力,退避后会成功     |
| `Feign / RPC TimeoutException`(下游超时)        | 抛出 → 重试      | 临时不可达,Broker 退避后重试 |
| 其他 `RuntimeException`                       | 抛出 → 重试到 DLQ | 未识别,人工介入           |

**关键反例(必须避免)**:

```java
// ❌ 反例 1:把所有异常都吞掉 — DLQ 进不了,告警发不出,资损静默积累
catch (Exception e) { log.error("failed", e); }

// ❌ 反例 2:把所有异常都抛出 — 业务永久失败的消息浪费 16 次重试机会
catch (Exception e) { throw e; }

// ❌ 反例 3:重试前部分提交业务变更 — 退款下发了但 Inbox 没记录,重试又退一次
public void handle(...) {
    refundService.refund(...);             // 调外部网关成功
    consumedEventRepository.save(...);     // ★ 这里失败 → 下次重试再退一次款
}
```

### 人工介入闭环:DLQ 重投与对账

死信不是终点。支付场景必须有**运营后台 + 对账机制**关闭闭环:

#### DLQ 持久化 + 重投接口

```java
@Component
public class PaymentDlqListener extends AbstractDlqListener<PaymentSucceededEvent> {

    private final DeadLetterRepository deadLetterRepository;
    private final AlertService alertService;

    public PaymentDlqListener(RocketMqProperties props,
                              DeadLetterRepository deadLetterRepository,
                              AlertService alertService) {
        super(props);
        this.deadLetterRepository = deadLetterRepository;
        this.alertService = alertService;
    }

    @Override protected String getOriginalConsumerGroup() { return "ledger-payment-succeeded"; }
    @Override protected Class<PaymentSucceededEvent> getEventClass() { return PaymentSucceededEvent.class; }

    @Override
    protected void handleDeadLetter(PaymentSucceededEvent event, int totalAttempts) {
        deadLetterRepository.save(DeadLetterRecord.builder()
                .eventId(event.getEventId())
                .topic(getTopic())
                .consumerGroup(getOriginalConsumerGroup())
                .payload(JSON.toJSONString(event))
                .totalAttempts(totalAttempts)
                .status(DlqStatus.PENDING)
                .build());

        alertService.urgent("支付消息进入死信,outTradeNo=" + event.getOutTradeNo());
    }
}

// 运营后台触发的重投接口
@PostMapping("/admin/dlq/{id}/replay")
@PreAuthorize("hasRole('ops_payment')")
public void replay(@PathVariable Long id) {
    DeadLetterRecord record = deadLetterRepository.findById(id).orElseThrow(...);
    PaymentSucceededEvent event = JSON.parseObject(record.getPayload(), PaymentSucceededEvent.class);

    // 重投会被原 Listener 的 Inbox 唯一索引拦截 → 安全幂等
    domainEventPublisher.publish(record.getTopic(), event);
    record.markReplayed();
    deadLetterRepository.save(record);
}
```

#### 与上游的双向对账(关键)

支付通知**最后一道防线是对账**,不要假设 MQ 万无一失:

```java
/**
 * 对账定时任务(XXL-JOB):每 10 分钟拉取支付网关的成功流水,
 * 与本地 Payment 表比对,差异项触发补偿。
 */
@XxlJob("paymentReconciliationHandler")
public void reconcile() {
    LocalDateTime start = LocalDateTime.now().minusMinutes(30);
    LocalDateTime end   = LocalDateTime.now().minusMinutes(10);   // 容忍 10 分钟延迟

    List<GatewayTrade> gatewayTrades = paymentGateway.queryTrades(start, end);
    for (GatewayTrade gt : gatewayTrades) {
        Payment local = paymentRepository.findByOutTradeNo(gt.getOutTradeNo()).orElse(null);
        if (local == null || local.getStatus() != PaymentStatus.SUCCEEDED) {
            // 网关说付款成功,本地却没记 → 通知确实丢了 / 卡死信里 / Bug → 主动补偿
            paymentService.confirmPayment(gt.getOutTradeNo(), gt.getAmount(), gt.getChannel());
            log.warn("[RECONCILE] Payment fixed by reconciliation, outTradeNo: {}", gt.getOutTradeNo());
        }
    }
}
```

**对账是支付链路真正的"最终保证"**,MQ + Inbox + 状态机解决 99.99% 场景,剩下 0.01% 由对账兜底。**没有对账的支付消费不能上线
**。

### 完整支付通知消费样例

把以上所有要素串起来 —— 一个生产级支付成功通知消费者:

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSucceededListener extends AbstractRocketMqListener<PaymentSucceededEvent> {

    private final PaymentApplicationService paymentService;
    private final ConsumedEventRepository consumedEventRepository;
    private final UnprocessableEventRepository unprocessableEventRepository;
    private final AlertService alertService;

    @Override protected String getTopic()                          { return "eagle-PaymentSucceededEvent"; }
    @Override protected String getConsumerGroup()                  { return "ledger-payment-succeeded"; }
    @Override protected Class<PaymentSucceededEvent> getEventClass() { return PaymentSucceededEvent.class; }
    @Override protected int getRetryAlertThreshold()               { return 5; }   // 支付场景早点告警

    @Override
    @Transactional(rollbackFor = Exception.class)                  // ① Inbox + 业务同事务
    protected void handle(PaymentSucceededEvent event) {
        // ② 写 Inbox(组合幂等 key)
        try {
            consumedEventRepository.save(ConsumedEvent.builder()
                    .eventId(event.getEventId())
                    .consumerGroup(getConsumerGroup())
                    .topic(getTopic())
                    .bizRef(event.getOutTradeNo())
                    .consumedAt(LocalDateTime.now())
                    .build());
        } catch (DataIntegrityViolationException duplicate) {
            log.warn("Duplicate payment event skipped, outTradeNo: {}", event.getOutTradeNo());
            return;                                                // 幂等命中,业务已生效
        }

        // ③ 业务处理(状态机内自动忽略已是 SUCCEEDED 的支付)
        try {
            paymentService.confirmPayment(event.getOutTradeNo(), event.getPaidAmount(), event.getChannel());

        } catch (DomainException businessFail) {
            // ④ 业务规则不允许 — 不再重试,记录待人工
            unprocessableEventRepository.save(UnprocessableRecord.of(event, businessFail));
            alertService.warn("支付消息无法处理,需人工:" + event.getOutTradeNo() + ", reason=" + businessFail.getMessage());
            // 注意:这里不抛出,事务正常提交,Inbox 也保留 → 重试不会再来
            //       但因为业务没变更,需要靠告警 + 人工 / 对账兜底
        }
        // ⑤ 其他异常(乐观锁、行锁、RPC 超时...)向上抛出 → RocketMQ 重试
    }

    @Override
    protected void onRetryAlert(MessageView mv, String body, PaymentSucceededEvent event, Exception cause) {
        alertService.urgent("【支付消费持续失败】outTradeNo=" + (event != null ? event.getOutTradeNo() : "?")
                + ", attempt=" + mv.getDeliveryAttempt()
                + ", error=" + cause.getMessage());
    }
}
```

**配套组件清单**:

- ✅ `t_consumed_event`(Inbox 表)+ 唯一索引 `(event_id, consumer_group)`
- ✅ `Payment` 聚合根状态机 + `@Version` 乐观锁
- ✅ `PaymentSucceededDlqListener` 死信持久化
- ✅ `paymentReconciliationHandler` 定时对账(10 分钟窗口)
- ✅ 告警通道(`onRetryAlert` + 死信告警 + 业务失败告警 + 对账差异告警)
- ✅ 运营后台:DLQ 列表 + 一键重投 + 待处理业务列表

**支付级消费"必达"的完整公式**:

```
必达 = 至少一次投递(MQ 提供)
     × 幂等(Inbox 唯一约束 + 状态机)
     × 原子(@Transactional 同事务)
     × 异常分类(系统异常重试 / 业务失败转人工)
     × 死信兜底(持久化 + 告警 + 重投)
     × 对账兜底(上游主数据双向核对)
```

**任意一项缺失,资损就是时间问题。**

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
