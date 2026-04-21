# 异常处理规范（Exception Handling）

## 异常体系

采用统一的类型化异常体系，所有业务异常继承 `AppException`：

```
AppException（抽象基类，持有 ErrorCode）
├── NotFoundException    → HTTP 404（资源不存在）
├── ConflictException    → HTTP 409（资源冲突）
├── DomainException      → HTTP 400（领域验证失败、业务不变性违反）
└── ServiceException     → HTTP 500（基础设施故障、外部服务失败）
```

## ErrorCode 枚举体系

`ErrorCode` 是接口，各业务域有独立的枚举实现（如 `OrderErrorCode`、`PaymentErrorCode`）。每个枚举常量包含：数字码、i18n key、默认消息。

## 抛出异常：统一使用 ErrorCode 工厂方法

**禁止直接使用 Java 标准异常表达业务错误：**

```java
// ❌ 禁止
throw new IllegalArgumentException("用户名不能为空");
throw new IllegalStateException("订单已关闭");
throw new RuntimeException("资源不存在");

// ✅ 正确：使用 ErrorCode 工厂方法
throw OrderErrorCode.ORDER_NOT_FOUND.toNotFoundException();
throw OrderErrorCode.ORDER_ALREADY_CLOSED.toDomainException();
throw PaymentErrorCode.PAYMENT_GATEWAY_ERROR.toServiceException(cause);
```

**各工厂方法对应的 HTTP 状态：**

| 方法 | HTTP 状态 | 适用场景 |
|------|-----------|----------|
| `.toNotFoundException()` | 404 | 资源不存在 |
| `.toConflictException()` | 409 | 资源冲突（重复创建等）|
| `.toDomainException()` | 400 | 领域验证失败、状态不变性违反 |
| `.toServiceException()` | 500 | 基础设施故障、外部服务失败 |
| `.toServiceException(Throwable)` | 500 | 带原因链的服务故障 |

## 新增错误码

只需在对应枚举文件加一行常量：

```java
// 1. 在对应枚举中加常量（数字码、i18n key、默认消息）
ORDER_ITEM_LIMIT_EXCEEDED(30005, "error.order.item_limit", "订单项超出上限");

// 2. 在 i18n 消息文件中各加一行（中文/英文）

// 3. 使用
throw OrderErrorCode.ORDER_ITEM_LIMIT_EXCEEDED.toDomainException();
```

## 国际化消息

所有错误消息通过 i18n key 传递，全局异常处理器自动解析：
- key 存在 → 返回对应语言翻译
- key 不存在 → 原文降级返回（向后兼容）
- 语言由 `Accept-Language` 请求头决定

## 全局异常处理器行为

全局异常处理器统一处理所有异常，**Controller 层禁止 try-catch**：

| 异常类型 | HTTP 状态 | 响应含 errorCode |
|----------|-----------|-----------------|
| `NotFoundException` | 404 | ✅ |
| `ConflictException` | 409 | ✅ |
| `DomainException` | 400 | ✅ |
| `ServiceException` | 500 | ✅ |
| `MethodArgumentNotValidException` | 400 | ❌ |
| `AccessDeniedException` | 403 | ❌ |
| `Exception`（兜底）| 500 | ❌ |

## 各层异常职责

| 层 | 职责 |
|----|------|
| **Controller** | 不得捕获异常，只做入参校验和响应封装 |
| **Application** | 只捕获**可处理**的异常，无法处理的向上抛出；捕获后必须记录日志 |
| **Domain** | 使用 ErrorCode 工厂方法抛出 `DomainException`（领域规则违反）|
| **Infrastructure** | 外部服务异常转换为 `ServiceException`，不直接上抛底层异常 |

## 其他规范

- `finally` 块中禁止 `return` 语句
- 捕获异常后必须记录日志（含 Throwable），禁止静默吞掉
- 禁止硬编码整数错误码，统一使用 ErrorCode 枚举
- 禁止自定义新的 Exception 子类（现有四层已覆盖所有场景）
