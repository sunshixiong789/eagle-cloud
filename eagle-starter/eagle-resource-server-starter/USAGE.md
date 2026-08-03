# eagle-resource-server-starter — OAuth2 资源服务器（JWT 鉴权 + EagleUser 注入）

## 何时使用

- 业务服务接收 JWT 鉴权（不签发 Token）
- 需要 `EagleUser` 作为 Spring Security `Authentication.principal`
- 与 `eagle-system-server`（Authorization Server）配合

## 何时不要使用

- Authorization Server（用 OAuth2 Authorization Server）
- 无用户上下文的纯内部 RPC（用 mTLS / API Key）

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-resource-server-starter')
```

Choose the web stack in the application module:

```gradle
// Servlet / Spring MVC application
implementation 'org.springframework.boot:spring-boot-starter-webmvc'

// Reactive / WebFlux application
implementation 'org.springframework.boot:spring-boot-starter-webflux'
```

OpenAPI UI is also web-stack specific. Use `eagle-openapi-starter` for shared
OpenAPI customizations, and add the matching SpringDoc UI artifact for the
selected web stack.

> ⚠️ **必填**:挂上 `@EnableEagleResourceServer` 后,`spring.security.oauth2.resourceserver.jwt.issuer-uri`
> 或 `jwk-set-uri` 必须配其一,否则 Spring 找不到 `JwtDecoder`,启动失败。
> `issuer-uri` 启动时立即拉 `/.well-known/openid-configuration`(auth-server 必须可达);
> `jwk-set-uri` 懒加载,本地联调更友好。**值必须是完整 URL(带 `http://` / `https://`)**,
> 裸 `host:port` 启动会失败。

```yaml
# JWT 解码走 Spring Boot 标准配置 —— issuer-uri 与 jwk-set-uri 二选一,必填
spring.security.oauth2.resourceserver.jwt:
  issuer-uri: ${OAUTH2_ISSUER:http://eagle-system-server:8081}

# starter 自身配置
eagle.resource-server:
  permit-paths:                   # 额外放行（合并默认白名单）
    - /sms/code
    - /auth/refresh

# OpenAPI / Swagger 配置已迁移到 eagle-openapi-starter（同时支持 Servlet 与 WebFlux）
eagle.openapi:
  title: 订单服务 API
  version: v1.0.0
  description: ""
  auth-server-url: http://localhost:8080   # Swagger OAuth2 流程显示用，可空
```

> ⚠️ **Servlet vs WebFlux 路径匹配差异**：
> - Servlet 链路使用 Spring MVC `Ant` 语法（`*` / `**` / `?`）
> - WebFlux 链路使用 Spring `PathPattern`（`*` / `**` / `{var}` / `{var:regex}`）
> - 简单通配（`/x/**`、`/x/*`）在两种语法下行为一致
> - 高级用法（`?` 单字符匹配、自定义正则）只在 PathPattern 可用，Ant 不支持
> - `eagle.resource-server.permit-paths` 同一份配置文件在 Servlet/WebFlux 服务都用时，应避免使用 `?` 单字符匹配等仅 PathPattern 支持的高级语法

主应用类加 `@EnableEagleResourceServer`：

```java

@EnableEagleResourceServer
@SpringBootApplication
public class OrderServerApplication {
}
```

**默认放行**（无需配置）：`/public/**` / `/actuator/health` / `/actuator/info` / `/swagger-ui/**` / `/v3/api-docs/**` 等。

## 核心 API

| 类 / 注解                            | 说明                                                                                                                                                  |
|-----------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| `@EnableEagleResourceServer`      | 启用资源服务器（与 Spring Boot 自动配置等效，二选一）                                                                                                                   |
| `EagleAuthentication`             | 自定义 `Authentication`，`getPrincipal()` 返回 `EagleUser`                                                                                                |
| `EagleJwtAuthenticationConverter` | JWT → `EagleAuthentication` 转换器                                                                                                                     |
| `SecurityUtils`                   | 静态：`getAuthentication() / getCurrentUser() / getCurrentUserId() / getCurrentUsername() / getCurrentDeptId() / hasRole(role) / hasAnyRole(roles...)` |
| `ResourceServerSecurityConfig`    | Servlet 默认 SecurityFilterChain（业务可覆盖更高优先级 Bean）                                                                                                    |
| `ReactiveResourceServerSecurityConfig` | WebFlux 默认 SecurityWebFilterChain                                                                                                              |
| `CacheConfig`                     | 缓存 + Redis 序列化配置（由 starter 自动 Import）                                                                                                              |

## 最小示例

```java

@EnableEagleResourceServer
@SpringBootApplication
public class OrderServerApplication {
}

@Tag(name = "订单管理")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "我的订单")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public List<OrderResponse> mine() {
        EagleUser user = SecurityUtils.getCurrentUser();
        return orderService.findByUserId(user.getId());
    }

    @GetMapping("/profile")
    public UserProfileResponse profile(@AuthenticationPrincipal EagleUser principal) {
        return userProfileService.findByUserId(principal.getId());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/all")
    public Page<OrderResponse> adminAll(@SpringQueryMap Pageable pageable) {
        return orderService.findAll(pageable);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest req) {
        return orderService.create(req);
    }
}

// 编程式
if(!SecurityUtils.

hasRole("ADMIN")){
        throw CommonErrorCode.ACCESS_DENIED.

toDomainException();
}

EagleUser user = SecurityUtils.getCurrentUser();
String name = user.getName();
Long deptId = user.getDeptId();
```

> 当前用户信息统一从 `SecurityUtils.getCurrentUser()` 获取；只需要用户 ID 时可用
> `SecurityUtils.getCurrentUserId()`。Controller 参数注入时使用
> `@AuthenticationPrincipal EagleUser principal`。不要用 `@AuthenticationPrincipal Jwt jwt`
> 读取 `subject` / claims 作为业务用户信息，因为 starter 的 principal 是 `EagleUser`。

## 自定义 SecurityFilterChain

需要更复杂规则时定义优先级更高的 Bean：

```java

@Configuration
public class CustomSecurityConfig {
    @Bean
    @Order(0)
    public SecurityFilterChain customChain(HttpSecurity http) throws Exception {
        return http.securityMatcher("/api/custom/**")
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/custom/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .build();
    }
}
```

## 配置项

| key                                  | 类型   | 默认   | 说明                                  |
|--------------------------------------|------|------|-------------------------------------|
| `eagle.resource-server.permit-paths` | List | `[]` | 额外放行路径（合并默认白名单，Servlet=Ant / WebFlux=PathPattern） |

OpenAPI 相关配置移至 `eagle.openapi.*`（详见 `eagle-openapi-starter`）。

JWT 解码走 `spring.security.oauth2.resourceserver.jwt.issuer-uri` 或 `jwk-set-uri`。

## 常见错误

- ❌ 引入 starter + `@EnableEagleResourceServer` 却**没配 issuer-uri / jwk-set-uri** → ✅ 必须二选一,
  否则启动失败(找不到 `JwtDecoder`)
- ❌ `issuer-uri: 172.27.0.155:8081`(裸 host:port) → ✅ 必须带 scheme:`http://172.27.0.155:8081`
- ❌ Controller 漏 `@PreAuthorize` → ✅ 必须显式声明
- ❌ 自己 `request.getHeader("Authorization")` 解析 → ✅ `SecurityUtils.getCurrentUser()`
- ❌ `@AuthenticationPrincipal Jwt jwt` 取当前用户 → ✅ `SecurityUtils.getCurrentUser()` 或
  `@AuthenticationPrincipal EagleUser principal`
- ❌ Token 放 URL → ✅ 仅 `Authorization: Bearer xxx`
- ❌ Feign 调用 Token 不透传 → ✅ 引入 `eagle-restclient-starter`
- ❌ 配置写 `eagle.security.oauth2.resource-server.*` → ✅ 真实是 **`eagle.resource-server.*`**
- ❌ 配置写 `public-paths` → ✅ 真实是 **`permit-paths`**
- ❌ 配置写 `enable-swagger` → ✅ 没有此字段，Swagger 默认放行

## 关联规则

- `.claude/rules/05-security.md`
- `.claude/rules/03-api-error.md`
