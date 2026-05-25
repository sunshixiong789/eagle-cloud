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

格式：`{service}_{domain}_events`（**下划线**分隔，全小写）：

```
auth_account_events       # auth-service 账号生命周期事件
order_order_events        # order-service 订单生命周期事件
payment_payment_events    # payment-service 支付事件
system_user_events        # system-service 用户域事件
```

- `service` — 发布方服务名（**必须**带，避免不同服务相同 domain 撞名）
- `domain` — 业务域（`account / order / payment / user`）
- `events` — 固定后缀，标识这是事件 topic

> **为什么是下划线不是点号**：RocketMQ 5.x gRPC 客户端（`rocketmq-client-java`）在
> `MessageBuilderImpl.setTopic()` 强制校验 topic 必须匹配正则 `^[%a-zA-Z0-9_-]+$`，
> **不允许点号 `.`**。违反会在 producer 发消息时直接抛 `IllegalArgumentException:
> topic does not match the regex`，导致跨域事件链路彻底瘫痪。

**Tag** 用过去时动词区分子事件类型（tag 只禁止 `|`，允许点号，可用点号细分子类型）：

```
topic: auth_account_events
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
        publisher.publish("notification_admin_events", "alert", event);
    }
}

// ✅ 事务消息（推荐用于聚合根写库 + 发消息的强保证场景）
publisher.

publishInTransaction(
    "order_order_events",
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
    publisher.publish("order_order_events", "created",
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

### 契约演进与文档化

不共享 Java 类 = 没有"IDE 跳转到所有消费方"的能力，必须靠**文档+流程**补齐：

**1) 每个 topic 一份 schema 文档**

在仓库的 `docs/events/{topic}.md` 维护字段表 + 版本 + 已知消费方清单：

```markdown
# order_order_events

## tag: created (v1.2)
| 字段              | 类型         | 必填 | 版本    | 说明           |
|-----------------|------------|----|-------|--------------|
| eventId         | String     | Y  | v1.0  | UUID v7      |
| occurredOn      | DateTime   | Y  | v1.0  |              |
| orderId         | Long       | Y  | v1.0  |              |
| orderNo         | String     | Y  | v1.0  |              |
| amount          | BigDecimal | Y  | v1.0  |              |
| couponCode      | String     | N  | v1.2  | 新增；旧消费方可忽略 |

## 已知消费方
- stock-service / OrderCreatedConsumer（订阅: created, deleted）
- ledger-service / OrderCreatedConsumer（订阅: created）
- notification-service / OrderEventConsumer（订阅: created, paid, cancelled）
```

生产方加 / 改 / 废弃字段**必须同 PR 更新文档**，CI 校验文档与 producer 的字段不一致直接 fail。

**2) 字段兼容性铁律**

| 变更类型      | 是否允许      | 流程                                |
|-----------|-----------|-----------------------------------|
| 新增非必填字段   | ✅         | 直接发，旧消费方天然兼容                      |
| 新增必填字段    | ❌         | 改为新增非必填 + 兼容期 + 后续升 v2            |
| 修改字段类型    | ❌         | 必须升 v2，双发，迁移完下线 v1                |
| 重命名字段     | ⚠️ 双名期    | 至少 1 个 release 同时序列化新旧两个名，迁移完再下线旧名 |
| 删除字段      | ⚠️ 灰度     | 文档先标 `@Deprecated since vX.Y`，下线条件：所有消费方确认不读 → 双发 N 个 release → 下线 |
| 改字段语义（值域） | ❌         | 当作"删除旧字段 + 新增新字段"处理               |

**3) 下线确认机制**

废弃字段下线前，生产方在文档 PR 中 **@ 所有已知消费方** 确认；消费方在 PR 上回复"已验证不读 + 版本号"才可下线。

无 schema 注册中心时这是兜底流程；规模 > 10 个消费方建议引入 schema 注册（Avro / Protobuf + 中心校验）。

## 消费者（继承 AbstractRocketMqListener）

`eagle-rocketmq-starter` 使用 RocketMQ 5.x **原生客户端 API**（`PushConsumer` + `MessageView`），消费者**不使用**
`@RocketMQMessageListener` 注解，而是继承 `AbstractRocketMqListener<T>` 实现 3 个抽象方法：

```java
// ✅ 标准消费者：泛型指向**本模块**声明的 OrderCreatedMessage（不是生产方的 IntegrationEvent）
@Component
public class OrderCreatedConsumer
        extends AbstractRocketMqListener<OrderCreatedMessage> {

    private final StockApplicationService stockService;

    public OrderCreatedConsumer(RocketMqProperties props,
                                StockApplicationService stockService) {
        super(props);
        this.stockService = stockService;
    }

    @Override
    protected String getTopic() {
        return "order_order_events";
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
        // 幂等下沉到 ApplicationService（业务表唯一约束 / 状态机）。
        // 副作用型场景（外部 API / 短信 / 推送）由 ApplicationService 内部用 Redis SETNX 前置守卫。
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

消费者**必须**实现幂等，防止重复消费。**幂等由消费方业务层（ApplicationService）保证**，Consumer 只反序列化与转发，不写事件级去重。具体机制按场景选型：

### 选型表

| 场景                              | 幂等机制                                | 真相源       |
|---------------------------------|-------------------------------------|-----------|
| 创建型（订单、入账、关系映射）                 | 业务表唯一约束 + catch 后**匹配冲突键值**         | 业务表       |
| 状态机推进                           | 条件 UPDATE + 0 行后**二次 SELECT 区分原因**  | 业务表       |
| 累加型（计数器、累计金额、会员天数）              | 业务专属事实表（带业务语义的幂等键）+ 同事务更新累加器        | 事实表       |
| **副作用型（外部 API / 短信 / 推送 / 写第三方）** | **Redis SETNX(`eventId`) 前置守卫**     | Redis 幂等键 |
| 高吞吐读多写少（缓存预热、读模型同步）             | Consumer 层 Redis 短 TTL 缓存快速过滤 + 业务表兜底 | 业务表 + 缓存   |

**核心原则：** 优先业务表（单一真相 + 复用事务边界）；副作用型操作没有业务表可挂时必须用 Redis SETNX，**不是"罕见兜底"，是必备**。

### Mode A — 创建型（业务表唯一约束）

```java
@Table(uniqueConstraints = @UniqueConstraint(
    name = "uk_entity_source", columnNames = {"user_id", "source_type", "source_ref_id"}))
public class Entity { ... }

// ApplicationService:
try {
    repo.save(Entity.create(userId, sourceType, sourceRefId, ...));
} catch (DataIntegrityViolationException e) {
    // ⚠️ 必须确认冲突来自幂等键，而不是其他唯一约束（避免吞掉业务并发异常）
    if (repo.existsByUserIdAndSourceTypeAndSourceRefId(userId, sourceType, sourceRefId)) {
        log.info("idempotent skip, userId={}, sourceRefId={}", userId, sourceRefId);
        return;
    }
    throw e;   // 是别的约束冲突（如业务并发抢同名资源），继续抛出
}
// 其它必要写入(同事务)...
```

**禁止**裸 catch + return —— 会把"两个用户抢同名资源"等业务并发冲突静默吞掉。

### Mode B — 状态机推进（条件 UPDATE + 二次 SELECT）

```java
int updated = repo.updateStatusIfCurrentlyEquals(id, OLD, NEW);
if (updated == 0) {
    // ⚠️ 0 行可能是"已转换过"也可能是"记录不存在"，必须区分
    Status current = repo.findStatusById(id)
        .orElseThrow(() -> OrderErrorCode.ORDER_NOT_FOUND.toNotFoundException());
    if (current == NEW) {
        log.info("idempotent skip, id={}, alreadyAt={}", id, NEW);
        return;
    }
    // current 既不是 OLD 也不是 NEW —— 状态机异常，必须抛出
    throw OrderErrorCode.ILLEGAL_STATUS_TRANSITION.toDomainException();
}
```

**禁止** `if (updated == 0) return;` 一刀切 —— 会静默吞掉"找不到记录""非法状态跳转"等真实错误。

### Mode C — 累加型（业务专属事实表）

直接 `counter += 1` 无法幂等。建**业务专属事实表**挂幂等键 + 同事务更新累加器：

```java
// 1) FactLog 实体：(user_id, source, source_ref_id) UNIQUE，字段带业务语义
// 2) ApplicationService:
try {
    factLogRepo.save(FactLog.create(userId, source, sourceRefId, amount, ...));
} catch (DataIntegrityViolationException e) {
    if (factLogRepo.existsByUserIdAndSourceAndSourceRefId(userId, source, sourceRefId)) {
        return;
    }
    throw e;
}
aggregate.accumulate(amount);          // 同事务
aggregateRepo.save(aggregate);
```

**注意**：业务专属事实表 ≠ 通用 inbox 表。前者带业务语义（user_id / source_ref_id / amount...），可用于业务查询与对账；后者只挂 `event_id`，纯防重复且与业务表割裂。

### Mode D — 副作用型（Redis SETNX 前置守卫）

外部 API / 短信 / 推送 / 写第三方账户等**没有业务表可挂唯一约束**的操作，由 ApplicationService 内部用 Redis SETNX 保护：

```java
// ApplicationService:
String key = "eagle:mq:idempotent:" + eventId;
Boolean first = redisTemplate.opsForValue()
        .setIfAbsent(key, "1", Duration.ofDays(1));
if (Boolean.FALSE.equals(first)) {
    log.info("idempotent skip, eventId={}", eventId);
    return;
}
smsClient.send(phone, content);   // 副作用调用
```

- 幂等 Key **必须**用消息自带的 `eventId`（`BaseEvent.eventId`，UUID v7），**不**用 MQ `MsgId`（重投递会变）
- TTL 取消息可能重投递的最长窗口（RocketMQ 默认 16 次重试 + 死信，1 天足够）
- Redis 故障降级时副作用可能被重复执行，关键路径需要下游接口本身也支持幂等（如外部 API 的 `Idempotency-Key` 头）

### Consumer 编码约定

幂等下沉到 ApplicationService 后，Consumer/DlqListener **不写**事件级去重：

```java
@Override
protected void handle(SomeMessage event) {
    appService.doSomething(event.getEventId(), event.getUserId(), event.getSourceRefId(), ...);
}
```

`eventId` 作为参数透传到 ApplicationService，由后者按场景选择 Mode A/B/C/D。

### 反模式：通用 inbox 表

**禁止**建一张 `xxx_mq_inbox(event_id, consumer_group)` 通用去重表挡所有事件 ——

- 两份真相（inbox 表 + 业务表）需要跨表事务协调
- 与业务无关的 boilerplate，Consumer 每条消息多一次 DB 往返
- 业务对账 / 排障时 inbox 表完全无用（只有 event_id，没有业务语义）

**例外**：极少数完全无业务表也无 Redis 的场景，可以考虑——但实际遇到的概率几乎为 0，先确认 Mode A-D 都不适用再说。

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
- 禁止消费方 `import` 生产方的 `XxxIntegrationEvent` 类（必须各自声明 `XxxMessage`，靠 JSON 字段名兼容）
- 禁止删除或重命名已发布字段不走"双名期 / 灰度下线"流程
- 禁止生产方加 / 改字段不同步更新 `docs/events/{topic}.md`
- 禁止裸 `catch (DataIntegrityViolationException) { return; }` —— 必须匹配冲突键值，否则会吞业务并发异常
- 禁止 Mode B 中 `if (updated == 0) return;` 不二次 SELECT 区分原因
- 禁止副作用型操作（外部 API / 短信 / 推送）不加 Redis SETNX 守卫
- 禁止建通用 `xxx_mq_inbox(event_id, consumer_group)` 表挡所有事件
- 禁止 Consumer 内写事件级去重逻辑（幂等下沉到 ApplicationService）
- 禁止生产环境 ConsumerGroup 与开发环境共用
