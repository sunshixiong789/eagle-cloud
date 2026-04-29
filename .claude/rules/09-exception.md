# 异常处理规范（Exception Handling）

## 异常体系

```
AppException（抽象基类，持有 ErrorCode）
├── NotFoundException    → 404
├── ConflictException    → 409
├── DomainException      → 400（领域验证失败）
└── ServiceException     → 500（基础设施故障）
```

## 抛出异常：统一使用 ErrorCode 工厂方法

```java
// ❌ 禁止直接使用 Java 标准异常
throw new IllegalArgumentException("用户名不能为空");

// ✅ 使用 ErrorCode 工厂方法
throw OrderErrorCode.ORDER_NOT_FOUND.toNotFoundException();        // → 404
throw OrderErrorCode.ORDER_ALREADY_CLOSED.toDomainException();     // → 400
throw PaymentErrorCode.GATEWAY_ERROR.toServiceException(cause);    // → 500
```

## 新增错误码

在对应枚举文件加一行常量 + i18n 消息文件加翻译：

```java
ORDER_ITEM_LIMIT_EXCEEDED(30005, "error.order.item_limit", "订单项超出上限");
```

## 各层异常职责

| 层 | 职责 |
|----|------|
| **Controller** | 不得捕获异常，只做入参校验和响应封装 |
| **Application** | 只捕获**可处理**的异常，无法处理的向上抛出 |
| **Domain** | 使用 ErrorCode 工厂方法抛出 `DomainException` |
| **Infrastructure** | 外部服务异常转换为 `ServiceException`，不直接上抛底层异常 |

## 规范

- 禁止自定义新的 Exception 子类（现有四层已覆盖所有场景）
- 禁止硬编码整数错误码，统一使用 ErrorCode 枚举
- 捕获异常后必须记录日志（含 Throwable），禁止静默吞掉
- i18n：错误消息通过 key 传递，全局异常处理器按 `Accept-Language` 自动解析
