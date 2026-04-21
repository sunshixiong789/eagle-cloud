# 代码分层规范（DDD 架构）

适用于模块化单体（Spring Modulith）和微服务架构。

## 有界上下文（Bounded Context）

项目按业务域划分为多个有界上下文 + 支撑模块：

```
{root-package}
├── {domain-a}/    # 业务域 A
├── {domain-b}/    # 业务域 B
├── config/        # 全局配置（粘合层）— Security、Cache、Async 等
└── common/        # 共享内核（Shared Kernel）— 异常体系、错误码、基类、跨域事件
```

模块边界治理方式：
- **单体架构**：Spring Modulith `@ApplicationModule` + 架构测试自动验证
- **微服务架构**：独立部署单元 + API 契约（Feign/gRPC）+ 消息队列事件

## 跨域依赖核心原则

### 原则一：出站端口（Driven Port）定义在领域层，适配器在基础设施层实现

采用**六边形架构（Ports & Adapters）**。域需要外部提供数据时（如查询授权信息），在**自身的 `domain/port/`** 定义接口，由**外部模块的 `infrastructure/` 层**提供实现（Driven Adapter）。

```
依赖方向示例（auth 需要授权信息）：
  auth/domain/port/AuthorizationPort（接口，auth 定义）
        ↑ 实现
  system/infrastructure/security/AuthorizationAdapter（适配器，system 实现）
  ⟹ system 依赖 auth::port，auth 对 system 零依赖
```

```java
// ✅ 正确：auth 领域层定义出站端口
// auth/domain/port/AuthorizationPort.java
public interface AuthorizationPort {
    Optional<AuthorizationInfo> findAuthorizationInfo(Long accountId);
}

// ✅ 正确：system 基础设施层提供适配器实现
// system/infrastructure/security/AuthorizationAdapter.java
@Service
public class AuthorizationAdapter implements AuthorizationPort {
    // 查询 system 域的 User、Dept、Role
}

// ✅ 正确：auth 内部通过 Spring DI 注入，不感知 system 实现
// auth/infrastructure/adapter/EagleUserDetailsServiceImpl.java
private final AuthorizationPort authorizationPort;  // 注入的是 AuthorizationAdapter

// ❌ 错误：auth 直接依赖 system 的 service 或 repository
import com.example.system.domain.repository.UserRepository;  // 跨越有界上下文！
```

**微服务拆分路径**：auth 提取为独立服务时，在 **auth 基础设施层**新增远程适配器（HTTP/gRPC 客户端），实现同一个 `AuthorizationPort` 接口，无需修改领域层任何代码。

```java
// 单体 → 微服务：只需在 auth/infrastructure/ 新增，替换注入
@Service
@Profile("microservice")
public class RemoteAuthorizationAdapter implements AuthorizationPort {
    // HTTP/gRPC 调用 system 服务
}
```

**禁止直接依赖另一个域的 domain 层**（聚合根、Repository、领域服务）：

```java
// ❌ 错误：直接注入另一个域的 Repository
private final OtherModuleRepository otherRepo;  // 跨越有界上下文边界！

// ❌ 错误：直接构造另一个域的聚合根
OtherAggregate entity = OtherAggregate.create(...);
otherRepo.save(entity);
```

### 原则二：跨域事件必须放在共享模块（common）

事件类是跨域通信的契约，**不能定义在发布方或消费方的 domain.event 包中**，必须放在 `common.event` 包：
- 发布方和消费方都不直接依赖对方
- 拆分为微服务时，事件契约可独立抽取为共享 JAR / Schema Registry

```java
// ✅ 正确：跨域事件放在 common
package com.example.common.event;
public record OrderPaidEvent(Long orderId, BigDecimal amount) {}

// ❌ 错误：跨域事件放在发布方域内（消费方必须依赖发布方）
package com.example.order.domain.event;
public class OrderPaidEvent { ... }
```

**域内部事件**（仅本域消费）可以放在自己的 `domain/event/` 包中。

### 原则三：禁止跨域直接操作聚合根

每个聚合根只能由其所属域的应用服务管理。其他域需要触发操作时：

```
方式一（事件驱动）：域 A 发布事件 → common.event → 域 B 监听并处理
方式二（端口调用）：域 A 在 domain/port/ 定义接口 → 域 B 基础设施层实现适配器
```

## 每个模块内的 DDD 分层

```
{module}/
├── web/                           # Presentation layer
│   ├── controller/               # REST 控制器
│   └── dto/
│       ├── request/              # 入参 DTO（Bean Validation）
│       └── response/             # 出参 DTO
├── application/                   # Application layer
│   ├── service/                  # 应用服务（用例编排，事务边界）
│   ├── port/                     # 对外暴露的应用层端口接口
│   └── mapper/                   # 对象映射器（MapStruct）
├── domain/                        # Domain layer（纯业务，无框架依赖）
│   ├── model/                    # 聚合根、实体、值对象
│   │   ├── enums/
│   │   ├── entity/               # 仅聚合内子实体（无独立 Repository）
│   │   └── valueobject/
│   ├── event/                    # 域内部领域事件（仅本域消费）
│   ├── port/                     # 出站端口接口（Driven Ports，六边形架构）
│   │                             # ← 由外部模块的 infrastructure/ 层实现
│   ├── repository/               # Repository 接口 + 投影接口（CQRS）
│   └── service/                  # 领域服务接口（跨聚合业务规则）
└── infrastructure/                # Infrastructure layer
    ├── event/                    # 领域事件处理器
    ├── adapter/                 # 安全相关适配器（含实现其他域 Driven Port 的 Adapter）
    ├── service/                  # 领域服务实现
    └── config/                   # 技术配置（Properties 等）
```

**分层依赖方向（单向）：** `web → application → domain ← infrastructure`

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
// ✅ 正确：工厂方法接收 hints，@PostPersist 自动注册事件
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

// 应用服务：一次 save 搞定
Order order = Order.create(orderNo, hints);
orderRepository.save(order);  // INSERT → @PostPersist 注册事件 → Spring Data 发布事件

// ❌ 错误：在 save() 前手动发布事件（getId() 为 null）
Order order = Order.create(orderNo);
order.publishCreatedEvent(xxx, null, yyy, null);  // getId() == null!
orderRepository.save(order);

// ❌ 错误：为了拿 ID 被迫 save 两次
Order saved = orderRepository.save(order);
saved.publishCreatedEvent(xxx);
orderRepository.save(saved);  // 多余的第二次 save
```

**注意**：`@PostPersist` 仅在 INSERT 时触发，UPDATE 时不会重复发布。对于非创建场景的事件（如删除），因聚合根已有 ID，可直接在业务方法中 `registerEvent()`。

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
| `domain/port/` 接口 + 本地 Adapter | 远程 Adapter（HTTP/gRPC 客户端）实现同一接口 | **仅替换 infrastructure 层实现** |
| `common.event` 跨域事件 | 消息队列事件契约（Kafka / RabbitMQ），抽取为共享 JAR | 领域层不变 |
| Spring Modulith `verify()` | 独立部署单元天然隔离 | 持续保证边界 |

**六边形架构的核心价值**：领域层（`domain/port/` 接口）稳定不变，只需在对应服务的 `infrastructure/adapter` 层替换 Adapter 实现，业务代码零改动。
