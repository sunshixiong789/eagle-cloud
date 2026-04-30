# eagle-dynamic-datasource-starter — 多数据源动态路由 + 读写分离

## 何时使用

- 主从读写分离（写主库，读从库）
- 多个业务库（如 order DB + user DB）
- 多租户 Schema 隔离（与 `eagle-tenant-starter` 配合）

## 何时不要使用

- 单数据源应用（增加复杂度无收益）
- 跨库事务（需配合 `eagle-seata-starter`）

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-dynamic-datasource-starter')
```

```yaml
spring.datasource.dynamic:
  primary: master
  strict: false                       # 严格模式：未指定数据源时报错
  datasource:
    master:
      url: jdbc:mysql://master:3306/eagle
      username: ${DB_USER}
      password: ${DB_PASSWORD}
    slave:
      url: jdbc:mysql://slave:3306/eagle
      username: ${DB_USER}
      password: ${DB_PASSWORD}

eagle.datasource:
  enabled: true
  read-only-routing: slave            # @ReadOnly 路由到的数据源
```

## 核心 API

| 类 / 接口 / 注解 | 用途 |
|---|---|
| `@ReadOnly` | 方法注解：本次调用路由到只读库 |
| `ReadOnlyAspect` | 切面（自动切换数据源） |
| `DataSourceContextHolder` | 编程式切换数据源（`set` / `clear`） |
| `DynamicDataSource` | `AbstractRoutingDataSource` 子类 |

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

// 编程式：临时切换数据源
public List<Stat> stat() {
    DataSourceContextHolder.set("analytics");
    try {
        return analyticsRepository.findAll();
    } finally {
        DataSourceContextHolder.clear();
    }
}

// 使用 baomidou dynamic-datasource 注解（注意：starter 已基于此构建）
@DS("slave")
public List<User> queryFromSlave() { ... }
```

## 配置项

| key | 类型 | 默认 | 说明 |
|---|---|---|---|
| `eagle.datasource.enabled` | boolean | `true` | 总开关 |
| `eagle.datasource.read-only-routing` | String | `slave` | `@ReadOnly` 路由目标 |
| `spring.datasource.dynamic.primary` | String | `master` | 默认数据源 |

## 常见错误

- ❌ 写操作误用 `@ReadOnly` → ✅ 仅查询方法
- ❌ 同方法内多个数据源切换 → ✅ 拆方法或用编程式 + finally clear
- ❌ 主从延迟未考虑 → ✅ 写后立即读走主库（不加 `@ReadOnly`）
- ❌ 跨数据源事务期望 ACID → ✅ 用 `eagle-seata-starter`
- ❌ 异步任务未传递数据源上下文 → ✅ 配置 `TaskDecorator`

## 关联规则

- `.claude/rules/16-transaction-distributed.md` — 跨库事务
- `.claude/rules/17-tenant-permission.md` — 多租户 Schema 隔离配合
- `.claude/rules/23-performance.md` — 主从延迟与一致性
