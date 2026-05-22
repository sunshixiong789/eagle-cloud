# Eagle Data R2DBC Starter

为 WebFlux + R2DBC 服务提供：

- Spring Boot R2DBC 默认装配（ConnectionFactory / R2dbcEntityTemplate）
- **审计字段自动填充**：聚合根继承 `BaseR2dbcEntity` 即可
- 可通过 `eagle.r2dbc.enabled=false` 整体禁用

## 接入

```gradle
implementation project(':eagle-starter:eagle-data-r2dbc-starter')
```

PostgreSQL 配置：

```yaml
spring:
  r2dbc:
    url: r2dbc:pool:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:eagle}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:}

eagle:
  r2dbc:
    enabled: true   # 默认 true，可省略
```

## 基类选型

| 基类                              | 适用                              | 关键能力                       |
|---------------------------------|---------------------------------|----------------------------|
| `BaseR2dbcAggregateRoot<T>`     | **聚合根**（有独立 Repository、可被其他聚合引用） | 审计 + 乐观锁 + **领域事件注册/自动发布** |
| `BaseR2dbcEntity`               | 聚合内子实体（无独立 Repository）          | 审计 + 乐观锁，无事件能力             |

字段语义与 JPA 端 `BaseAggregateRoot` / `BaseEntity` 对齐，便于在两种持久化技术之间迁移。

## 审计字段

聚合根：

```java
@Table("t_order")
@Getter @NoArgsConstructor
public class Order extends BaseR2dbcAggregateRoot<Order> {

    private String orderNo;
    private OrderStatus status;
    private BigDecimal totalAmount;

    public static Order create(String orderNo, BigDecimal amount) {
        Order order = new Order();
        order.orderNo = orderNo;
        order.status = OrderStatus.CREATED;
        order.totalAmount = amount;
        order.registerEvent(new OrderCreatedEvent(orderNo, amount));
        return order;
    }

    public void pay() {
        this.status = OrderStatus.PAID;
        registerEvent(new OrderPaidEvent(getId(), orderNo));
    }
}
```

子实体（无领域事件）：

```java
@Table("t_order_item")
@Getter @Setter @NoArgsConstructor
public class OrderItem extends BaseR2dbcEntity {
    private Long orderId;
    private Long productId;
    private Integer quantity;
}
```

两个基类自带：

| 字段           | 类型            | 注解                                          |
|--------------|---------------|---------------------------------------------|
| `id`         | Long          | `@Id`                                       |
| `createBy`   | Long          | `@CreatedBy`（启用 Security 时自动填当前用户 ID）       |
| `updateBy`   | Long          | `@LastModifiedBy`                           |
| `createTime` | LocalDateTime | `@CreatedDate`                              |
| `updateTime` | LocalDateTime | `@LastModifiedDate`                         |
| `version`    | Long          | `@Version`（乐观锁，R2DBC 由 Spring Data 自动维护）    |

填充逻辑：

- `@EnableR2dbcAuditing` 触发 Spring Data 内置 `AuditingEntityCallback`
- 默认 `ReactiveAuditorAware<Long>` 从响应式 `SecurityContext` 取 `EagleUser.id`
- 未登录场景回退到 `0L`，不为 null

如需自定义审计来源（例如改用 Long 之外的类型 / 增加租户字段）：

```java
@Bean
public ReactiveAuditorAware<Long> reactiveAuditorAware() {
    return () -> Mono.deferContextual(ctx -> Mono.justOrEmpty(
            ctx.<Long>getOrEmpty("eagle.audit.userId").orElse(0L)));
}
```

声明 `@ConditionalOnMissingBean` 后 Eagle 默认实现会自动让位。

## 与 JPA 共存

`BaseR2dbcAggregateRoot` / `BaseR2dbcEntity` 与 `eagle-common-starter` 中的
`BaseAggregateRoot` / `BaseEntity`（JPA 版）字段语义完全对齐，
但因 R2DBC 不依赖 JPA 注解，二者独立维护。同一服务同时使用 JPA + R2DBC 是合法的，
分别继承各自的基类即可。

## 领域事件发布机制

`BaseR2dbcAggregateRoot` 继承自 Spring Data `AbstractAggregateRoot`。
当 `R2dbcRepository.save(...)` 被调用后，
`EventPublishingRepositoryProxyPostProcessor` 会通过 `ApplicationEventPublisher`
自动发布 `registerEvent(...)` 注册的事件，并随即清空待发布列表，行为与 JPA 端完全一致。
事件处理器应使用：

```java
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onOrderPaid(OrderPaidEvent event) {
    // ...
}
```

## 自动配置一览

| AutoConfiguration                     | 作用                              |
|---------------------------------------|---------------------------------|
| `EagleR2dbcAutoConfiguration`         | 占位 + Properties 注册              |
| `EagleR2dbcAuditingAutoConfiguration` | `@EnableR2dbcAuditing` + 默认审计提供者 |
