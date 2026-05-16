# 代码分层规范（DDD 架构）

适用于模块化单体（Spring Modulith）和微服务架构。

模块边界治理方式：

- **单体架构**：Spring Modulith `@ApplicationModule` + 架构测试自动验证
- **微服务架构**：独立部署单元 + API 契约（Feign/gRPC）+ 消息队列事件

## 跨域依赖核心原则

### 原则一：出站端口（Driven Port）定义在领域层，适配器在基础设施层实现

采用**六边形架构（Ports & Adapters）**。域需要外部提供数据时（如查询授权信息），在**自身的 `domain/port/`** 定义接口，由*
*本模块或外部模块的 `infrastructure/` 层**提供实现（Driven Adapter）。

```
依赖方向示例（auth 需要授权信息）：
  auth/domain/port/AuthorizationPort（接口，auth 定义）
        ↑ 实现
  base/infrastructure/adapter/AuthorizationAdapter（适配器，base 实现）
  ⟹ base 依赖 auth::port，auth 对 base 零依赖
```

```java
// ✅ auth 定义出站端口，base 在 infrastructure/adapter/ 提供适配器实现
public interface AuthorizationPort {
    Optional<AuthorizationInfo> findAuthorizationInfo(Long accountId);
}

// ❌ 禁止直接依赖另一个域的 Repository
import com.eagle.system.base.domain.repository.UserRepository;
```

**微服务拆分路径**：auth 提取为独立服务时，在 `auth/infrastructure/remote/` 新增远程适配器（HTTP/gRPC 客户端）实现同一
`AuthorizationPort` 接口即可，领域层零改动。

**禁止直接依赖另一个域的 domain 层**（聚合根、Repository、领域服务）。

### 原则二：禁止跨域直接操作聚合根

每个聚合根只能由其所属域的应用服务管理。其他域需要触发操作时：

```
方式一（事件驱动）：域 A 发布事件 → 域 B 通过 Named Interface 订阅并处理
方式二（端口调用）：域 A 在 domain/port/ 定义接口 → 域 B 基础设施层实现适配器
```

跨域事件定义在**发布方**的 `domain/event/` 包中，通过 `@NamedInterface("event")` 暴露给订阅方。单体内通过 Spring
事件机制传递；拆分微服务后改为 JSON + MQ 传递，领域层不变。

```java
// ✅ 正确：跨域事件定义在发布方（auth）的 domain/event/ 中
package com.eagle.system.auth.domain.event;

public record AccountRegisteredEvent(Long accountId, String username, ...) {
}

// ✅ 正确：订阅方（base）通过 allowedDependencies 声明依赖 auth::event
@ApplicationModule(allowedDependencies = {"auth::event", "auth::port", "common"})
```

## 每个模块内的 DDD 分层

```
{module}/
├── interfaces/                         # 接口层（Interfaces Layer）
│   ├── controller/                     # REST 控制器
│   └── dto/                            # 入参 / 出参 DTO（Bean Validation）
│       ├── request/
│       └── response/
├── application/                        # 应用层（Application Layer）
│   ├── service/                        # 应用服务（用例编排，事务边界）
│   └── mapper/                         # DTO ↔ 领域对象映射器（MapStruct）
├── domain/                             # 领域层（Domain Layer，纯业务，无框架依赖）
│   ├── model/
│   │   ├── aggregate/                  # 聚合根（有独立 Repository）
│   │   ├── entity/                     # 聚合内子实体（无独立 Repository）
│   │   ├── valueobject/                # 值对象
│   │   └── enums/                      # 领域枚举
│   ├── repository/                     # Repository 接口 + 投影接口（CQRS）
│   ├── service/                        # 领域服务接口（跨聚合业务规则）
│   ├── event/                          # 领域事件（本域 + 跨域集成事件）
│   └── port/                           # 出站端口接口（Driven Ports，六边形架构）
│                                       # ← 由 infrastructure/ 层实现
└── infrastructure/                     # 基础设施层（Infrastructure Layer）
    ├── persistence/                    # 数据访问（JPA Repository 实现）
    ├── adapter/                        # Driven Port 适配器实现
    ├── event/                          # 事件处理器（@TransactionalEventListener）
    ├── service/                        # 领域服务实现
    ├── config/                         # 技术配置（Properties 等）
    ├── security/                       # 安全适配器
    ├── schedule/                       # 定时任务（XXL-JOB / Spring Scheduler）
    ├── remote/                         # 外部服务调用（Feign / HTTP / gRPC）
    └── messaging/                      # 消息队列（MQ 生产者 / 消费者）
```

**分层依赖方向（单向）：** `interfaces → application → domain ← infrastructure`

> **可选演进（CQRS 命令/查询分离）**：如果模块的读写复杂度增长，可在 `application/` 下增加 `command/`（写模型入参）和 `query/`
> （读模型入参）包，将应用服务按读写职责拆分。当前项目未采用此模式。

## 聚合根规范

**判断标准：**

| 标准             | 聚合根（BaseAggregateRoot） | 子实体（BaseEntity） |
|----------------|------------------------|-----------------|
| 有独立 Repository | ✅                      | ❌               |
| 可被其他聚合引用       | ✅（通过 ID）               | ❌               |
| 有独立业务生命周期      | ✅                      | ❌               |

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

**原因**：`GenerationType.IDENTITY` 策略下，ID 由数据库 INSERT 后分配。若在 `save()` 前构建事件，`getId()` 为
null，导致事件携带错误数据。

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
orderRepository.

save(Order.create(orderNo, hints));
```

**注意**：`@PostPersist` 仅在 INSERT 时触发，UPDATE 时不会重复发布。对于非创建场景的事件（如删除），因聚合根已有 ID，可直接在业务方法中
`registerEvent()`。

## 事件架构

```
domain/event/                # 领域事件（本域事件 + 跨域集成事件）
infrastructure/event/        # 事件处理器 / 事件分发器
infrastructure/messaging/    # MQ 生产者和消费者（微服务拆分后使用）
```

跨域事件通过 `@NamedInterface` 暴露，订阅方在 `allowedDependencies` 中声明依赖：

```java
// auth/domain/event/package-info.java — 暴露事件给其他模块
@NamedInterface("event")
package com.eagle.system.auth.domain.event;

// base/package-info.java — 声明依赖 auth::event
@ApplicationModule(allowedDependencies = {"auth::event", "auth::port", "common"})

// base/infrastructure/event/ — 事件处理器
@Component
public class UserEventHandler {
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handleAccountRegistered(AccountRegisteredEvent event) {
        // 跨域事件处理，独立事务
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
inventoryValidationService.

validateStock(productId, qty);  // 跨聚合校验
order.

addItem(productId, qty);                              // 聚合内操作
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

| 单体时                                      | 拆分后                             | 改动范围                               |
|------------------------------------------|---------------------------------|------------------------------------|
| `domain/port/` 接口 + 本地 Adapter           | 远程 Adapter（HTTP/gRPC 客户端）实现同一接口 | **仅替换 infrastructure/remote/ 实现**  |
| `domain/event/` 跨域事件 + `@NamedInterface` | 消息队列事件契约（JSON），消费方按需解析          | 领域层不变，infrastructure/messaging/ 替换 |
| Spring Modulith `verify()`               | 独立部署单元天然隔离                      | 持续保证边界                             |

**六边形架构的核心价值**：领域层（`domain/port/` 接口 + `domain/event/` 事件）稳定不变，只需在对应服务的 `infrastructure/`
层替换实现，业务代码零改动。
