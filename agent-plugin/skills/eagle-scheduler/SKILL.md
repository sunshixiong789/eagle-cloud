---
name: eagle-scheduler
description: Use when implementing distributed scheduled tasks in eagle-cloud projects — XXL-JOB integration, @XxlJob annotation, XxlJobHelper (getJobParam/getShardIndex/log), idempotency requirements
---

# eagle-scheduler-starter — XXL-JOB 分布式定时任务

## 何时使用

- 集群部署需分布式调度
- 可视化调度中心 + 任务监控 + 失败告警
- 大数据量分片处理
- 业务关键定时任务（订单超时关闭、对账、报表）

## 何时不要使用

- 单机本地任务（用 `@Scheduled`）
- 业务流程相关延迟（用 RocketMQ 延迟消息 / Redis 延迟队列）

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-scheduler-starter')
```

```yaml
eagle.xxl-job:
  enabled: true
  admin-addresses: ${XXL_ADMIN:http://xxl-job-admin:8080/xxl-job-admin}
  access-token: ${XXL_TOKEN:}
  app-name: ${spring.application.name}
  ip:                                   # 留空自动获取
  port: 0                                # 0 = 自动分配
  log-path: /data/applogs/xxl-job/jobhandler
  log-retention-days: 30
```

启动后会向调度中心注册 Executor，业务方在调度中心配置任务即可触发。

## 核心 API（XXL-JOB 原生）

| 注解 / 类                   | 用途                                                                         |
|--------------------------|----------------------------------------------------------------------------|
| `@XxlJob("handlerName")` | 任务方法注解（值是 handler 名，调度中心配置时填同名）                                            |
| `XxlJobHelper`           | 静态：`getJobParam` / `getShardIndex` / `getShardTotal` / `log(format, args)` |

业务无需引入 starter 自身的代码——本 starter 只做自动配置。

## 最小示例

```java

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderJobs {

    private final OrderApplicationService orderService;
    private final ReportService reportService;
    private final ReportRepository reportRepository;

    /** 普通任务：每 5 分钟取消超时订单 */
    @XxlJob("orderTimeoutHandler")
    public void cancelTimeoutOrders() {
        XxlJobHelper.log("start cancelTimeoutOrders");
        int count = orderService.cancelTimeoutOrders(Duration.ofMinutes(30));
        XxlJobHelper.log("cancelled {} orders", count);
        log.info("orderTimeout done: count={}", count);
    }

    /** 幂等：同日重复执行不重复出报表 */
    @XxlJob("dailyReportHandler")
    public void dailyReport() {
        LocalDate date = LocalDate.now().minusDays(1);
        if (reportRepository.existsByDate(date)) {
            XxlJobHelper.log("report exists for {}, skip", date);
            return;
        }
        reportRepository.save(reportService.generate(date));
    }

    /** 分片广播 */
    @XxlJob("syncBigTableHandler")
    public void syncBigTable() {
        int idx = XxlJobHelper.getShardIndex();
        int total = XxlJobHelper.getShardTotal();
        XxlJobHelper.log("processing shard {}/{}", idx, total);
        orderRepository.findByIdMod(idx, total).forEach(this::process);
    }

    /** 带参数 */
    @XxlJob("parameterizedJob")
    public void parameterized() {
        String params = XxlJobHelper.getJobParam();
        // 解析 JSON 参数...
    }
}
```

调度中心配置：

```
任务名: orderTimeoutHandler
JobHandler: orderTimeoutHandler
Cron: 0 */5 * * * ?
路由策略: 第一个 / 故障转移
失败重试: 3 次
告警邮件 / 钉钉
```

## 配置项

| key                                | 类型      | 默认                                    | 说明       |
|------------------------------------|---------|---------------------------------------|----------|
| `eagle.xxl-job.enabled`            | boolean | `true`                                | 总开关      |
| `eagle.xxl-job.admin-addresses`    | String  | `http://localhost:8080/xxl-job-admin` | 调度中心     |
| `eagle.xxl-job.access-token`       | String  | `""`                                  | 访问令牌     |
| `eagle.xxl-job.app-name`           | String  | `""`                                  | 执行器名     |
| `eagle.xxl-job.ip`                 | String  | `""`                                  | 留空自动     |
| `eagle.xxl-job.port`               | int     | `0`                                   | 0 = 自动分配 |
| `eagle.xxl-job.log-path`           | String  | `/data/applogs/xxl-job/jobhandler`    | 任务日志目录   |
| `eagle.xxl-job.log-retention-days` | int     | `30`                                  | 日志保留     |

## 常见错误

- ❌ 集群中用 `@Scheduled` 跑业务任务 → ✅ 改 `@XxlJob`
- ❌ 任务执行 > 30 min → ✅ 拆分或分片
- ❌ 不设置失败告警 → ✅ 调度中心配钉钉/企微
- ❌ 任务用 `Thread.sleep(很长)` → ✅ 拆分多次执行 + 状态记录
- ❌ 多租户全量扫描 → ✅ 任务参数声明 `tenantId` + 强制设置上下文

## 关联规则

- `.claude/rules/05-security.md` — `XxlJobHelper.log` vs SLF4J
