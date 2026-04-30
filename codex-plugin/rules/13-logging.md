# 日志规范（Logging）

技术栈：SLF4J 门面 + Logback 实现，整合 `eagle-tracing-starter`（Brave/Zipkin）输出 traceId。

## 声明 Logger

```java
// ✅ 使用 Lombok @Slf4j（推荐）
@Slf4j
@Service
public class OrderApplicationService {
}

// ✅ 显式声明也可
private static final Logger log = LoggerFactory.getLogger(OrderApplicationService.class);

// ❌ 禁止
private Logger log = LoggerFactory.getLogger(this.getClass());  // 实例字段
public static Logger LOG = ...;                                  // 公开/可变
```

## 占位符 vs 字符串拼接

```java
// ✅ 正确：占位符 — 仅在日志启用时格式化
log.info("user {} logged in from {}",userId, ip);

// ✅ 异常必须作为最后一个独立参数（不进入占位符）
log.

error("failed to process order {}",orderId, ex);

// ❌ 禁止：字符串拼接（即使日志被禁用也会执行字符串构造）
log.

info("user "+userId +" logged in");

// ❌ 禁止：把异常拼到 message
log.

error("failed: "+ex.getMessage());        // 丢堆栈
        log.

error("failed: {}",ex);                    // 占位符吞堆栈
```

## 日志级别选择

| 级别      | 使用场景                              |
|---------|-----------------------------------|
| `ERROR` | 业务失败但服务可继续运行；外部依赖异常；需立即告警的事件      |
| `WARN`  | 可恢复的异常路径；接近限流阈值；降级开关触发；废弃 API 被调用 |
| `INFO`  | 重要业务节点（订单创建、支付成功）；启动/停止/配置加载      |
| `DEBUG` | 开发期调试细节；外部请求/响应体；SQL 参数           |
| `TRACE` | 高频内部细节（循环内、拦截器）                   |

- 生产环境默认 `INFO`，关键 starter 可调到 `WARN`
- **禁止**所有路径都用 `INFO`（噪声淹没真问题）
- **禁止**用 `ERROR` 记录预期内的业务异常（404、参数校验失败）

## 核心操作必须埋点

**判定"核心操作"的标准**（满足任一即是）：

- 触发**状态机变更**的聚合根方法（订单创建/支付/取消/退款、用户注册/注销、权限授予/回收）
- 涉及**金额 / 库存 / 配额**的写操作
- **跨服务**的远程调用入口与出口（Feign 调用、MQ 发送/消费）
- **分布式事务 / 分布式锁 / 缓存双写**的关键节点
- **外部依赖**的调用边界（支付网关、短信、OSS、第三方 API）
- **定时任务 / 异步任务**的开始与结束
- **登录 / 登出 / 鉴权 / Token 刷新 / 权限校验失败**

**INFO vs DEBUG 选择**：

| 场景                     | 级别                     | 示例                                                                 |
|------------------------|------------------------|--------------------------------------------------------------------|
| 状态机变更（业务关键里程碑）         | `INFO`                 | `order created`、`payment succeeded`、`user registered`              |
| 跨服务 / 外部依赖调用结果         | `INFO`（成功）/ `WARN`（异常） | `feign call to inventory completed in 120ms`                       |
| MQ 发送 / 消费 / 分布式事务关键阶段 | `INFO`                 | `event published, topic=...`、`global tx committed`                 |
| 异步任务起止                 | `INFO`                 | `job orderTimeoutHandler started`、`processed 1234 records in 3.2s` |
| 入参 / 出参 / SQL 参数       | `DEBUG`                | `request body: {...}`、`query result size=42`                       |
| 中间过程 / 内部决策分支          | `DEBUG`                | `cache miss, fallback to db`                                       |
| 高频循环内细节                | `TRACE`                | `processing item 12345/1000000`                                    |

**埋点模板**（以"创建订单"为例）：

```java
// ✅ 入口 INFO：标识业务起点（关键参数 ≤ 3 个，避免膨胀）
log.info("create order: userId={}, productCount={}, totalAmount={}",
         userId, items.size(),totalAmount);

// ✅ 中间细节 DEBUG：调试用，生产默认不输出
        log.

debug("order items: {}",items);

// ✅ 状态机变更 INFO：业务里程碑
log.

info("order created: orderId={}, orderNo={}",order.getId(),order.

getOrderNo());

// ✅ 跨服务调用 INFO：含耗时
long start = System.currentTimeMillis();
inventoryFeignClient.

lockStock(items);
log.

info("inventory locked: items={}, costMs={}",items.size(),System.

currentTimeMillis() -start);

// ✅ 警告路径 WARN：预期异常但有业务影响
        log.

warn("stock insufficient, fallback to backorder: orderId={}",order.getId());

// ✅ 失败路径 ERROR + 堆栈
        catch(
PaymentException ex){
        log.

error("payment failed: orderId={}",order.getId(),ex);
        throw PaymentErrorCode.GATEWAY_ERROR.

toServiceException(ex);
}
```

**禁止**：

- 无脑给所有 Controller / Service 方法包 `log.info("entering xxx")` / `log.info("exiting xxx")`（用 AOP 统一埋点替代）
- 在 `for` 循环内 `log.info`（用 `DEBUG` 或循环外汇总后单条 `INFO`）
- 同一动作多个层各自 `log.info` 重复（应用层一条足够，基础设施层用 `DEBUG`）
- 把入参 / 出参 / 完整请求体打到 `INFO`（敏感泄漏 + 噪声 → 一律 `DEBUG`）

**结构化字段建议**：

INFO 日志写"业务关键 ID + 关键指标"，**不写**完整对象。便于 ELK 按字段过滤：

```java
// ✅ 关键 ID + 指标（结构化检索友好）
log.info("order paid: orderId={}, amount={}, channel={}, costMs={}",
         orderId, amount, channel, costMs);

// ❌ 完整对象 dump
log.

info("order paid: {}",order);   // 噪声 + 可能含敏感字段
```

## MDC（Mapped Diagnostic Context）

由 `eagle-tracing-starter` + 网关过滤器自动注入，业务代码**不应**手动 put：

| Key         | 来源                     | 说明                    |
|-------------|------------------------|-----------------------|
| `traceId`   | tracing-starter        | 全链路追踪 ID（B3）          |
| `spanId`    | tracing-starter        | 当前 Span               |
| `userId`    | resource-server filter | 当前登录用户 ID             |
| `tenantId`  | tenant-starter         | 当前租户 ID               |
| `requestId` | gateway                | 请求唯一 ID（X-Request-Id） |

业务自定义 MDC 必须在 `try { MDC.put(...); ... } finally { MDC.remove(...); }` 中成对使用，防止线程池污染。

## 日志格式（Logback 配置）

```
%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [traceId=%X{traceId},userId=%X{userId},tenantId=%X{tenantId}] %logger{36} - %msg%n
```

生产环境推荐 JSON 格式（`logstash-logback-encoder`），便于 ELK 采集。

## 异常日志

```java
// ✅ 必须含完整堆栈
try{
        paymentGateway.pay(request);
}catch(
PaymentException ex){
        log.

error("payment failed, orderNo={}",request.getOrderNo(),ex);
        throw PaymentErrorCode.GATEWAY_ERROR.

toServiceException(ex);
}

// ❌ 禁止：吞掉异常 / 丢堆栈
        catch(
Exception e){ /* ignore */ }
        catch(
Exception e){log.

error(e.getMessage());}
        catch(
Exception e){log.

error("failed: "+e); }
```

`AppException`（业务可预期异常）通常 `log.warn` 即可，无需打印堆栈：

```java
catch(DomainException ex){
        log.

warn("domain rule violated: {}",ex.getErrorCode(),ex);
        throw ex;
}
```

## 敏感数据脱敏

**禁止**直接打印以下字段；使用项目工具 `SensitiveLogger.mask(...)` 或 `@ToString.Exclude`：

| 字段                    | 处理方式                 |
|-----------------------|----------------------|
| 密码 / 密钥               | 完全省略，绝不输出            |
| Token / Refresh Token | 仅打印前 8 位 + `***`     |
| 手机号                   | `138****1234`        |
| 身份证                   | `110***********1234` |
| 银行卡                   | 仅末 4 位               |
| 完整请求体含敏感字段            | 使用 DTO 时排除 / 自定义序列化器 |

```java
// ❌ 禁止：直接打印整个请求对象
log.info("login request: {}",loginRequest);  // 会暴露密码

// ✅ 选择性记录
log.

info("login attempt: username={}",loginRequest.getUsername());
```

## 高频路径性能

```java
// ✅ 大对象 / 计算耗时的日志使用 isDebugEnabled 守卫
if(log.isDebugEnabled()){
        log.

debug("aggregate state: {}",aggregate.dumpFullState());
        }
```

普通占位符日志（`log.debug("x={}", x)`）**无需**守卫——SLF4J 的占位符已是惰性求值。

## 输出位置

- **禁止** `System.out` / `System.err` / `printStackTrace()`
- **禁止**写文件、网络等自定义 Appender 之外的 IO
- 日志文件位置由运维统一规划（`/var/log/eagle/{service}/`）

## CHANGELOG / 业务事件

业务关键事件（订单创建、支付完成、用户注册）**通过领域事件发布**，由 `infrastructure/event/` 统一异步落库或推送 ELK，**不**靠
`log.info` 来"记录历史"。
