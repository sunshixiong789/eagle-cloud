# 接口规范（RESTful API）

## 资源命名

- URL 使用名词复数：`/users`、`/orders`、`/membership-cards`
- 使用 kebab-case（小写中划线），禁止驼峰或下划线
- 嵌套资源表示从属关系：`/orders/{orderId}/items`
- 避免在 URL 中使用动词（用 HTTP 方法表达语义）

## HTTP 方法语义

| 方法 | 语义 | 幂等 | 示例 |
|------|------|------|------|
| `GET` | 查询资源 | 是 | `GET /orders/{id}` |
| `POST` | 创建资源 | 否 | `POST /orders` |
| `PUT` | 全量更新 | 是 | `PUT /orders/{id}` |
| `PATCH` | 部分更新 | 否 | `PATCH /orders/{id}/status` |
| `DELETE` | 删除资源 | 是 | `DELETE /orders/{id}` |

## HTTP 状态码

| 场景 | 状态码 |
|------|--------|
| 查询/更新成功 | `200 OK` |
| 创建成功 | `201 Created` |
| 删除/无返回值操作成功 | `204 No Content` |
| 参数校验失败 / 领域异常 | `400 Bad Request` |
| 未认证 | `401 Unauthorized` |
| 无权限 | `403 Forbidden` |
| 资源不存在 | `404 Not Found` |
| 资源冲突（重复创建等）| `409 Conflict` |
| 请求过于频繁 | `429 Too Many Requests` |
| 服务器内部错误 | `500 Internal Server Error` |

## 权限控制

所有 Controller 方法必须显式声明权限（`@PreAuthorize` 或等效机制）：

```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
@PreAuthorize("hasRole('admin')")
public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) { }

@GetMapping("/{id}")
@PreAuthorize("isAuthenticated()")
public OrderResponse getOrderById(@PathVariable Long id) { }

// ❌ 禁止：缺少权限注解
@GetMapping("/{id}")
public OrderResponse getOrderById(@PathVariable Long id) { }
```

## 响应格式

直接返回数据，不使用统一包装类，通过 HTTP 状态码传递语义：

```java
// ✅ 正确
public OrderResponse getOrderById(Long id) { }
public Page<OrderResponse> queryOrders(Pageable pageable) { }

// ❌ 错误
public ApiResult<OrderResponse> getOrderById(Long id) { }
```

## 分页

使用 Spring Data 标准分页，CQRS 列表查询使用投影接口：

```java
@GetMapping
@PreAuthorize("isAuthenticated()")
public Page<OrderSummary> queryOrders(Pageable pageable) {
    return orderApplicationService.queryOrders(pageable);
}
```

## 入参校验

- 请求 DTO 字段使用 Bean Validation（`@NotNull`、`@NotBlank`、`@Size`、`@Min`、`@Max`）
- Controller 方法参数加 `@Valid` 触发校验
- 校验失败返回 `400 Bad Request`，响应体包含字段和错误描述
- 路径变量（`@PathVariable`）和请求参数（`@RequestParam`）同样需要校验

## 错误响应格式

统一由全局异常处理器处理，格式示例：

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

- `errorCode` 仅在使用 `AppException` 子类时携带（类型化异常）
- `message` 支持 i18n，根据 `Accept-Language` 请求头返回对应语言
