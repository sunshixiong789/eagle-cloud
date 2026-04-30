# OpenAPI / Swagger 文档规范

技术栈：`eagle-openapi-starter`（基于 SpringDoc OpenAPI 3.0.2）。所有对外接口**必须**有完整 OpenAPI 注解，`/swagger-ui.html`
自动生成。

## 项目级配置

每个可执行服务在 `infrastructure/config/` 提供 `OpenApiConfig`：

```java

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI eagleOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Eagle System Server API")
                        .version("v1.0")
                        .description("系统管理服务接口"))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt",
                                new SecurityScheme().type(HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
```

## Controller 注解

每个 Controller 必须有：

- 类级 `@Tag` — 描述资源域
- 方法级 `@Operation` — 描述用例
- 必要时 `@ApiResponses` 描述错误码

```java
// ✅ 完整示例
@Tag(name = "订单管理", description = "订单创建、查询、取消")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    @Operation(summary = "创建订单", description = "支持普通订单与团购订单",
            responses = {
                    @ApiResponse(responseCode = "201", description = "创建成功"),
                    @ApiResponse(responseCode = "400", description = "参数校验失败"),
                    @ApiResponse(responseCode = "409", description = "库存不足或重复订单号")
            })
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) { ...}
}
```

## DTO 注解

```java
// ✅ 请求 DTO
@Data
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

// ✅ 响应 DTO
@Schema(description = "订单详情")
public record OrderResponse(
        @Schema(description = "订单 ID", example = "10086") Long id,
        @Schema(description = "订单号", example = "ORD20260430123456") String orderNo,
        @Schema(description = "金额（元）", example = "199.00") BigDecimal totalAmount
) {
}
```

- `requiredMode = REQUIRED` 与 Bean Validation `@NotNull/@NotBlank` **必须配套**
- `example` 必填，便于 Swagger UI 一键填表
- 敏感字段（密码、Token）**禁止** `example` 暴露

## 分页参数

Controller 方法使用 Spring Data `Pageable` 时，必须显式标注为查询参数对象，避免 Swagger UI 将分页参数展示为 JSON 请求体：

```java
@GetMapping
public Page<UserResponse> queryUsers(@ParameterObject
                                     @Parameter(description = "分页参数（page=页码从0开始, size=每页条数, sort=排序字段）")
                                     @PageableDefault Pageable pageable) {
    return userApplicationService.queryUsers(pageable);
}
```

- 必须同时使用 `@ParameterObject`、`@Parameter(description = "...")`、`@PageableDefault`
- `@ParameterObject` 负责让 SpringDoc 将 `page`、`size`、`sort` 展开为 query 参数
- `@PageableDefault` 明确这是 Spring MVC 分页绑定参数，后续可按接口需要设置默认 `size` 或 `sort`

## 接口分组

通过 `GroupedOpenApi` 拆分成多组（管理后台 / OpenAPI / 内部）：

```java

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
```

## 版本管理

URL 路径携带版本：`/api/v1/orders` → `/api/v2/orders`。

- 新增字段（向后兼容）→ 保持 v1，加 `@Schema(description = "...（v1.2 新增）")` 标注
- 破坏性变更 → 新建 `/api/v2/`，v1 保留至少 3 个月并标注 `@Deprecated`
- 同一 Controller 不混用多版本路径

```java
@Operation(deprecated = true, summary = "[已废弃] 改用 GET /api/v2/orders")
@Deprecated(since = "2026-04-01", forRemoval = true)
@GetMapping("/api/v1/orders")
public ...{...}
```

## 错误码文档化

将各域 `ErrorCode` 枚举导出到 OpenAPI 描述中：

```java
@ApiResponses({
        @ApiResponse(responseCode = "404", description = "ORDER_NOT_FOUND (30001)"),
        @ApiResponse(responseCode = "409", description = "ORDER_ALREADY_PAID (30002)"),
        @ApiResponse(responseCode = "400", description = "INSUFFICIENT_STOCK (30005)")
})
```

或在 `@Operation.description` 中以 Markdown 表格列出。

## 安全限制

- 生产环境**默认关闭** Swagger UI（`springdoc.swagger-ui.enabled: false`）
- 仅内网访问的 staging 环境可开启
- **禁止**生产环境暴露 `/v3/api-docs`（结构泄漏）

```yaml
# application-prod.yml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

## 文档导出

CI 流水线导出 OpenAPI JSON 用于：

- Apifox / Postman 同步
- 客户端代码生成（OpenAPI Generator）
- 契约测试（Pact）

```bash
./gradlew :eagle-base-server:eagle-system-server:bootRun &
curl http://localhost:8081/v3/api-docs > docs/openapi/system-server.json
```

## 禁止清单

- 禁止 `@RequestBody` 不加 `@Valid`（校验失效）
- 禁止 DTO 字段无 `@Schema`（生成的文档无说明）
- 禁止生产环境暴露 Swagger UI / api-docs
- 禁止把领域模型（聚合根 / Entity）直接作为请求/响应（必须用 DTO）
- 禁止 `@Schema(example = "<密码明文>")`
- 禁止接口路径出现动词（用 HTTP 方法 + 名词资源）
- 禁止 Controller 中的 `Pageable` 参数缺少 `@ParameterObject`、`@Parameter`、`@PageableDefault`
