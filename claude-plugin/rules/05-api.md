# 接口规范（RESTful API）

## 资源命名

- URL 使用名词复数、kebab-case：`/users`、`/membership-cards`
- 嵌套资源表示从属关系：`/orders/{orderId}/items`
- 禁止在 URL 中使用动词

## 权限控制

所有 Controller 方法**必须**显式声明 `@PreAuthorize`，即使全局已配置 `anyRequest().authenticated()`：

```java

@PreAuthorize("hasRole('admin')")       // 管理员操作
@PreAuthorize("isAuthenticated()")      // 登录即可
@PreAuthorize("hasRole('admin') or #id == authentication.principal.id")  // 本人或管理员

// ❌ 禁止：缺少权限注解
@GetMapping("/{id}")
public OrderResponse getOrderById(@PathVariable Long id) {
}
```

## CORS 配置

```java
// ✅ 正确：使用 setAllowedOriginPatterns（支持携带 credentials）
config.setAllowedOriginPatterns(List.of("*"));  // 开发环境
// 生产环境必须改为具体域名

// ❌ 禁止：setAllowedOrigins("*") 与 allowCredentials(true) 不兼容
```

## 响应格式

- 直接返回数据，**不使用** `ApiResult` 包装类，通过 HTTP 状态码传递语义
- 分页使用 Spring Data `Page<T>`，列表查询使用投影接口
- 创建成功返回 `201 Created`（`@ResponseStatus(HttpStatus.CREATED)`）

## 入参校验

- 请求 DTO 使用 Bean Validation（`@NotNull`、`@NotBlank`、`@Size` 等）
- Controller 方法参数加 `@Valid` 触发校验
- `@PathVariable` 和 `@RequestParam` 同样需要校验

## 错误响应

统一由全局异常处理器返回，Controller 层**禁止 try-catch**：

```json
{
  "timestamp": "2026-04-12T09:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "订单不存在",
  "path": "/api/orders/999",
  "errorCode": 30001
}
```

`errorCode` 仅在 `AppException` 子类时携带；`message` 支持 i18n（`Accept-Language`）。
