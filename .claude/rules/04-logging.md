# 日志规范（Logging）

## 日志框架

- 使用 SLF4J 接口，底层实现为 Logback
- 禁止直接使用 `System.out.println` 或 `e.printStackTrace()`

## 日志级别

| 级别 | 使用场景 |
|------|----------|
| `DEBUG` | 调试信息，仅开发/测试环境开启，禁止生产使用 |
| `INFO` | 系统运行关键节点（服务启动、核心业务流程完成） |
| `WARN` | 潜在问题，系统仍可运行（参数降级、重试、第三方超时） |
| `ERROR` | 系统错误，需人工介入（未预期异常、数据一致性问题） |

## 占位符

```java
// ✅ 正确：使用占位符（延迟求值）
log.info("order created, orderId: {}, userId: {}", orderId, userId);

// ❌ 错误：字符串拼接（level 未启用时仍执行拼接运算）
log.info("order created, orderId: " + orderId);
```

## 异常日志

```java
// ✅ 必须传入 Throwable，保留完整堆栈
log.error("payment failed, orderId: {}", orderId, e);

// ❌ 错误：只打 message，丢失堆栈
log.error("payment failed: {}", e.getMessage());
```

## 禁止

- 禁止在日志中输出密码、Token 明文、手机号、身份证号等敏感信息
- 禁止在循环体内打 INFO/ERROR 日志（循环结束后汇总输出）
- 禁止捕获异常后只打日志不处理（静默吞掉异常）
- 生产环境禁止开启 DEBUG 级别日志

## 日志内容规范

- 应包含足够上下文（业务 ID、操作人、关键参数），便于排查
- 建议格式：`[模块/操作] 描述, key1: {}, key2: {}`
