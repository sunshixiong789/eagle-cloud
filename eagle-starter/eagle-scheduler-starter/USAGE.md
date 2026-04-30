# eagle-scheduler-starter — XXL-JOB 分布式定时任务

## 何时使用

- 集群部署需要分布式调度（避免重复执行）
- 需要可视化调度中心 / 任务监控 / 失败告警
- 大数据量分片处理
- 业务关键定时任务（订单超时关闭、对账、报表）

## 何时不要使用

- 单机本地任务（用 `@Scheduled` 即可）
- 与业务流程紧耦合的延迟任务（用 RocketMQ 延迟消息 / Redis 延迟队列）

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-scheduler-starter')
```

```yaml
xxl.job:
  admin:
    addresses: ${XXL_ADMIN:http://xxl-job-admin:8080/xxl-job-admin}
  executor:
    appname: ${spring.application.name}
    address:
    ip:
    port: 9999                       # executor 端口（容器内）
    log-path: /data/applogs/xxl-job/jobhandler
    log-retention-days: 30
  access-token: ${XXL_TOKEN}

eagle.xxl-job:
  enabled: true
```

## 核心 API

| 注解 / 类 | 用途 |
|---|---|
| `@XxlJob` | 任务方法注解（值为 handler 名）|
| `XxlJobHelper` | 任务工具：`getJobParam` / `getShardIndex` / `getShardTotal` / `log` |

业务通过实现方法 + 在调度中心配置 cron 完成调度。

## 最小示例

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderTimeoutJob {

    private final OrderApplicationService orderService;
    private final ReportRepository reportRepository;

    /** 普通任务：每 5 分钟取消超时未支付订单 */
    @XxlJob("orderTimeoutHandler")
    public void cancelTimeoutOrders() {
        XxlJobHelper.log("start cancelTimeoutOrders");
        int count = orderService.cancelTimeoutOrders(Duration.ofMinutes(30));
        XxlJobHelper.log("cancelled {} orders", count);
        log.info("orderTimeout job done: count={}", count);
    }

    /** 幂等任务：每日报表（同日重复执行不重复出报表） */
    @XxlJob("dailyReportHandler")
    public void dailyReport() {
        LocalDate date = LocalDate.now().minusDays(1);
        if (reportRepository.existsByDate(date)) {
            XxlJobHelper.log("report exists for {}, skip", date);
            return;
        }
        reportRepository.save(reportService.generate(date));
    }

    /** 分片任务：100 万行数据并行处理 */
    @XxlJob("syncBigTableHandler")
    public void syncBigTable() {
        int idx = XxlJobHelper.getShardIndex();
        int total = XxlJobHelper.getShardTotal();
        XxlJobHelper.log("processing shard {}/{}", idx, total);

        orderRepository.findByIdMod(idx, total).forEach(this::process);
    }
}
```

调度中心配置：

```
任务名称: orderTimeoutHandler
JobHandler: orderTimeoutHandler
Cron: 0 */5 * * * ?
路由策略: 第一个 / 故障转移
失败重试: 3 次
告警邮件: oncall@eagle.com
```

## 配置项

| key | 类型 | 默认 | 说明 |
|---|---|---|---|
| `eagle.xxl-job.enabled` | boolean | `true` | 总开关 |
| `xxl.job.admin.addresses` | String | — | 调度中心地址 |
| `xxl.job.executor.appname` | String | 应用名 | 执行器名 |
| `xxl.job.executor.port` | int | `9999` | 执行器端口 |
| `xxl.job.access-token` | String | — | 访问令牌 |

## 常见错误

- ❌ 任务非幂等 → ✅ 必须按状态 / 游标判断（详见 `27-scheduling.md`）
- ❌ 集群中用 `@Scheduled` → ✅ 改用 `@XxlJob`
- ❌ 任务执行 > 30 min → ✅ 拆分或分片
- ❌ 不设置失败告警 → ✅ 调度中心邮件 + 钉钉
- ❌ 任务直接 `Thread.sleep(很长)` → ✅ 拆分多次执行 + 状态记录
- ❌ 多租户任务全量扫描 → ✅ 任务参数声明 `tenantId`，强制设置上下文

## 关联规则

- `.claude/rules/27-scheduling.md` — 路由策略 / 分片 / 幂等 / 超时
- `.claude/rules/17-tenant-permission.md` — 多租户任务上下文
- `.claude/rules/13-logging.md` — `XxlJobHelper.log` vs SLF4J
