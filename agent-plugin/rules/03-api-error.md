# 接口、异常与错误码

## RESTful 约定

- URL 用名词复数、kebab-case：`/users`、`/membership-cards`；嵌套表从属：`/orders/{orderId}/items`
- **URL 中禁止出现动词**（用 HTTP 方法 + 名词资源）
- 管理端收敛到 `/admin/**`；内部回调 / webhook 放 `/internal/**`
- 版本走路径 `/api/v1/...`；破坏性变更才开 `/api/v2/`，v1 保留 ≥ 3 个月并标 `@Deprecated`

## 响应格式：不用包装类

**直接返回数据，不用 `ApiResult` 包装**（仓库中确无此类），语义由 HTTP 状态码传递。

- 请求 / 响应 DTO 一律 `record`（见 `01-java25.md`）
- 分页用 Spring Data `Page<T>`，列表查询用投影接口
- 创建成功返 `201`（`@ResponseStatus(HttpStatus.CREATED)`）
- 金额返回原始 `BigDecimal`、日期返回 ISO 8601，由前端按 locale 格式化

## 权限声明

`eagle-resource-server-starter` 的 filter chain 已是 `permitPaths.permitAll() + anyRequest().authenticated()`，**默认就要求登录**。由此：

```java
// ❌ 多余：filter chain 已强制登录（存量违例 13 处，新代码不得新增）
@PreAuthorize("isAuthenticated()")
@GetMapping("/me")
public UserResponse me() { ... }

// ✅ 直接写
@GetMapping("/me")
public UserResponse me() { ... }
```

```java
// ❌ 无效：不会让接口变公开，路径不在白名单仍 401（存量违例 4 处）
@PreAuthorize("permitAll()")
```

**公开接口靠 yml 放行**（单一信息源），Controller 上只留一行注释标识：

```yaml
eagle:
  resource-server:
    permit-paths: [/products/**, /banners, /internal/orders/callback]
```

**只有角色 / SpEL / 数据级判断才写 `@PreAuthorize`** —— 这是主流用法（`hasRole('admin')` 现有 44 处）：

```java
@PreAuthorize("hasRole('admin')")
@PreAuthorize("hasAnyRole('super_admin','operator')")
@PreAuthorize("#accountId == authentication.principal.id")
@PreAuthorize("hasRole('admin') or #accountId == authentication.principal.id")
```

## 入参校验

请求 `record` 用 Bean Validation，Controller 参数加 `@Valid`；`@PathVariable` / `@RequestParam` 同样要校验。

校验消息用 `{key}` 引用 i18n，**不硬编码中文**：

```java
public record CreateOrderRequest(
        @NotBlank(message = "{validation.order.no.required}") String orderNo,
        @NotNull @Min(value = 1, message = "{validation.order.amount.min}") BigDecimal totalAmount
) { }
```

## CORS

```java
config.setAllowedOriginPatterns(List.of("https://*.eagle.com"));  // 生产必须枚举具体域名
config.setAllowCredentials(true);
```

**禁止** `setAllowedOrigins(List.of("*"))` 与 `allowCredentials(true)` 同时使用（不兼容）。JWT 模式下 CSRF 默认关闭。

---

# 异常体系

```text
AppException（抽象基类，持有 ErrorCode）
├── NotFoundException    → 404
├── ConflictException    → 409
├── DomainException      → 400（领域验证失败）
└── ServiceException     → 500（基础设施故障）
```

**禁止自定义新的 Exception 子类**，**禁止**直接抛 Java 标准异常：

```java
// ❌
throw new IllegalArgumentException("用户名不能为空");

// ✅ 统一 ErrorCode 工厂方法
throw OrderErrorCode.ORDER_NOT_FOUND.toNotFoundException();
throw OrderErrorCode.ORDER_ALREADY_CLOSED.toDomainException();
throw PaymentErrorCode.GATEWAY_ERROR.toServiceException(cause);
```

**选型注意**：现状 `toDomainException` 98 次、`toNotFoundException` 仅 4 次 —— 存在"查不到也抛 400"的语义漂移。**资源不存在必须用 `toNotFoundException()`**，让前端能按 404 区分，不要一律 `DomainException`。

## 各层职责

| 层 | 职责 |
|---|---|
| Controller | **禁止 try-catch**，只做入参校验和响应封装 |
| Application | 只捕获**可处理**的异常，其余上抛 |
| Domain | 用 ErrorCode 工厂方法抛 `DomainException` |
| Infrastructure | 外部异常转 `ServiceException`，不直接上抛底层异常 |

## 定义 ErrorCode 枚举

只有三个固定元素；`getCode()` / `getMessageKey()` / `getDefaultMessage()` 由接口 default 方法委托给 `Meta`，**实现类不覆写**：

```java
public enum OrderErrorCode implements ErrorCode {

    ORDER_NOT_FOUND(50001, "error.order.not_found", "订单不存在"),
    ORDER_ALREADY_PAID(50002, "error.order.already_paid", "订单已支付");

    private final ErrorCode.Meta meta;                      // 唯一字段

    OrderErrorCode(int code, String messageKey, String defaultMessage) {
        this.meta = new ErrorCode.Meta(code, messageKey, defaultMessage);
    }

    @Override public ErrorCode.Meta meta() { return meta; } // 唯一覆写
}
```

## 号段分配（新建枚举前必查）

错误码是**对外 API 契约**，已发布的号不可变更。当前占用：

| 号段 | 归属 |
|---|---|
| 10000–10999 | 用户域 `UserErrorCode` |
| 11000–11999 | 认证域 `AuthErrorCode` |
| 12000–15999 | common-starter：`DataErrorCode` / `OperationErrorCode` / `FileErrorCode` / `ExternalErrorCode` |
| 16000–16999 | rocketmq-starter |
| 17000–17999 | notification-starter |
| 20000–29999 | 系统管理域 `SystemErrorCode` |
| 30500–30599 | 消息 / 公告域 |
| 40000–40999 | ⚠️ **撞号 `40001-40003`**：`FileErrorCode`(system) 与 `IdempotencyErrorCode`(starter) |
| 90000–90999 | ⚠️ **撞号 `90001-90004`**：`LockErrorCode` 与 `AiErrorCode` |

**新增取号**：starter 用 `18000–19999`；业务域用 `50000–89999`。**禁止**复用上表号段。

两组历史撞号（`40001-40003`、`90001-90004`）需单独评估修复，**不要在无关 PR 里顺手改号**。

## i18n

- `messageKey` 格式 `error.{domain}.{result}`，与枚举常量对齐：`ORDER_NOT_FOUND` → `error.order.not_found`
- **三语文件必须同步**：`messages.properties`(zh_CN) / `messages_en_US` / `messages_zh_TW`
- 占位符用 `{0} {1}`（`MessageFormat`），**不用** `${}`（与 SpEL 冲突）
- properties 文件必须 UTF-8
- 第三个构造参数是 i18n 缺失时的兜底
- **业务日志不做 i18n**，只有最终用户可见的消息才需要

## 错误响应

由全局异常处理器统一返回，`errorCode` 仅 `AppException` 子类时携带：

```json
{
  "timestamp": "2026-04-12T09:00:00Z", "status": 404, "error": "Not Found",
  "message": "订单不存在", "path": "/api/orders/999", "errorCode": 50001
}
```

**禁止**在 message 中暴露表名、堆栈、SQL。

---

# OpenAPI

- Controller：类级 `@Tag` + 方法级 `@Operation`，必要时 `@ApiResponses` 标注错误码
- DTO 字段必须有 `@Schema`（含 `example`）；`requiredMode = REQUIRED` 与 `@NotNull/@NotBlank` **配套出现**
- 敏感字段（密码、Token）**禁止** `example` 暴露
- **禁止**把聚合根 / Entity 直接作为请求或响应

## `Pageable` 必须三注解齐全

```java
@GetMapping
public Page<UserResponse> queryUsers(@ParameterObject
                                     @Parameter(description = "分页参数（page 从 0 开始）")
                                     @PageableDefault Pageable pageable) { ... }
```

缺 `@ParameterObject` 会让 Swagger UI 把分页参数错误地渲染成 JSON 请求体。

## 生产环境关闭

```yaml
springdoc:
  api-docs.enabled: false      # 禁止暴露 /v3/api-docs（结构泄漏）
  swagger-ui.enabled: false
```
