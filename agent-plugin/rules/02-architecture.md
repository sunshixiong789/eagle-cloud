# 架构：DDD 分层 + Modulith 边界 + 事件驱动

## 依赖方向

```text
interfaces -> application -> domain <- infrastructure
```

| 层 | 职责 |
|---|---|
| `interfaces` | Controller、request/response DTO（`record`）、Bean Validation |
| `application` | 用例编排、事务边界、DTO ↔ 领域映射 |
| `domain` | 聚合根、子实体、值对象、Repository 接口、领域服务接口、事件、Port |
| `infrastructure` | JPA、远程调用、MQ、缓存、配置、安全、定时任务等适配器 |

实际目录（`eagle-auth-service` / `com.eagle.auth.core`）：

```text
core/
├── interfaces/{controller,dto}/
├── application/{command,service,mapper}/
├── domain/{model,repository,service,event,port}/
└── infrastructure/{adapter,cache,config,external,messaging,remote,security}/
```

## 服务与模块实况

**auth 已从模块化单体拆为独立服务**，不再是 `com.eagle.system.auth` 子模块。当前 `@ApplicationModule` 声明：

| 模块 | 所属服务 | allowedDependencies |
|---|---|---|
| `com.eagle.system.base` | eagle-system-service | 未声明（默认全开） |
| `com.eagle.system.file` | eagle-system-service | 未声明 |
| `com.eagle.system.message` | eagle-system-service | `{}`（完全隔离） |
| `com.eagle.auth.core` | **eagle-auth-service** | `{}`（完全隔离） |

由此推出两条：

- **system ↔ auth 是跨服务调用**，走 HTTP client（`infrastructure/remote/`）+ 集成事件，**不是** `auth::port` 这类 Named Interface
- `message` 与 `auth.core` 已声明 `{}`，**新增任何跨模块 import 都会让架构测试失败** —— 需要协作就加 Port 或走事件，不要去放宽 `allowedDependencies`

## 跨域协作（只有两条合法路径）

1. **Port + Adapter** —— 出站端口定义在**调用方** `domain/port/`，由对方 `infrastructure/adapter/` 实现
2. **领域事件** —— 定义在**发布方** `domain/event/`，`@NamedInterface("event")` 暴露，订阅方在 `allowedDependencies` 声明

**禁止**直接依赖其他域的聚合根、Repository、领域服务实现或内部包。

## Modulith 声明

```java
// 模块根包 package-info.java
@ApplicationModule(displayName = "站内消息模块", allowedDependencies = {})
@NullMarked
package com.eagle.system.message;

// 对外暴露的子包
@NamedInterface("port")
package com.eagle.order.domain.port;
```

架构验证是**纯静态分析**（不启动 Spring），PR 前必跑：

```bash
gradle :eagle-services:eagle-system-service:test --tests "*ModulithArchitectureTest"
```

测试失败时：依赖**合理** → 被依赖包加 `@NamedInterface` + 依赖方声明；依赖**错误** → 用 Port/Adapter 重构。

## 聚合根

```java
@Entity
@Getter
@NoArgsConstructor
@Table(name = "auth_account", indexes = { ... })
public class Account extends BaseAggregateRoot<Account> {

    @Enumerated(EnumType.STRING)
    private AccountStatus status = AccountStatus.ACTIVE;

    /** 静态工厂创建 */
    public static Account register(String phone) { ... }

    /** 业务方法改状态 + 注册事件，不暴露 setter */
    public void freeze(String reason) {
        this.status = AccountStatus.FROZEN;
        registerEvent(new AccountFrozenEvent(getId(), reason));
    }
}
```

- 聚合根继承 `BaseAggregateRoot<T>`，子实体继承 `BaseEntity`
- 子实体增删改必须经聚合根业务方法
- 跨聚合**只存 ID**，不建对象引用、不建物理外键（见 `04-data.md`）

## DTO 映射

两种写法，按复杂度选：

| 情况 | 写法 |
|---|---|
| 单聚合、字段直取 | DTO `record` 内静态工厂 `static XxxResponse of(Domain d)` |
| 跨聚合 / 多来源 / 需要查字典 | `application/mapper/` 下 `@Component` Mapper（现有 8 个） |

- Mapper 方法名 `toResponse / toDto / toDomain`；入参 `null` 返回 `null`
- **字段逐行显式映射**；枚举输出 String 用 `.name()`
- 批量转换由调用方 `stream().map(mapper::toResponse).toList()`
- Mapper **不访问** Repository / Service，不做跨聚合查询和业务判断

**禁止 MapStruct、ModelMapper、`BeanUtils.copyProperties`。**

## CQRS

写模型走聚合根 + Repository；读模型用 Repository 投影接口、只读 QueryPort 或独立查询服务。复杂读查询不得污染聚合根业务方法。

---

# 事件驱动

## 事件分类

| 类型 | 作用域 | 载体 | 位置 |
|---|---|---|---|
| 领域事件 | 单域内 | Spring `ApplicationEvent` | `{module}/domain/event/` |
| 集成事件（生产方） | 跨服务 | MQ（JSON） | `{producer}/infrastructure/messaging/XxxIntegrationEvent.java` |
| 集成事件（消费方） | 跨服务 | MQ（JSON） | `{consumer}/infrastructure/messaging/XxxMessage.java` —— **各消费方独立声明** |

## 发布时机：由聚合根发出

```java
// ✅ 业务方法内注册
public void pay(BigDecimal amount) {
    this.status = OrderStatus.PAID;
    registerEvent(new OrderPaidEvent(getId(), amount));
}

// ✅ 创建型事件：ID 尚未分配，用 @PostPersist 延迟注册
@PostPersist
private void onPostPersist() {
    if (profileHints != null) {
        registerEvent(new OrderCreatedEvent(getId(), orderNo));
        profileHints = null;
    }
}
```

**禁止**应用服务 / Controller 手动 `publishEvent()`；也**禁止**在 `@Transactional` 中直接 `ApplicationEventPublisher.publishEvent()` —— 用 `registerEvent()`，Spring Data 在 `save()` 后自动发布。

事件载荷用 `record`，只含跨域消费所需的最小字段，**禁止持有完整聚合根**。同一聚合的事件层级可用 `sealed interface` 收口（见 `01-java25.md`）。

## 事件处理器

```java
// ✅ 同域：事务提交后异步
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onOrderPaid(OrderPaidEvent event) { ... }

// ✅ 跨域/跨服务：额外开独立事务，避免级联回滚
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void onAccountRegistered(AccountRegisteredEvent event) { ... }
```

## 集成事件契约：字段名是唯一契约

生产方与**每个**消费方在各自模块内独立声明类，靠 JSON 字段名兼容：

```java
// ✅ 生产方
public class OrderPaidIntegrationEvent extends BaseEvent {
    private String eventVersion;   // "1.0"
    private Long orderId;
    private BigDecimal amount;
}

// ✅ 消费方——只声明本模块用得到的字段子集
public class OrderPaidMessage extends BaseEvent {
    private Long orderId;
    private BigDecimal amount;
}
```

- **禁止**消费方 `import` 生产方的 `XxxIntegrationEvent`
- **禁止**抽 "shared-events.jar" 或 `common/integration/` 共享类
- 取舍理由与风险缓解见 [ADR-0003](../../docs/adr/0003-consumer-declares-own-event-class.md)；
  字段改名由 `docs/contracts/*.json` 契约测试兜底（生产方 auth-service + 消费方 system-service 双向）
- 新增字段直接加；**删除 / 重命名 / 改类型 → 升 `eventVersion` 并灰度**（双发 → 切换 → 下线），过渡期 ≥ 3 个月

## Topic / Tag

Topic 用**下划线**分隔：`{prefix}_{domain}_{event}`，如实际在用的 `eagle_auth_events`。Tag 用业务动作：`created` / `paid` / `cancelled`。消息体**禁止**含密码、Token、密钥、完整证件号。

## 幂等

幂等 key 用 `BaseEvent.eventId`（时间有序 UUID），**不用** MQ 的 `MsgId`（重投递会变）。

优先级：创建型 → 业务表唯一约束；状态机推进 → 条件 UPDATE + 二次 SELECT；累加型 → 业务事实表记 eventId；纯副作用 → Redis SETNX 守卫。

**禁止**用通用 inbox 表替代业务幂等。

## Saga

多步骤跨聚合流程用**编排器**统一控制步骤与补偿，**禁止**用链式 `@EventListener`（状态难追踪、补偿无保证）。超时未收敛的 Saga 由定时补偿任务扫描处理。

Event Sourcing 仅用于需完整历史审计的少数聚合，**禁止**全业务铺开。

## 禁止清单

- 跨服务直接消费对方内部领域事件（必须转集成事件）
- 事件处理器内再次触发同一事件（无限循环）
- 事件处理器非幂等
- 死信静默吞掉（必须有 `AbstractDlqListener` + 告警）
