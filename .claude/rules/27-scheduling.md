# 定时任务规范

技术栈：`eagle-scheduler-starter`（基于 XXL-JOB 2.4.2）。

## 何时用 XXL-JOB vs Spring `@Scheduled`

| 场景 | 选型 |
|------|------|
| 集群部署 / 需要分片 / 失败转移 | **XXL-JOB** |
| 单机本地任务（启动初始化、内存清理）| `@Scheduled` |
| 业务关键 / 需要监控 / 需可视化 | **XXL-JOB** |
| 简单 cron / 不影响业务 | `@Scheduled` |

**禁止**：生产环境用 `@Scheduled` 跑业务关键任务（多实例并发 / 不可视化 / 不可重试）。

## XXL-JOB 任务定义

```java
@Component
@RequiredArgsConstructor
public class OrderTimeoutJob {

    private final OrderApplicationService orderService;

    @XxlJob("orderTimeoutHandler")
    public void execute() {
        String params = XxlJobHelper.getJobParam();
        XxlJobHelper.log("start orderTimeoutHandler, params={}", params);

        try {
            int count = orderService.cancelTimeoutOrders();
            XxlJobHelper.log("cancelled {} timeout orders", count);
        } catch (Exception ex) {
            XxlJobHelper.log("orderTimeoutHandler failed", ex);
            throw ex;   // 重新抛出触发 XXL-JOB 失败重试
        }
    }
}
```

- 任务名（`@XxlJob` 值）使用 lowerCamelCase + `Handler` 后缀
- **必须**在 XXL-JOB 调度中心注册，配置 cron / 路由策略 / 重试 / 告警
- 业务异常**抛出**而非吞掉，让调度中心感知失败

## 路由策略

| 策略 | 适用 |
|------|------|
| **第一个** | 单机执行任务（默认） |
| **轮询** | 无状态、可负载均衡 |
| **一致性 Hash** | 与数据分片对齐 |
| **故障转移** | 高可用：主节点失败自动切到备 |
| **分片广播** | 大数据量并行处理（必须配套分片逻辑）|

## 分片任务

```java
@XxlJob("syncBigTableHandler")
public void execute() {
    int shardIndex = XxlJobHelper.getShardIndex();
    int shardTotal = XxlJobHelper.getShardTotal();

    // ✅ 按主键取模分片，每个 executor 处理自己的分片
    orderRepository.findByIdMod(shardIndex, shardTotal)
        .forEach(this::process);
}
```

适用：百万级数据批处理、报表生成、清理任务。

## 幂等性（必须）

调度中心可能重试 → 任务**必须幂等**：

```java
// ✅ 方案一：先标记 → 再处理 → 最后清标记（DB 状态机）
@XxlJob("dailyReportHandler")
public void execute() {
    LocalDate yesterday = LocalDate.now().minusDays(1);
    if (reportRepository.existsByDate(yesterday)) {
        XxlJobHelper.log("report for {} already exists, skip", yesterday);
        return;
    }
    Report report = reportService.generate(yesterday);
    reportRepository.save(report);
}

// ✅ 方案二：批处理任务记录 last processed cursor
Long lastProcessedId = cursorRepository.getLast("orderSync");
List<Order> batch = orderRepository.findAfter(lastProcessedId, 1000);
batch.forEach(this::process);
cursorRepository.update("orderSync", batch.lastId());
```

**禁止**任务依赖"上次执行成功的状态"——上次失败下次必须能继续。

## 任务超时

```java
@XxlJob("longRunningHandler")
public void execute() {
    Instant deadline = Instant.now().plus(Duration.ofMinutes(10));
    while (hasMoreData() && Instant.now().isBefore(deadline)) {
        processOneBatch();
    }
    if (hasMoreData()) {
        XxlJobHelper.log("partial work done, will continue next run");
    }
}
```

- 调度中心设置任务超时（默认建议 5 分钟）
- 长任务必须**断点续传**——超时后下次能继续，不重头跑

## 长任务拆分

```java
// ❌ 禁止：单次任务跑 30 分钟
@XxlJob("hugeBatchJob")  // 一次处理 500 万行

// ✅ 拆分：分片广播 + 每片处理 5 万行
@XxlJob("shardedBatchJob")  // 100 片 × 5 万行
```

## 重试与告警

| 配置 | 推荐 |
|------|------|
| 失败重试 | 3 次（指数退避） |
| 阻塞策略 | 单机串行（禁止重复触发） |
| 告警邮件 | 必填到 oncall |

调度中心**告警必须接入企微 / 钉钉**，邮件容易漏。

## Cron 表达式

```
0 0/5 * * * ?       # 每 5 分钟
0 0 2 * * ?         # 每天凌晨 2 点
0 30 1 * * 1        # 每周一凌晨 1:30
0 0 0 1 * ?         # 每月 1 号 0 点
```

- **禁止**秒级触发（`* * * * * ?`）— 用消息驱动替代
- 错峰调度：避免大量任务在 `0:00` / `00:00` 同时启动（DB 压力）
- 时区：调度中心统一设置项目时区（`Asia/Shanghai`）

## 任务参数

```java
@XxlJob("parameterizedJob")
public void execute() {
    String json = XxlJobHelper.getJobParam();
    JobParam param = objectMapper.readValue(json, JobParam.class);
    // 处理...
}
```

- 参数**必须 JSON**，禁止 key=value 自定义解析
- 参数解析失败 → 任务失败，**不**默认值兜底

## 日志

- `XxlJobHelper.log(...)` 输出到调度中心面板（运维可见）
- 业务详细日志走 SLF4J（详见 `13-logging.md`），**不要**全量塞调度日志
- 每次执行**必须**记录：开始时间、处理条数、耗时、结果

## 数据隔离（多租户）

```java
// ✅ 任务参数声明 tenantId，强制设置上下文
@XxlJob("tenantOrderCleanup")
public void execute() {
    JobParam param = parse(XxlJobHelper.getJobParam());
    TenantContextHolder.setCurrentTenantId(param.getTenantId());
    try {
        orderService.cleanup();
    } finally {
        TenantContextHolder.clear();
    }
}
```

**禁止**任务跨租户全量扫描（除非超管运维任务）。

## Spring `@Scheduled`（仅限单机）

```java
// ✅ 仅用于本地缓存刷新等内部任务
@Scheduled(fixedDelay = 60_000)
public void refreshLocalCache() { ... }
```

- 单机部署 / 容器副本 = 1 才使用
- 集群中**绝对禁止**做业务关键调度

## 禁止清单

- 禁止任务非幂等
- 禁止任务依赖外部系统的强一致性（应通过补偿任务收敛）
- 禁止任务执行时间 > 30 分钟（拆分或分片）
- 禁止生产任务无告警
- 禁止生产任务无超时
- 禁止集群中用 `@Scheduled` 跑业务任务
- 禁止任务参数解析失败兜底默认值
- 禁止任务直接 `Thread.sleep` 长时间等待
