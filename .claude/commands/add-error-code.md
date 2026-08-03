---
description: 在指定 ErrorCode 枚举追加常量并同步在 i18n messages 文件中加翻译
argument-hint: "<ErrorCodeEnum>:<CONSTANT_NAME>:<中文消息>"
---

# /add-error-code — 添加错误码并同步 i18n

在指定的 ErrorCode 枚举中追加新常量，同时在 `messages_zh_CN.properties` / `messages_en_US.properties` /
`messages_zh_TW.properties` 三种语言文件中插入翻译占位。

## 输入

`$ARGUMENTS` 格式：`<ErrorCodeEnum>:<CONSTANT>:<defaultMessage>`

例：`OrderErrorCode:ORDER_TIMEOUT:订单超时`

或交互询问：

1. ErrorCode 枚举类（用项目搜索辅助补全）
2. 错误码常量名（CONSTANT_CASE）
3. 数值（自动取该枚举最大值 +1）
4. 默认消息（中文）
5. i18n key（自动按 `error.{module}.{action}` 推导，可改）
6. 英文翻译（必填）
7. 繁体中文翻译（默认与简体相同，可手改）

## 执行步骤

### 1. 解析 ErrorCode 枚举类

```bash
# 找到枚举文件
find . -name "{ErrorCodeEnum}.java"
```

读取现有枚举：

- 当前最大数值（推断所属域的码段）
- i18n key 命名习惯
- 已有常量列表（避免重名）

### 2. 追加枚举常量

```java
// 插入到枚举常量列表末尾（最后一个 ; 前）
{NEW_CONSTANT}({code},"error.{module}.{action}","{默认消息}"),
```

`code` 取所属枚举段位的下一个值（如 `30001-30099` 段，取最后一个 +1）。

### 3. 更新 messages 文件

定位 `src/main/resources/i18n/messages*.properties` 三套文件：

```properties
# messages_zh_CN.properties
error.{module}.{action} = {中文消息}
# messages_en_US.properties
error.{module}.{action} = {English message}
# messages_zh_TW.properties
error.{module}.{action} = {繁體中文}
```

按字母排序插入，保持文件整洁。

### 4. 校验

```bash
./gradlew :{service}:compileJava
```

确认枚举编译通过。

### 5. （可选）生成使用示例

```java
// ✅ 在领域层抛异常的示例
throw{ErrorCodeEnum}.{NEW_CONSTANT}.

toDomainException();

// ✅ 在仓储层（NotFound）
throw{ErrorCodeEnum}.{NEW_CONSTANT}.

toNotFoundException();

// ✅ 基础设施失败
throw{ErrorCodeEnum}.{NEW_CONSTANT}.

toServiceException(cause);
```

## 输出

```
=== /add-error-code 完成 ===

✅ 枚举更新：OrderErrorCode.ORDER_TIMEOUT (30015)
✅ messages_zh_CN.properties: error.order.timeout=订单超时
✅ messages_en_US.properties: error.order.timeout=Order has timed out
✅ messages_zh_TW.properties: error.order.timeout=訂單已超時
✅ 编译通过

使用示例：
  throw OrderErrorCode.ORDER_TIMEOUT.toDomainException();
```

## 参考规则

- `02-api-error.md` — 异常体系
- `00-core.md` — ErrorCode 命名
- `02-api-error.md` — i18n key 命名
