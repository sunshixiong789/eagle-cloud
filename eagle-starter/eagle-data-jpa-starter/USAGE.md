# eagle-data-jpa-starter — JPA / Hibernate 配置（审计 + 多数据库支持）

## 何时使用

- 业务模块使用 JPA（默认数据访问层）
- 需要 JPA Auditing（自动填充 `createdBy / createdAt / updatedBy / updatedAt`）
- 多数据库类型支持（MySQL / PostgreSQL / H2）

## 何时不要使用

- 模块只用 MyBatis → 用 `eagle-mybatis-starter`
- 只读 Elasticsearch / Redis 查询

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-data-jpa-starter')
runtimeOnly 'mysql:mysql-connector-j'      // 或 postgresql / h2
```

```yaml
spring.datasource:
  url: ${DB_URL:jdbc:mysql://localhost:3306/eagle?useUnicode=true&characterEncoding=utf8mb4}
  username: ${DB_USER:root}
  password: ${DB_PASSWORD:}
  driver-class-name: com.mysql.cj.jdbc.Driver

spring.jpa:
  hibernate.ddl-auto: validate           # 生产 validate；开发 none/update
  open-in-view: false
  properties:
    hibernate:
      enable_lazy_load_no_trans: false
      jdbc.batch_size: 100
      order_inserts: true
      order_updates: true
      query.in_clause_parameter_padding: true

eagle.data-jpa:
  enabled: true
  auditing-enabled: true                 # 自动填充审计字段
```

## 核心 API

| 类 | 用途 |
|---|---|
| `JpaConfig` | 启用 JPA Auditing（`@EnableJpaAuditing`）+ 默认 `AuditorAware`（从 `SecurityContext` 取当前用户） |
| `JpaProperties` | `eagle.data-jpa.*` 配置 |

业务代码使用 Spring Data JPA 标准 API：`JpaRepository<T, ID>`、`@Query`、`@EntityGraph`、`Pageable` 等。

## 最小示例

```java
// 实体（继承 BaseAggregateRoot 自动获得审计字段）
@Entity
@Getter
public class Order extends BaseAggregateRoot<Order> {
    private String orderNo;
}

// Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNo(String orderNo);

    @EntityGraph(attributePaths = {"items"})
    Optional<Order> findFullById(Long id);

    @Query("SELECT o.id AS id, o.orderNo AS orderNo, o.totalAmount AS totalAmount " +
           "FROM Order o WHERE o.status = :status")
    Page<OrderSummary> findSummaries(OrderStatus status, Pageable pageable);
}

// 投影接口（只取必要字段，避免 N+1）
public interface OrderSummary {
    Long getId();
    String getOrderNo();
    BigDecimal getTotalAmount();
}
```

## 配置项

| key | 类型 | 默认 | 说明 |
|---|---|---|---|
| `eagle.data-jpa.enabled` | boolean | `true` | 总开关 |
| `eagle.data-jpa.auditing-enabled` | boolean | `true` | 启用审计 |
| `eagle.data-jpa.show-sql` | boolean | `false` | 打印 SQL |

JPA / Hibernate 标准配置走 `spring.jpa.*`。

## 常见错误

- ❌ 生产 `ddl-auto: update` → ✅ `validate`（详见 `28-migration.md`）
- ❌ `open-in-view: true` → ✅ `false`（视图层延迟加载是反模式）
- ❌ 列表查询返回完整实体 → ✅ 投影接口（`OrderSummary`）
- ❌ 跨聚合 `@ManyToMany` → ✅ 存 ID 集合（`@ElementCollection`）
- ❌ 循环内逐条 `findById` → ✅ `findAllById(ids)`
- ❌ 实体加 `@Data` / `@Builder` → ✅ `@Getter` + 静态工厂方法

## 关联规则

- `.claude/rules/06-database.md` — 实体规范、索引、CQRS 投影
- `.claude/rules/23-performance.md` — N+1 防护、批量写入
- `.claude/rules/28-migration.md` — Flyway 迁移
