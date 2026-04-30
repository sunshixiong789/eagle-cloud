---
name: eagle-data-jpa
description: Use when working with JPA/Hibernate in eagle-cloud projects — entity mapping, Spring Data Repository, JPA Auditing (createBy/createTime auto-fill), batch writes, slow query thresholds, EntityGraph
---

# eagle-data-jpa-starter — JPA Auditing 自动填充 + Hibernate 批量/慢 SQL 优化

## 何时使用

- 业务模块使用 JPA / Hibernate
- 自动填充审计字段（`createBy / updateBy / createTime / updateTime`），从 `EagleUser` 取当前用户 ID
- 类型安全配置 Hibernate 批量写入和慢 SQL 阈值

## 何时不要使用

- 模块只用 MyBatis → `eagle-mybatis-starter`

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-data-jpa-starter')
runtimeOnly 'mysql:mysql-connector-j'
```

```yaml
spring.datasource:
  url: ${DB_URL:jdbc:mysql://localhost:3306/eagle?useUnicode=true&characterEncoding=utf8mb4}
  username: ${DB_USER:root}
  password: ${DB_PASSWORD:}

spring.jpa:
  hibernate.ddl-auto: validate         # 生产 validate；开发 none
  open-in-view: false

eagle.jpa:
  batch-size: 100
  order-inserts: true
  order-updates: true
  slow-query-threshold-millis: 2000
  show-sql: false
```

## 核心 API

| 类                    | 用途                                                                    |
|----------------------|-----------------------------------------------------------------------|
| `JpaConfig`          | `@EnableJpaAuditing` + 自动配置 Hibernate 属性                              |
| `AuditorAware<Long>` | Bean 自动注册：从 `SecurityContextHolder` 提取 `EagleUser.getId()`，未登录回退 `0L` |

业务代码用 Spring Data JPA 标准 API：`JpaRepository<T, ID>`、`@Query`、`@EntityGraph`、`Specification`、`Pageable` 等。

## 工作机制

1. `@EnableJpaAuditing` 自动启用——`BaseAggregateRoot / BaseEntity` 的
   `@CreatedBy / @CreatedDate / @LastModifiedBy / @LastModifiedDate` 字段自动填充
2. `eagleHibernatePropertiesConfigurer` BeanPostProcessor 在 EntityManagerFactory 初始化前注入：
    - `hibernate.jdbc.batch_size`
    - `hibernate.order_inserts` / `hibernate.order_updates`
    - `hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS`
3. **已通过 `spring.jpa.properties.*` 显式设置的属性优先级更高**

## 最小示例

```java
// 实体（继承 BaseAggregateRoot 自动获得审计字段）
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseAggregateRoot<Order> {
    private String orderNo;

    public static Order create(String orderNo) {
        Order o = new Order();
        o.orderNo = orderNo;
        return o;
    }
}
// 保存时 createBy / createTime 自动填充

// Repository
public interface OrderRepository extends JpaRepository<Order, Long>,
        JpaSpecificationExecutor<Order> {

    Optional<Order> findByOrderNo(String orderNo);

    @EntityGraph(attributePaths = {"items"})
    Optional<Order> findFullById(Long id);

    @Query("SELECT o.id AS id, o.orderNo AS orderNo, o.totalAmount AS totalAmount " +
            "FROM Order o WHERE o.status = :status")
    Page<OrderSummary> findSummaries(OrderStatus status, Pageable pageable);
}

// 投影（避免完整加载聚合根）
public interface OrderSummary {
    Long getId();

    String getOrderNo();

    BigDecimal getTotalAmount();
}
```

## 配置项

| key                                     | 类型      | 默认      | 说明                       |
|-----------------------------------------|---------|---------|--------------------------|
| `eagle.jpa.batch-size`                  | int     | `100`   | Hibernate JDBC 批量大小，0 禁用 |
| `eagle.jpa.order-inserts`               | boolean | `true`  | 重排批量 INSERT              |
| `eagle.jpa.order-updates`               | boolean | `true`  | 重排批量 UPDATE              |
| `eagle.jpa.slow-query-threshold-millis` | long    | `2000`  | 慢 SQL 阈值，0 禁用            |
| `eagle.jpa.show-sql`                    | boolean | `false` | 控制台输出 SQL（生产 false）      |

JPA / Hibernate 标准配置走 `spring.jpa.*` 和 `spring.jpa.properties.hibernate.*`（优先级更高）。

## 常见错误

- ❌ 生产 `ddl-auto: update` → ✅ `validate`（详见 `28-migration.md`）
- ❌ `open-in-view: true` → ✅ `false`
- ❌ 列表返回完整聚合根 → ✅ 投影接口
- ❌ 跨聚合 `@ManyToMany` → ✅ 存 ID 集合
- ❌ 未实现 SecurityContext 时审计字段为 null → ✅ AuditorAware 兜底返回 `0L`，但建议项目层完善 `EagleUser` 注入
- ❌ 实体加 `@Data / @Builder` → ✅ `@Getter` + 静态工厂方法

## 关联规则

- `.claude/rules/06-database.md`
- `.claude/rules/23-performance.md`
- `.claude/rules/28-migration.md`
