# 数据层：JPA、事务、并发

## 基类与字段

[BaseEntity](eagle-starter/eagle-data-jpa-starter/src/main/java/com/eagle/datajpa/base/BaseEntity.java) 提供且**仅提供**：

```java
Long id;  Long createBy;  Long updateBy;
LocalDateTime createTime;  LocalDateTime updateTime;
@Version Long version;
```

- 字段名是 `createBy` / `createTime`，**不是** `createdBy` / `createdAt`
- **基类不含 `deleted`** —— 需要软删除的表自行声明该字段
- 聚合根继承 `BaseAggregateRoot<T>`（多了领域事件能力），子实体继承 `BaseEntity`
- 值对象用 `@Embeddable` + 聚合根侧 `@Embedded`；JPA 实体**不能是 `record`**（Hibernate 需可变对象）

## 表命名

按服务域前缀，**不是** `t_`：

| 前缀 | 归属 | 实例 |
|---|---|---|
| `sys_` | 系统管理 | `sys_user` `sys_role` `sys_dict` `sys_file` `sys_log` |
| `auth_` | 认证 | `auth_account` `auth_blacklist` |
| `user_` | 用户侧数据 | `user_message` `user_announcement_cursor` |
| `eagle_` | starter 自带 | `eagle_audit_log` |

字段全小写下划线，每个字段加 `@Column(comment = "...")`，非空字段加 `nullable = false`。

## 枚举必须 `EnumType.STRING`

```java
// ✅ 存字符串名，枚举重排不影响历史数据
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
private OrderStatus status = OrderStatus.CREATED;

// ❌ 默认 ORDINAL，枚举插入/重排会让历史数据语义错乱
@Enumerated
private OrderStatus status;
```

持久化状态用 `enum`；**行为分派**用 `sealed` + 模式匹配（见 `01-java25.md`），两者分工不要混。

## 跨聚合只存 ID

```java
// ✅ ID 集合
@ElementCollection(fetch = FetchType.LAZY)
@CollectionTable(name = "sys_user_role",
        joinColumns = @JoinColumn(name = "user_id",
                foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)))
@Column(name = "role_id")
private Set<Long> roleIds = new HashSet<>();

// ❌ 禁止跨聚合关联注解
@ManyToMany private List<Permission> permissions;
@ManyToOne  private Department dept;
```

## 强制禁止物理外键

**所有表禁止 `FOREIGN KEY` / `REFERENCES`**，含 `@ElementCollection` / `@CollectionTable` 默认会生成的那些。

理由：物理 FK 阻塞跨服务拆分、分库分表、数据迁移、批量导入。引用完整性由聚合根业务方法和应用层校验保证。

任何会让 Hibernate 建 FK 的注解必须显式 `foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)` —— 涵盖 `@JoinColumn`（含 `@CollectionTable.joinColumns`、`@JoinTable.joinColumns/inverseJoinColumns`）、`@MapKeyJoinColumn`、`@PrimaryKeyJoinColumn`。

遗留库上已存在的 FK 不会被 Hibernate 自动删除，需 `ALTER TABLE ... DROP FOREIGN KEY` 手工清理。

## 索引名必须 schema 内全局唯一

H2 / PostgreSQL / Oracle 要求索引名在 schema 内唯一（MySQL 只要表内唯一，但跨 DB 兼容按全局唯一）。两表共用同名索引会触发 `Index "XXX" already exists` **启动失败**。

格式 `idx_{table}_{column}` / `uk_{table}_{column}`：

```java
// ✅ 含表名前缀
@Table(name = "sys_user", indexes = {
        @Index(name = "uk_user_account_id", columnList = "account_id", unique = true),
        @Index(name = "idx_user_dept_id",   columnList = "dept_id")
})
@Table(name = "sys_role_dept", indexes = {
        @Index(name = "idx_role_dept_dept_id", columnList = "dept_id")   // 不与 user 表冲突
})
```

多租户表必须有 `tenant_id`，且**索引以 `tenant_id` 为前导列**，创建后不可更新。

## 读查询

```java
public interface OrderSummary {          // 投影接口，避免 SELECT *
    Long getId();
    String getOrderNo();
    BigDecimal getTotalAmount();
}

@Query("SELECT o.id AS id, o.orderNo AS orderNo, o.totalAmount AS totalAmount FROM Order o")
Page<OrderSummary> findOrderSummaries(Pageable pageable);
```

- **防 N+1**：列表页不在循环内触发懒加载，用投影 / `@EntityGraph` / fetch join / 批量查询
- 深翻页（`page > 100`）改游标分页或限制最大页数
- 只读查询加 `@Transactional(readOnly = true)`
- 批量写入走 JPA batch（`hibernate.order_inserts` + `jdbc.batch_size`），**禁止**循环单条 insert；分批用 `Gatherers.windowFixed(500)`

---

# 事务与并发

## 事务边界

- 写操作加 `@Transactional(rollbackFor = Exception.class)`；只读加 `@Transactional(readOnly = true)`
- **禁止**在 `@Transactional` 内调用远程服务 —— 远程慢会长期持有 DB 连接，远程失败不该触发 DB 回滚
  - 拆法：事务内 `registerEvent()`，由 AFTER_COMMIT 异步触发远程
  - 例外：`@GlobalTransactional`（Seata）模式下必须在事务内调用远程
- 事务方法不得被同类内部调用（Spring AOP 代理限制）

## 锁

- 聚合根用乐观锁（`@Version`，基类已提供）
- 需要悲观锁时用 `@Lock(LockModeType.PESSIMISTIC_WRITE)`，严格控制粒度
- 分布式锁用 `DistributedLock.tryLock(key, long waitSec, long leaseSec, Supplier)` —— **参数是 `long` 秒不是 `Duration`**
- **禁止**用 Redis 做强一致性事务

## 线程安全

- Service / Repository / Controller 是单例 Bean，**禁止可变实例变量**
- 工具类必须无状态或线程安全
- `ThreadLocal` 用完必须 `remove()`（`try/finally`）—— 虚拟线程下泄漏后果更严重

## 异步执行器

`@Async` 统一用 Bean 名 `taskExecutor`（[AsyncConfig](eagle-starter/eagle-common-starter/src/main/java/com/eagle/common/config/AsyncConfig.java)），**禁止** `new Thread()` 和无界线程池。

| 模式 | 背压 |
|---|---|
| 平台线程（默认） | 有界队列 + `CallerRunsPolicy` |
| 虚拟线程（`spring.threads.virtual.enabled=true`） | `eagle.async.concurrency-limit`，**默认无界，开虚拟线程前必须设正数** |

异步任务必须传递 trace / tenant / 安全上下文（由 `ContextPropagationConfig` 统一处理）。详见 `01-java25.md` 的虚拟线程注意事项。

---

# Schema 管理

**Flyway 尚未引入** —— 仓库 0 个 migration 文件，schema 当前由 Hibernate 同步。实际配置：

| 环境 | `ddl-auto` |
|---|---|
| dev / local / test | `update` |
| **prod** | **`validate`**（启动期校验实体与表结构匹配，运行时禁止改表） |

**生产禁止 `ddl-auto: update`。**

引入 Flyway 后遵循（届时再展开）：

- 文件位置 `{module}/src/main/resources/db/migration/`，命名 `V{yyyyMMddHHmm}__{snake_case}.sql`（时间戳版本号，**禁止**顺序数字，多人开发必撞号）
- **已发布到任何环境的迁移文件不得修改** —— checksum 不匹配会让所有环境启动失败；要改逻辑就新建脚本
- 迁移脚本同样**禁止** `FOREIGN KEY` / `REFERENCES`
- Flyway 不支持自动回滚，靠前向修复；PR 描述里必须附回滚 SQL
- 加列必须可空或有默认值；大表数据迁移按主键分批 1–10 万行
- **禁止**迁移与业务代码同 PR

---

## PR 前自检（无输出即合规）

```bash
# 漏网的物理外键（@ForeignKey 常换行，必须跨行检查）
find eagle-services eagle-starter -name "*.java" -not -path "*/build/*" -exec perl -0777 -ne \
  'while (/\@JoinColumn\b/g) { print "$ARGV\n" unless substr($_, pos(), 200) =~ /NO_CONSTRAINT/ }' {} \; | sort -u

# 重名索引
find . -name "*.java" -not -path "*/build/*" | xargs grep -hE "@Index\(name\s*=\s*\"" \
  | grep -oE 'name\s*=\s*"[^"]+"' | sort | uniq -c | awk '$1 > 1'
```
