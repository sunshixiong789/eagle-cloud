# eagle-openapi-starter — Swagger / OpenAPI 3.0 文档（SpringDoc）

## 何时使用

- 所有对外提供 REST API 的服务
- 需要 Swagger UI 调试 / Apifox 同步
- OpenAPI Generator 客户端代码生成

## 何时不要使用

- 纯内部 RPC（已用 Feign 接口契约）
- WebFlux 网关（Gateway 不暴露业务 API）

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-openapi-starter')
```

```yaml
eagle.openapi:
  enabled: true
  title: ${spring.application.name} API
  version: v1.0
  description: 业务接口文档
  contact:
    name: Eagle Team
    email: dev@eagle.com
  servers:
    - url: http://localhost:${server.port}
      description: Local
  groups:
    - name: admin
      paths-to-match: /api/admin/**
    - name: public
      paths-to-match: /api/v1/**

springdoc:
  swagger-ui:
    enabled: true                      # 生产必须 false
  api-docs:
    enabled: true                      # 生产必须 false
```

## 核心 API

由 SpringDoc 提供，starter 已预配置：

| 注解 | 用途 |
|---|---|
| `@Tag` | Controller 类级 — 资源域分组 |
| `@Operation` | 方法级 — 接口说明 |
| `@ApiResponses` / `@ApiResponse` | 响应码说明 |
| `@Schema` | DTO 字段说明（含 `requiredMode` / `example` / `description`） |
| `@Parameter` | 参数说明 |

`EagleOpenApiAutoConfiguration` 自动注入 `OpenAPI` Bean、JWT Security Scheme、分组 Bean。

## 最小示例

```java
@Tag(name = "订单管理", description = "订单 CRUD")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    @Operation(summary = "创建订单",
        responses = {
            @ApiResponse(responseCode = "201", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "参数错误"),
            @ApiResponse(responseCode = "409", description = "重复订单号")
        })
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.create(request);
    }
}

@Schema(description = "创建订单请求")
public class CreateOrderRequest {

    @Schema(description = "商品 ID 列表", requiredMode = REQUIRED, example = "[101, 102]")
    @NotEmpty
    private List<Long> productIds;

    @Schema(description = "收货地址 ID", requiredMode = REQUIRED, example = "9527")
    @NotNull
    private Long addressId;
}
```

访问 `http://localhost:port/swagger-ui.html` 查看。

## 配置项

| key | 类型 | 默认 | 说明 |
|---|---|---|---|
| `eagle.openapi.enabled` | boolean | `true` | 总开关 |
| `eagle.openapi.title` | String | 应用名 | 文档标题 |
| `eagle.openapi.version` | String | `v1.0` | 版本 |
| `eagle.openapi.groups` | List | — | API 分组 |
| `springdoc.swagger-ui.enabled` | boolean | `true` | UI 开关（生产必须 false）|
| `springdoc.api-docs.enabled` | boolean | `true` | api-docs 开关（生产必须 false） |

## 生产环境关闭

```yaml
# application-prod.yml
springdoc:
  api-docs.enabled: false
  swagger-ui.enabled: false
```

## 常见错误

- ❌ 生产暴露 `/v3/api-docs` → ✅ `enabled: false`
- ❌ `@Schema(example = "<密码>")` → ✅ 敏感字段不写 example
- ❌ 直接把 JPA 实体当响应 DTO → ✅ 必须用 Response DTO
- ❌ `@Schema(requiredMode = REQUIRED)` 但代码 `@NotNull` 缺失 → ✅ 两者必须配套
- ❌ Controller 无 `@Tag` → ✅ 类级必须有

## 关联规则

- `.claude/rules/18-openapi.md` — 完整 OpenAPI 规范
- `.claude/rules/05-api.md` — RESTful 约定
- `.claude/rules/12-security.md` — 生产关闭文档暴露
