---
name: eagle-openapi
description: Use when configuring OpenAPI/Swagger documentation in eagle-cloud projects — SpringDoc annotations (@Tag/@Operation/@Schema/@ApiResponses), grouping, JWT security scheme
---

# eagle-openapi-starter — Swagger / OpenAPI 3.0（SpringDoc）

## 何时使用

- 对外提供 REST API 的服务
- Swagger UI 调试 / Apifox 同步 / OpenAPI Generator

## 何时不要使用

- 纯内部 RPC（已有 Feign 接口契约）
- WebFlux 网关（不暴露业务 API）

## 依赖与启用

`eagle-openapi-starter` 只提供通用 OpenAPI Bean、JWT Security Scheme 和
`@PreAuthorize` 文档增强，不替业务服务选择 Servlet/WebMVC 或 WebFlux。业务服务必须按自己的 Web
栈显式选择 SpringDoc UI 适配包。

### Servlet / Spring MVC

```gradle
dependencies {
    implementation project(':eagle-starter:eagle-openapi-starter')
    implementation 'org.springframework.boot:spring-boot-starter-webmvc'
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui'
}
```

### Reactive / WebFlux

```gradle
dependencies {
    implementation project(':eagle-starter:eagle-openapi-starter')
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    implementation 'org.springdoc:springdoc-openapi-starter-webflux-ui'
}
```

```yaml
eagle.openapi:
  title: 订单服务 API
  version: v1.0.0
  description: 订单创建、查询、取消
  auth-server-url: http://localhost:8080      # OAuth2 流程显示

# 生产关闭 Swagger UI / api-docs
springdoc:
  swagger-ui:
    enabled: true
  api-docs:
    enabled: true
```

`EagleOpenApiAutoConfiguration` 自动注入 `OpenAPI` Bean、JWT Security Scheme，业务方只需用 SpringDoc 注解。

## 核心注解（来自 SpringDoc）

| 注解                                            | 用途                    |
|-----------------------------------------------|-----------------------|
| `@Tag(name, description)`                     | Controller 类级 — 资源域分组 |
| `@Operation(summary, description, responses)` | 方法级 — 接口说明            |
| `@ApiResponses` / `@ApiResponse`              | 响应码说明                 |
| `@Schema(description, example, requiredMode)` | DTO 字段说明              |
| `@Parameter(description)`                     | 单参数说明                 |

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
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest req) {
        return orderService.create(req);
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

    @Schema(description = "备注", maxLength = 200, example = "请尽快发货")
    @Size(max = 200)
    private String remark;
}

@Schema(description = "订单详情")
public record OrderResponse(
        @Schema(description = "订单 ID", example = "10086") Long id,
        @Schema(description = "订单号", example = "ORD20260430123456") String orderNo,
        @Schema(description = "金额", example = "199.00") BigDecimal totalAmount
) {
}
```

访问：`http://localhost:port/swagger-ui.html`、`http://localhost:port/v3/api-docs`

## 配置项

| key                             | 类型     | 默认                    | 说明                                    |
|---------------------------------|--------|-----------------------|---------------------------------------|
| `eagle.openapi.title`           | String | `Eagle API`           | 文档标题                                  |
| `eagle.openapi.version`         | String | `v1.0.0`              | 版本                                    |
| `eagle.openapi.description`     | String | —                     | 描述                                    |
| `eagle.openapi.auth-server-url` | String | `http://localhost:80` | OAuth2 授权服务器地址（authorizeUrl/tokenUrl） |

⚠️ **starter 仅 4 个字段**，没有 `enabled` / `groups` 等。

## 生产关闭文档暴露

```yaml
# application-prod.yml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

## 接口分组（用 SpringDoc 原生 GroupedOpenApi）

```java

@Configuration
public class OpenApiGroupConfig {

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .pathsToMatch("/api/admin/**")
                .build();
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/api/v1/**")
                .build();
    }
}
```

## 常见错误

- ❌ 生产暴露 `/v3/api-docs` → ✅ `springdoc.api-docs.enabled: false`
- ❌ `@Schema(example = "<密码>")` → ✅ 敏感字段不放 example
- ❌ 直接把 JPA 实体当响应 → ✅ 必须用 Response DTO
- ❌ `@Schema(requiredMode = REQUIRED)` 但 `@NotNull` 缺失 → ✅ 两者必须配套
- ❌ Controller 无 `@Tag` → ✅ 类级必须有
- ❌ 配置写 `eagle.openapi.servers/groups` → ✅ 没有这些字段

## 关联规则

- `.claude/rules/03-api-error.md`
- `.claude/rules/05-security.md` — 生产关闭文档
