# 数据库规范（Database）

## 实体类规范

**聚合根 vs 子实体继承：**

```java
// ✅ 有独立 Repository 的实体（聚合根）
public class Order extends BaseAggregateRoot<Order> { }

// ✅ 聚合内子实体（无独立 Repository，由聚合根级联管理）
public class OrderItemEntity extends BaseEntity { }
```

**值对象（`@Embeddable`）映射：**

值对象使用 `@Embeddable` 嵌入聚合根，聚合根端使用 `@Embedded`：

```java
// 值对象定义
@Embeddable
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserProfile {
    @Column(comment = "昵称")
    private String nickname;

    @Column(comment = "头像")
    private String avatar;
}

// 聚合根中嵌入
@Embedded
private UserProfile profile;
```

**枚举字段必须指定 `EnumType.STRING`：**

```java
// ✅ 正确：存字符串名称，枚举顺序变化不影响数据
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
private OrderStatus status = OrderStatus.CREATED;

// ❌ 禁止：默认 ORDINAL，枚举插入/重排会导致历史数据语义错乱
@Enumerated
private OrderStatus status;
```

## 跨聚合 ID 引用

跨聚合边界只保存 ID，**禁止使用 JPA 关联注解跨聚合**：

```java
// ✅ 正确：@ElementCollection 存 ID 集合
@ElementCollection(fetch = FetchType.LAZY)
@CollectionTable(name = "t_role_permission", joinColumns = @JoinColumn(name = "role_id"))
@Column(name = "permission_id")
private Set<Long> permissionIds = new HashSet<>();

// ✅ 正确：单个 ID 引用
@Column(name = "dept_id")
private Long deptId;

// ❌ 禁止：@ManyToMany 跨聚合关联
@ManyToMany
private List<Permission> permissions;

// ❌ 禁止：@ManyToOne 跨聚合关联
@ManyToOne
private Department dept;
```

## 索引规范

实体必须在 `@Table` 中显式声明索引，禁止无索引的大表全表扫描：

```java
@Table(name = "t_order", indexes = {
    @Index(name = "idx_order_no", columnList = "order_no", unique = true),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
```

## 字段规范

- 字段使用 `@Column(comment = "...")` 添加数据库注释
- 新增字段必须设置合理默认值，保证向后兼容
- 非空字段必须加 `nullable = false`

## CQRS 读查询规范

列表查询使用 JPA 投影接口，避免 SELECT *：

```java
// 投影接口
public interface OrderSummary {
    Long getId();
    String getOrderNo();
    BigDecimal getTotalAmount();
}

// @Query 指定字段别名映射到投影
@Query("SELECT o.id AS id, o.orderNo AS orderNo, o.totalAmount AS totalAmount FROM Order o")
Page<OrderSummary> findOrderSummaries(Pageable pageable);
```

## 批量写入

批量写入使用 JPA batch，禁止循环单条 insert：

```yaml
spring.jpa.properties.hibernate.order_inserts: true
spring.jpa.properties.hibernate.jdbc.batch_size: 100
```

## 环境约束

- `ddl-auto: update` 仅用于开发环境，生产环境禁止自动变更表结构
- 生产环境使用 Flyway / Liquibase 管理 DDL 变更
