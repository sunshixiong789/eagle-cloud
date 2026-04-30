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

实体必须在 `@Table` 中显式声明索引，禁止无索引的大表全表扫描。

**索引命名约定（强制）：索引名在 schema 内必须全局唯一**

H2 / PostgreSQL / Oracle 等数据库要求索引名在 schema 范围内唯一（MySQL 是表内唯一，但跨 DB 兼容必须按全局唯一）。两个表共用同名索引会触发 `Index "XXX" already exists` 启动错误。

格式：`idx_{table}_{column}`（普通）/ `uk_{table}_{column}`（唯一）

```java
// ✅ 正确：包含表名前缀，全局唯一
@Table(name = "sys_user", indexes = {
    @Index(name = "uk_user_account_id", columnList = "account_id", unique = true),
    @Index(name = "idx_user_username", columnList = "username"),
    @Index(name = "idx_user_dept_id", columnList = "dept_id")
})

@Table(name = "sys_role_dept", indexes = {
    @Index(name = "idx_role_dept_role_id", columnList = "role_id"),
    @Index(name = "idx_role_dept_dept_id", columnList = "dept_id")   // 不与 user 表的 dept_id 索引冲突
})

// ❌ 错误：多张表共用 idx_dept_id 会在 H2 启动时冲突
@Table(name = "sys_user", indexes = {
    @Index(name = "idx_dept_id", columnList = "dept_id")
})
@Table(name = "sys_role_dept", indexes = {
    @Index(name = "idx_dept_id", columnList = "dept_id")
})
```

**自检：** PR 前可用以下命令扫描重名索引：

```bash
find . -name "*.java" | xargs grep -hE "@Index\(name\s*=\s*\"" \
  | grep -oE 'name\s*=\s*"[^"]+"' | sort | uniq -c | awk '$1 > 1'
```

输出非空即存在重名，必须修复。

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
