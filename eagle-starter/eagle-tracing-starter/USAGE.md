# eagle-tracing-starter — 分布式链路追踪（Brave / B3 / Zipkin）

## 何时使用

- 微服务架构跨服务调用追踪
- 排查跨服务性能问题（哪一跳慢）
- 与日志关联（traceId 自动注入 MDC）
- 与 `eagle-feign-starter` / `eagle-rocketmq-starter` 自动协作

## 何时不要使用

- 单体应用（无跨服务调用）
- 不接入 Zipkin / Tempo 等收集器（采集了不消费等于浪费）

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-tracing-starter')
```

```yaml
eagle.tracing:
  enabled: true
  sampling-rate: 0.1              # 采样率：0.0–1.0（生产建议 0.1）
  zipkin-endpoint: ${ZIPKIN:http://zipkin:9411/api/v2/spans}

management.tracing:
  sampling.probability: 0.1
```

## 核心能力（自动生效）

| 能力 | 说明 |
|---|---|
| traceId / spanId 自动生成 | 入口请求自动起 trace；下游服务延续 |
| MDC 自动注入 | 日志格式中 `%X{traceId}` 可输出（详见 `13-logging.md`） |
| Feign 透传 | B3 头自动透传到下游服务 |
| RocketMQ 透传 | 消息生产者注入、消费者还原 |
| HTTP Server 自动 instrument | Web 请求自动起 Span |

## 最小示例

```java
// ✅ 业务代码无需改动 — 自动生成 trace
@RestController
public class OrderController {
    @PostMapping("/orders")
    public OrderResponse create(@RequestBody CreateOrderRequest req) {
        return orderService.create(req);
    }
}

// ✅ 日志自动带 traceId
log.info("create order: userId={}", userId);
// 输出：[traceId=abc123,spanId=def456] create order: userId=42

// ✅ 自定义 Span（特殊场景）
@RequiredArgsConstructor
public class ComplexService {
    private final Tracer tracer;

    public void doWork() {
        Span span = tracer.nextSpan().name("complex-work").start();
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            span.tag("biz.userId", "42");
            // 业务...
        } finally {
            span.end();
        }
    }
}
```

## Logback 模板（推荐）

```xml
<!-- logback-spring.xml -->
<pattern>
  %d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level
  [traceId=%X{traceId},spanId=%X{spanId},userId=%X{userId},tenantId=%X{tenantId}]
  %logger{36} - %msg%n
</pattern>
```

## 配置项

| key | 类型 | 默认 | 说明 |
|---|---|---|---|
| `eagle.tracing.enabled` | boolean | `true` | 总开关 |
| `eagle.tracing.sampling-rate` | double | `0.1` | 采样率 |
| `eagle.tracing.zipkin-endpoint` | String | — | Zipkin 上报地址 |
| `management.tracing.sampling.probability` | double | `0.1` | Spring Boot Tracing 采样 |

## 常见错误

- ❌ 生产采样率 1.0 → ✅ 0.05–0.1（全采样性能开销大）
- ❌ 业务代码手动 put `MDC.put("traceId", ...)` → ✅ starter 自动注入
- ❌ 异步任务断链（不用 `eagleTaskExecutor`）→ ✅ 使用统一线程池（已装饰）
- ❌ 不输出到 Zipkin → ✅ 配置 `zipkin-endpoint` 并验证可达
- ❌ 线上发现 traceId 空白 → ✅ 检查 Logback pattern 是否含 `%X{traceId}`

## 关联规则

- `.claude/rules/13-logging.md` — MDC + 日志格式
- `.claude/rules/24-deployment.md` — 监控接入
- `.claude/rules/23-performance.md` — P99 监控
