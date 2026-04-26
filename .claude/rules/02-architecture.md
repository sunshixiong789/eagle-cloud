# 代码分层规范（DDD 架构）

适用于模块化单体（Spring Modulith）和微服务架构。


模块边界治理方式：
- **单体架构**：Spring Modulith `@ApplicationModule` + 架构测试自动验证
- **微服务架构**：独立部署单元 + API 契约（Feign/gRPC）+ 消息队列事件

## 跨域依赖核心原则

### 原则一：出站端口（Driven Port）定义在领域层，适配器在基础设施层实现

采用**六边形架构（Ports & Adapters）**。域需要外部提供数据时（如查询授权信息），在**自身的 `domain/port/`** 定义接口，由**本模块或外部模块的 `infrastructure/` 层**提供实现（Driven Adapter）。

```
依赖方向示例（auth 需要授权信息）：
  auth/domain/port/AuthorizationPort（接口，auth 定义）
        ↑ 实现
  system/infrastructure/security/AuthorizationAdapter（适配器，system 实现）
  ⟹ system 依赖 auth::port，auth 对 system 零依赖
```

```java
// ✅ auth 定义出站端口，system 在 infrastructure/security/ 提供适配器实现
public interface AuthorizationPort {
    Optional<AuthorizationInfo> findAuthorizationInfo(Long accountId);
}

// ❌ 禁止直接依赖另一个域的 Repository
import com.example.system.domain.repository.UserRepository;
```

**微服务拆分路径**：auth 提取为独立服务时，在 `auth/infrastructure/remote/` 新增远程适配器（HTTP/gRPC 客户端）实现同一 `AuthorizationPort` 接口即可，领域层零改动。

**禁止直接依赖另一个域的 domain 层**（聚合根、Repository、领域服务）。

### 原则二：禁止跨域直接操作聚合根

每个聚合根只能由其所属域的应用服务管理。其他域需要触发操作时：

```
方式一（事件驱动）：域 A 发布事件 → 消息队列（JSON）→ 域 B 监听并处理
方式二（端口调用）：域 A 在 domain/port/ 定义接口 → 域 B 基础设施层实现适配器
```

跨域事件通过 **JSON 序列化**在消息队列中传递，消费方按需解析，无需共享 Java 事件类：

```java
// ✅ 正确：本域定义跨域事件，JSON 解耦，不依赖 common
package com.example.order.domain.event.integration;
public record OrderPaidEvent(Long orderId, BigDecimal amount) {}

// ✅ 正确：消费方不依赖事件类，直接解析 JSON 或按需反序列化
```

## 每个模块内的 DDD 分层

```
{module}/
├── interfaces/                         # Presentation layer
│   ├── rest/                           # REST 入口
│   │   └── controller/                 # REST 控制器
│   ├── rpc/                            # Dubbo / gRPC 入口（预留）
│   └── dto/                            # 入参 / 出参 DTO（Bean Validation）
│       ├── request/
│       └── response/
├── application/                        # Application layer
│   ├── service/                        # 应用服务（用例编排，事务边界）
│   ├── command/                        # CQRS 命令对象（写模型入参）
│   ├── query/                          # CQRS 查询对象（读模型入参）
│   ├── assembler/                      # DTO ↔ 领域对象装配器（MapStruct）
│   └── port/                           # 入站端口（Driving Port，供 interfaces 调用）
├── domain/                             # Domain layer（纯业务，无框架依赖）
│   ├── model/
│   │   ├── aggregate/                  # 聚合根（有独立 Repository）
│   │   ├── entity/                     # 聚合内子实体（无独立 Repository）
│   │   ├── valueobject/                # 值对象
│   │   └── enums/                      # 领域枚举
│   ├── repository/                     # Repository 接口 + 投影接口（CQRS）
│   ├── service/                        # 领域服务接口（跨聚合业务规则）
│   ├── event/
│   │   ├── domain/                     # 本地领域事件（仅本域消费）
│   │   └── integration/                # 跨域集成事件（通过 JSON / MQ 传递）
│   └── port/                           # 出站端口接口（Driven Ports，六边形架构）
│                                       # ← 由 infrastructure/ 层实现
└── infrastructure/                     # Infrastructure layer
    ├── persistence/                    # 数据访问（强制收敛）
    │   └── repository/                 # Repository 实现（JPA / MyBatis）
    ├── messaging/                      # 消息队列（MQ）
    │   ├── producer/                   # 事件生产者
    │   └── consumer/                   # 事件消费者
    ├── remote/                         # 外部服务调用（Feign / HTTP / gRPC）
    ├── scheduler/                      # 定时任务（XXL-JOB / Spring Scheduler）
    ├── event/                          # 事件分发（domain → integration）
    ├── service/                        # 领域服务实现
    ├── config/                         # 技术配置（Properties 等）
    └── security/                       # 安全适配器
```

**分层依赖方向（单向）：** `interfaces → application → domain ← infrastructure`

## Driving Port 与 Driven Port

| 端口类型 | 位置 | 方向 | 作用 |
|----------|------|------|------|
| **Driving Port**（入站端口）| `application/port/` | 外部 → 应用层 | 定义表现层如何驱动应用，由应用服务实现 |
| **Driven Port**（出站端口）| `domain/port/` | 应用层 → 外部 | 定义领域需要什么外部能力，由基础设施实现 |

### 松散做法 vs 严格六边形

**❌ 松散做法**：Controller 直接注入 `OrderApplicationService` 实现类，导致：
- interfaces 层与 application 层强耦合
- 新增 MQ / gRPC 入口时无法复用同一用例
- 测试 Controller 需 mock 整个 Service

**✅ 严格六边形**：`application/port/` 按**用例分组**定义 Driving Port 接口（如 `OrderCommandPort`、`OrderQueryPort`），由 `application/service/` 实现。REST / RPC / MQ 等所有外部入口统一依赖 Port 接口，不感知实现类。

### 依赖关系

```
interfaces/
├── rest/controller/      ──┐
├── rpc/GrpcService       ──┼──► application/port/OrderCommandPort
└── messaging/consumer    ──┘         OrderQueryPort
                                              ▲
                                              │
application/service/OrderCommandService implements OrderCommandPort
application/service/OrderQueryService     implements OrderQueryPort
              │
              ▼
      domain/port/InventoryValidationPort ◄── infrastructure/remote/InventoryAdapter
```

### 应用层 Service 全部抽象为 Port

| 位置 | 内容 | 命名示例 |
|------|------|----------|
| `application/port/` | Driving Port 接口（按用例分组） | `OrderCommandPort`、`OrderQueryPort`、`UserManagePort` |
| `application/service/` | 接口实现（用例编排 + 事务） | `OrderCommandService`、`OrderQueryService` |

## 聚合根规范

**判断标准：**

| 标准 | 聚合根（BaseAggregateRoot）| 子实体（BaseEntity）|
|------|--------------------------|-------------------|
| 有独立 Repository | ✅ | ❌ |
| 可被其他聚合引用 | ✅（通过 ID）| ❌ |
| 有独立业务生命周期 | ✅ | ❌ |

**跨聚合引用只存 ID，禁止 JPA 关联注解跨聚合：**

```java
// ✅ 正确：存 ID 集合
@ElementCollection(fetch = FetchType.LAZY)
@CollectionTable(name = "t_role_permission", joinColumns = @JoinColumn(name = "role_id"))
@Column(name = "permission_id")
private Set<Long> permissionIds = new HashSet<>();

// ❌ 错误：@ManyToMany 跨聚合关联
@ManyToMany
private List<Permission> permissions;
```

**子实体规范：**
- 只有聚合根才能拥有 Repository，子实体禁止创建独立 Repository
- 子实体增删改必须通过聚合根的业务方法进行（级联管理）
- 子实体 API 使用嵌套资源路径：`/api/{root}/{rootId}/{child}`

## 聚合根创建型事件发布规范

聚合根创建时需要发布跨域事件的，**事件注册必须在工厂方法内完成**，由 `@PostPersist` 回调在 ID 分配后自动触发，禁止在应用服务中手动调用事件发布方法。

**原因**：`GenerationType.IDENTITY` 策略下，ID 由数据库 INSERT 后分配。若在 `save()` 前构建事件，`getId()` 为 null，导致事件携带错误数据。

**做法**：

1. 将事件所需的非聚合根自身字段封装为值对象（如 `ProfileHints`），通过工厂方法参数传入，存为 `@Transient` 字段
2. 使用 `@PostPersist` 回调构建并注册事件（此时 ID 已可用），注册后清除瞬态字段
3. 应用服务只需一次 `save()` 即可完成持久化 + 事件发布

```java
// ✅ 工厂方法接收 hints，@PostPersist 在 INSERT 后自动注册事件
public static Order create(String orderNo, ProfileHints hints) {
    Order order = new Order();
    order.orderNo = orderNo;
    order.profileHints = hints;  // @Transient
    return order;
}

@PostPersist
private void onPostPersist() {
    if (profileHints != null) {
        registerEvent(new OrderCreatedEvent(getId(), orderNo, profileHints.xxx()));
        profileHints = null;
    }
}

// 应用服务只需一次 save()
orderRepository.save(Order.create(orderNo, hints));
```

**注意**：`@PostPersist` 仅在 INSERT 时触发，UPDATE 时不会重复发布。对于非创建场景的事件（如删除），因聚合根已有 ID，可直接在业务方法中 `registerEvent()`。

## 事件架构

```
domain/event/domain/         # 本地事件（仅本域消费）
domain/event/integration/    # 跨域事件（JSON 解耦，MQ 传递）
infrastructure/event/        # 事件分发器：domain → integration
infrastructure/messaging/    # MQ 生产者和消费者
```

```java
// domain/event/domain/     — 本地事件（仅本域消费）
// domain/event/integration/ — 跨域事件（JSON 解耦，MQ 传递）

// infrastructure/event/ — 事件分发器：事务提交后 domain → integration
@Component
public class OrderEventDispatcher {
    @Async
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void onOrderPaid(OrderPaidEvent event) {
        messagingProducer.send("order-topic", JsonUtils.toJson(event));
    }
}
```

## 跨聚合操作

跨聚合的业务校验放在**领域服务**中，接口定义在 domain 层，实现在 infrastructure 层：

```java
// domain/service/ — 接口
public interface InventoryValidationService {
    void validateStock(Long productId, int quantity);
}

// 应用服务中使用
inventoryValidationService.validateStock(productId, qty);  // 跨聚合校验
order.addItem(productId, qty);                              // 聚合内操作
```

## CQRS 读模型分离

列表查询使用投影接口，避免加载完整聚合根：

```java
// 投影接口（只取必要字段）
public interface OrderSummary {
    Long getId();
    String getOrderNo();
    BigDecimal getTotalAmount();
}

// Repository 用 @Query 映射到投影
@Query("SELECT o.id AS id, o.orderNo AS orderNo, o.totalAmount AS totalAmount FROM Order o")
Page<OrderSummary> findOrderSummaries(Pageable pageable);
```

## 微服务拆分就绪

模块化单体设计天然为微服务拆分做好准备：

| 单体时 | 拆分后 | 改动范围 |
|--------|--------|---------|
| `domain/port/` 接口 + 本地 Adapter | 远程 Adapter（HTTP/gRPC 客户端）实现同一接口 | **仅替换 infrastructure/remote/ 实现** |
| `domain/event/integration/` 跨域事件 + `messaging/producer/` | 消息队列事件契约（JSON），消费方按需解析 | 领域层不变 |
| Spring Modulith `verify()` | 独立部署单元天然隔离 | 持续保证边界 |

**六边形架构的核心价值**：领域层（`domain/port/` 接口 + `domain/event/` 事件）稳定不变，只需在对应服务的 `infrastructure/` 层替换实现，业务代码零改动。
