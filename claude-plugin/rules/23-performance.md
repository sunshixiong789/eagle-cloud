# 性能规范（Performance）

## 数据库访问

### N+1 查询防护

```java
// ❌ N+1：循环内查询关联数据
List<Order> orders = orderRepository.findAll();
for (Order o : orders) {
    o.getItems().size();   // 每个订单触发一次 SELECT
}

// ✅ 方案一：@EntityGraph（推荐）
@EntityGraph(attributePaths = {"items"})
@Query("SELECT o FROM Order o WHERE o.status = :status")
List<Order> findByStatus(OrderStatus status);

// ✅ 方案二：JPQL fetch join
@Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.id = :id")
Optional<Order> findByIdWithItems(Long id);

// ✅ 方案三：投影接口（不需要完整聚合）
public interface OrderListView {
    Long getId();
    String getOrderNo();
    int getItemCount();   // 通过 JPQL 子查询
}
```

`spring.jpa.properties.hibernate.default_batch_fetch_size: 16` 已在 starter 配置，可缓解但不能根治 N+1。

### 慢查询监控

- Druid `slow-sql-millis: 1000`（开发期 500），自动记录到 Druid 监控
- MySQL 慢查询日志开启 `long_query_time = 1`
- 任何超过 1s 的查询**必须**优化（加索引 / 改 SQL / 加缓存）

### 索引设计

```java
@Table(name = "t_order", indexes = {
    @Index(name = "idx_tenant_status_created",
           columnList = "tenant_id, status, created_at"),
    @Index(name = "uk_order_no", columnList = "order_no", unique = true)
})
```

- 高频查询字段必须索引
- 多列索引最左前缀对齐查询条件
- 多租户场景 `tenant_id` 必须为索引前导列
- 索引数量 ≤ 5（写性能受影响），避免冗余

### 分页查询

```java
// ✅ 推荐：游标分页（大数据量）
@Query("SELECT o FROM Order o WHERE o.id > :lastId ORDER BY o.id ASC")
Slice<Order> findAfter(Long lastId, Pageable pageable);

// ⚠️ OFFSET 分页：> 10000 条数据时性能急剧下降
Page<Order> findAll(Pageable pageable);
```

深翻页（`page > 100`）**必须**改游标方式或限制最大 page。

## 连接池

### Druid（业务库）

```yaml
spring.datasource.druid:
  initial-size: 5
  min-idle: 10
  max-active: 50           # 单实例最大；总量 = max-active × 实例数
  max-wait: 60000          # 获取连接超时（毫秒）
  validation-query: SELECT 1
  test-while-idle: true
  time-between-eviction-runs-millis: 60000
  min-evictable-idle-time-millis: 300000
```

- `max-active` 不超过 DB 配置的 `max_connections / 实例数 × 0.7`
- 高峰长期占满 → 不要直接调大，先排查慢 SQL / 长事务

### Hibernate / JPA

- 已启用字节码增强（`enableAssociationManagement`）
- 批量写：`hibernate.jdbc.batch_size: 100` + `order_inserts: true`
- 关闭 `open-in-view: false`（防止视图层延迟加载）

## 缓存

详见 `14-cache.md`。性能相关重点：

- 高频查询（QPS > 100）**必须**缓存
- 列表缓存 TTL 5–15 分钟
- 缓存命中率 < 80% → 重新评估缓存策略

## 异步处理

### `@Async` 线程池

`eagle-common-starter` 的 `AsyncConfig` 已注册默认 `@Bean("taskExecutor")`，参数：

| 参数 | 值 |
|---|---|
| corePoolSize | CPU 核心数 |
| maxPoolSize | CPU × 2 |
| queueCapacity | 200（有界） |
| keepAlive | 60s |
| 拒绝策略 | `CallerRunsPolicy`（背压） |
| 优雅关闭 | `waitForTasksToComplete=true`，`awaitTermination=30s` |

```java
// ✅ 默认池（不指定 = 用 "taskExecutor"）
@Async
public void processNotification(...) { ... }

// ✅ 显式指定（推荐——便于排查 / 隔离）
@Async("taskExecutor")
public void processNotification(...) { ... }

// ✅ 高频独立池：业务关键路径配独立池避免互相阻塞
@Bean("messageTaskExecutor")
public TaskExecutor messageTaskExecutor() {
    ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
    exec.setCorePoolSize(10);
    exec.setMaxPoolSize(50);
    exec.setQueueCapacity(500);
    exec.setKeepAliveSeconds(60);
    exec.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    exec.setThreadNamePrefix("message-async-");
    exec.setWaitForTasksToCompleteOnShutdown(true);
    exec.setAwaitTerminationSeconds(30);
    exec.initialize();
    return exec;
}

@Async("messageTaskExecutor")
public void sendNotification(...) { ... }
```

- **禁止**用 `@Async` 默认 `SimpleAsyncTaskExecutor`（每次 new 线程）— `eagle-common-starter` 已替换
- **禁止**无界队列（`Integer.MAX_VALUE`）→ OOM 风险
- 高频独立池：日志、消息推送各自配置 `TaskExecutor` Bean，避免互相阻塞

### 阻塞 IO 隔离

WebFlux 服务（`eagle-gateway-server`）中**禁止**调用阻塞 API（JDBC、JPA、Feign）。如必需用 `Schedulers.boundedElastic()`：

```java
return Mono.fromCallable(() -> blockingDbCall())
    .subscribeOn(Schedulers.boundedElastic());
```

## 大对象与流式处理

```java
// ❌ 一次加载全部
List<Order> all = orderRepository.findAll();   // 几十万行 → OOM

// ✅ Stream 流式
@QueryHints(@QueryHint(name = HINT_FETCH_SIZE, value = "1000"))
Stream<Order> streamAll();

try (Stream<Order> stream = orderRepository.streamAll()) {
    stream.forEach(this::process);
}

// ✅ 分批
int page = 0;
Slice<Order> slice;
do {
    slice = orderRepository.findAll(PageRequest.of(page++, 1000));
    process(slice.getContent());
} while (slice.hasNext());
```

## HTTP 调用

- Feign 调用必须设置超时（`connectTimeout: 2s`，`readTimeout: 5s`）
- 不在事务内远程调用（详见 `08-concurrency.md`）
- 高 QPS 场景使用 OkHttp 连接池而非默认实现

## JVM / GC

- 默认 G1GC，堆内存按容器内存的 70% 设置
- 启动参数：`-XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError`
- **禁止**手动 `System.gc()`

## 监控指标

每个服务必须暴露 Micrometer 指标到 Prometheus：

| 指标 | 用途 |
|------|------|
| `http_server_requests_seconds` | API 响应时间 |
| `hikaricp_connections_active` / `druid.*` | 连接池压力 |
| `cache_gets_total` / `cache_puts_total` | 缓存命中率 |
| `jvm_gc_pause_seconds` | GC 停顿 |
| `executor_queue_remaining` | 异步线程池队列 |

报警阈值：P99 响应 > 1s / 错误率 > 1% / GC 暂停 > 500ms。

## 压测

- 新功能上线前必须压测：单接口 QPS、P95/P99、稳态吞吐
- 工具：`wrk` / `gatling` / `jmeter`
- 不可用生产数据库压测；用 staging 环境 + 数据脱敏

## 禁止清单

- 禁止全表 `findAll()` 用于业务接口
- 禁止 SELECT * 大表（用投影）
- 禁止循环内调用 DB / Redis / Feign
- 禁止业务方法持久化日志（用 ELK / 异步落库）
- 禁止 `Thread.sleep()` 替代异步等待
- 禁止禁用 GC 日志（生产排障所需）
