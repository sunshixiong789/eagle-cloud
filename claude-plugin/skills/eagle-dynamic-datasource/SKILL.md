---
name: eagle-dynamic-datasource
description: Use when implementing master/slave read-write splitting in eagle-cloud projects — @ReadOnly annotation, DataSourceContextHolder programmatic switching
---

# eagle-dynamic-datasource-starter — 主从读写分离（master / slave）

## 何时使用

- 主从读写分离（写主库，读从库）
- 用 `@ReadOnly` 或 `@Transactional(readOnly=true)` 自动路由

## 何时不要使用

- 单数据源应用
- 多个业务库分库分表（用 ShardingSphere 等专门方案）
- 跨库强一致性事务（配合 `eagle-seata-starter`）

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-dynamic-datasource-starter')
runtimeOnly 'mysql:mysql-connector-j'
```

```yaml
eagle.datasource:
  enabled: true                     # 默认 false，必须显式开启
  master:
    url: jdbc:mysql://master:3306/eagle?useUnicode=true&characterEncoding=utf8mb4
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  slave:
    url: jdbc:mysql://slave:3306/eagle?useUnicode=true&characterEncoding=utf8mb4
    username: ${DB_USER}
    password: ${DB_PASSWORD}
```

⚠️ 这是 starter **自定义**的简单 master/slave 路由（非 baomidou `dynamic-datasource`）。

## 核心 API

| 类 / 注解 | 用途 |
|---|---|
| `@ReadOnly` | 方法注解：路由到 slave |
| `ReadOnlyAspect` | `@ReadOnly` + `@Transactional(readOnly=true)` 切面 |
| `DataSourceContextHolder` | 编程式：`set(MASTER / SLAVE)` / `get()` / `clear()` |
| `DynamicDataSource` | `AbstractRoutingDataSource` 子类 |

`DataSourceContextHolder.MASTER = "master"` / `SLAVE = "slave"`，默认返回 `MASTER`。

## 最小示例

```java
// 注解式：只读查询走从库
@Service
public class OrderQueryService {

    @ReadOnly
    public List<Order> findByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }
}

// 等价：Spring 标准注解
@Transactional(readOnly = true)
public Page<OrderSummary> listSummaries(Pageable pageable) {
    return orderRepository.findAll(pageable);
}

// 编程式（特殊场景）
public List<Stat> stats() {
    DataSourceContextHolder.set(DataSourceContextHolder.SLAVE);
    try {
        return orderRepository.findAggregations();
    } finally {
        DataSourceContextHolder.clear();
    }
}
```

## 配置项

| key | 类型 | 默认 | 说明 |
|---|---|---|---|
| `eagle.datasource.enabled` | boolean | **`false`** | 总开关 |
| `eagle.datasource.master.url` | String | — | 主库 URL |
| `eagle.datasource.master.username` | String | — | 主库用户名 |
| `eagle.datasource.master.password` | String | — | 主库密码 |
| `eagle.datasource.master.driver-class-name` | String | — | 可省略，自动推断 |
| `eagle.datasource.slave.*` | — | — | 同上结构 |

## 常见错误

- ❌ 写操作误用 `@ReadOnly` → ✅ 仅查询
- ❌ 同方法内多次切换 → ✅ 拆方法或 `try/finally clear()`
- ❌ 主从延迟未考虑（写后立即读）→ ✅ 一致性敏感读不加 `@ReadOnly`
- ❌ 异步任务未传递上下文 → ✅ 自定义 `TaskDecorator` 透传
- ❌ 配置写 `spring.datasource.dynamic.*`（baomidou 风格）→ ✅ 真实是 **`eagle.datasource.master/slave`**
- ❌ 期望默认开启 → ✅ **`enabled` 默认 `false`**

## 关联规则

- `.claude/rules/16-transaction-distributed.md`
- `.claude/rules/23-performance.md` — 主从延迟
