---
name: eagle-tracing
description: Use when configuring distributed tracing in eagle-cloud projects — Brave/B3/Zipkin integration, traceId/spanId MDC injection for log correlation, sampling probability
---

# eagle-tracing-starter — 分布式链路追踪（Brave / B3 / Zipkin）

## 何时使用

- 微服务跨服务调用追踪
- 日志关联（traceId 注入 MDC）
- 与 Feign / RocketMQ starter 自动协作

## 何时不要使用

- 单体应用无跨服务调用
- 不接入 Zipkin / Tempo（采集了不消费）

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-tracing-starter')
```

```yaml
eagle.tracing:
  sampling-probability: 0.1          # 0.0–1.0，生产建议 0.1
  zipkin:
    endpoint: ${ZIPKIN:http://zipkin:9411/api/v2/spans}
```

## 自动生效的能力

| 能力                        | 说明                           |
|---------------------------|------------------------------|
| traceId / spanId 自动生成     | 入口请求自动起 trace；下游延续           |
| MDC 注入                    | 日志 pattern `%X{traceId}` 可输出 |
| Feign B3 透传               | 自动注入下游                       |
| RocketMQ 透传               | 生产者注入、消费者还原                  |
| HTTP Server 自动 instrument | Web / WebFlux 自动起 Span       |

## Logback 日志格式（推荐）

```xml

<pattern>
    %d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level
    [traceId=%X{traceId},spanId=%X{spanId}]
    %logger{36} - %msg%n
</pattern>
```

## 自定义 Span（特殊场景）

```java

@RequiredArgsConstructor
public class ComplexService {
    private final Tracer tracer;     // Micrometer Tracing API

    public void doWork(Long userId) {
        Span span = tracer.nextSpan().name("complex-work").start();
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            span.tag("biz.userId", userId.toString());
            // 业务...
        } finally {
            span.end();
        }
    }
}
```

## 配置项

| key                                  | 类型     | 默认    | 说明                  |
|--------------------------------------|--------|-------|---------------------|
| `eagle.tracing.sampling-probability` | float  | `1.0` | 采样率（生产建议 0.1）       |
| `eagle.tracing.zipkin.endpoint`      | String | —     | Zipkin 上报地址（不配则不上报） |

⚠️ **没有 `eagle.tracing.enabled` 字段**——引入即生效，不引入即停用。

## 常见错误

- ❌ 生产采样率 1.0 → ✅ 0.05–0.1
- ❌ 业务代码手动 `MDC.put("traceId", ...)` → ✅ starter 自动注入
- ❌ 异步任务断链 → ✅ 用 starter 注册的 `taskExecutor`（已装饰）
- ❌ 配置 `eagle.tracing.enabled` → ✅ 该字段不存在
- ❌ 默认采样率以为是 `0.1` → ✅ 实际默认 **`1.0`**（全采样），生产必须显式调低

## 关联规则

- `.claude/rules/13-logging.md` — MDC + 日志格式
- `.claude/rules/24-deployment.md` — 监控接入
