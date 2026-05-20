# eagle-dynamic-datasource-starter — 主从读写分离

## 何时使用

- 主从读写分离（写主库，读从库）
- 用 `@ReadOnly` 或 `@Transactional(readOnly=true)` 自动路由
- 一主多从（支持从库列表轮询）

## 何时不要使用

- 单数据源应用
- 多个业务库分库分表（用 ShardingSphere 等专门方案）
- 跨库强一致性事务（配合 `eagle-seata-starter`）

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-dynamic-datasource-starter')
runtimeOnly 'mysql:mysql-connector-j'
```

### 单从库配置

```yaml
eagle:
  datasource:
    enabled: true                   # 默认 false，必须显式开启
    master:
      url: jdbc:mysql://master:3306/eagle?useUnicode=true&characterEncoding=utf8mb4
      username: ${DB_USER}
      password: ${DB_PASSWORD}      # 生产必须 ENC() 加密
    slave:
      url: jdbc:mysql://slave:3306/eagle?useUnicode=true&characterEncoding=utf8mb4
      username: ${DB_USER}
      password: ${DB_PASSWORD}
```

### 多从库配置（轮询路由）

```yaml
eagle:
  datasource:
    enabled: true
    master:
      url: jdbc:mysql://master:3306/eagle
      username: ${DB_USER}
      password: ${DB_PASSWORD}
    slaves:                          # 优先级高于单 slave 字段
      - url: jdbc:mysql://slave0:3306/eagle
        username: ${DB_USER}
        password: ${DB_PASSWORD}
      - url: jdbc:mysql://slave1:3306/eagle
        username: ${DB_USER}
        password: ${DB_PASSWORD}
```

⚠️ 这是 starter **自定义**的简单 master/slave 路由（非 baomidou `dynamic-datasource`）。

## 核心 API

| 类 / 注解                             | 用途                                                         |
|------------------------------------|------------------------------------------------------------|
| `@ReadOnly`                        | 方法或类级注解：路由到 slave                                          |
| `ReadOnlyAspect`                   | 拦截 `@ReadOnly` + `@Transactional(readOnly=true)` 自动切换从库    |
| `DataSourceContextHolder.set(key)` | 编程式设置数据源                                                   |
| `DataSourceContextHolder.get()`    | 获取当前数据源，默认 `"master"`                                      |
| `DataSourceContextHolder.getRaw()` | 获取原始 ThreadLocal 值（未设置时返回 `null`，用于区分"未设置"与"明确设置为 master"） |
| `DataSourceContextHolder.clear()`  | 清除 ThreadLocal（线程池场景必须在 finally 中调用）                       |
| `DynamicDataSource`                | `AbstractRoutingDataSource` 子类，多从库时内部轮询                    |
| `DynamicDataSourceProperties`      | 配置属性类（`eagle.datasource.*`）                                |

## 使用示例

### 注解式（推荐）

```java
@Service
public class OrderQueryService {

    // 方法级：只读查询走从库
    @ReadOnly
    public List<Order> findByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    // 等价写法：Spring 标准注解也会被自动路由到从库
    @Transactional(readOnly = true)
    public Page<OrderSummary> listSummaries(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }
}

// 类级：整个 Service 路由到从库
@ReadOnly
@Service
public class ReportQueryService {
    public List<DailyStat> dailyStats(LocalDate date) { ... }
    public List<TrendStat> trends(DateRange range) { ... }
}
```

### 编程式（特殊场景）

```java
public List<Stat> stats() {
    DataSourceContextHolder.set(DataSourceContextHolder.SLAVE);
    try {
        return orderRepository.findAggregations();
    } finally {
        DataSourceContextHolder.clear(); // 必须 clear()，不要 set("master")
    }
}
```

### 异步任务

starter 自动注册 `dataSourceContextTaskDecorator`（`@ConditionalOnMissingBean(TaskDecorator.class)`），
`@Async` 任务会自动透传 `DataSourceContextHolder`：

```java
@Async
public void asyncExport() {
    // DataSourceContextHolder 已由 TaskDecorator 透传，无需手动设置
    reportRepository.findAll().forEach(this::process);
}
```

若应用已有自定义 `TaskDecorator`（如 tenant 上下文传播），starter 不会覆盖，需手动组合：

```java
@Bean
public TaskDecorator combinedDecorator(TaskDecorator tenantDecorator) {
    return runnable -> {
        String dsKey = DataSourceContextHolder.getRaw();
        Runnable wrapped = tenantDecorator.decorate(runnable);
        return () -> {
            if (dsKey != null) DataSourceContextHolder.set(dsKey);
            try { wrapped.run(); }
            finally { DataSourceContextHolder.clear(); }
        };
    };
}
```

## 配置项

| 配置键                                         | 类型      | 默认值     | 说明                   |
|---------------------------------------------|---------|---------|----------------------|
| `eagle.datasource.enabled`                  | boolean | `false` | 总开关，必须显式设为 `true`    |
| `eagle.datasource.master.url`               | String  | —       | 主库 JDBC URL（必填）      |
| `eagle.datasource.master.username`          | String  | —       | 主库用户名                |
| `eagle.datasource.master.password`          | String  | —       | 主库密码                 |
| `eagle.datasource.master.driver-class-name` | String  | 自动推断    | 驱动类名（可省略）            |
| `eagle.datasource.slave.*`                  | —       | —       | 单从库配置，结构同 master     |
| `eagle.datasource.slaves[n].*`              | —       | —       | 多从库列表；非空时取代 slave 字段 |

## 常见错误

| 错误                                              | 正确做法                                                            |
|-------------------------------------------------|-----------------------------------------------------------------|
| ❌ 写操作加 `@ReadOnly`                              | ✅ 仅查询方法使用                                                       |
| ❌ `finally` 里 `set("master")` 而非 `clear()`      | ✅ 必须 `clear()` 防止线程池泄漏                                          |
| ❌ 主从延迟未考虑（写后立即读）                                | ✅ 强一致性读不加 `@ReadOnly`                                           |
| ❌ 期望 `@Transactional(readOnly=true)` 类级注解自动路由从库 | ✅ 类级 `@Transactional` 不被 `@annotation` 切点拦截，请用 `@ReadOnly` 类级注解 |
| ❌ 配置写 `spring.datasource.dynamic.*`             | ✅ 正确前缀是 `eagle.datasource.*`                                    |
| ❌ 期望默认开启                                        | ✅ `enabled` 默认 `false`，需显式开启                                    |
| ❌ 使用已废弃的 `DataSourceProperties` 类               | ✅ 改用 `DynamicDataSourceProperties`                              |

## 关联规范

- `.claude/rules/16-transaction-distributed.md`
- `.claude/rules/23-performance.md` — 主从延迟
- `.claude/rules/08-concurrency.md` — ThreadLocal 使用规范
