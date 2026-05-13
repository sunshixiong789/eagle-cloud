---
name: eagle-resource-server
description: Use when implementing OAuth2 resource server (JWT-protected service) in eagle-cloud projects — @EnableEagleResourceServer, EagleAuthentication, SecurityUtils (getCurrentUser/getCurrentUserId/hasRole/hasAnyRole), @PreAuthorize, EagleUser principal
---

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

### ⚠️ 必填:JWT 解码源(issuer-uri / jwk-set-uri)

只要引入了 starter **并挂上 `@EnableEagleResourceServer`**,就**必须**在 yml 配
`spring.security.oauth2.resourceserver.jwt.issuer-uri` 或 `jwk-set-uri` 之一,
否则 Spring Security 找不到 `JwtDecoder`,启动直接失败。

`issuer-uri` 是**启动时立即**型(Spring 会同步去拉 `/.well-known/openid-configuration`
探测 JWK URL),所以 auth-server 必须可达;`jwk-set-uri` 是懒加载(首次收到 JWT 才拉),
本地联调更友好。**值必须是完整 URL,带 `http://` / `https://` 前缀**,裸 `host:port` 启动会失败。

```yaml
# JWT 解码走 Spring Boot 标准配置 —— issuer-uri 与 jwk-set-uri 二选一,必填
spring.security.oauth2.resourceserver.jwt:
  issuer-uri: ${OAUTH2_ISSUER:http://eagle-system-server:8081}

# starter 自身配置
eagle.resource-server:
  permit-paths: # 额外放行（合并默认白名单）
    - /sms/code
    - /auth/refresh
  auth-server-url: http://localhost:8080  # Swagger OAuth2 流程显示用
  api:
    title: 订单服务 API
    version: v1.0.0
    description: ""
```

主应用类加 `@EnableEagleResourceServer`：

```java

@EnableEagleResourceServer
@SpringBootApplication
public class OrderServerApplication {
}
```

**默认放行**（无需配置）：`/public/**` / `/actuator/health` / `/actuator/info` / `/swagger-ui/**` / `/v3/api-docs/**` 等。

## 关键认知：filter chain 已经强制登录

starter 内 `SecurityFilterChain` 实际配置：

```java
http.authorizeHttpRequests(a -> a
    .requestMatchers(DEFAULT_PERMIT_PATHS + eagle.resource-server.permit-paths).permitAll()
    .anyRequest().authenticated());
```

由此衍生两条强制约定（与 `rules/12-security.md` 同步）：

### ❌ 禁止 `@PreAuthorize("isAuthenticated()")` —— 冗余

filter chain 已经拦掉未认证请求，方法层再判一次毫无意义，且会误导读者以为不写就会
变成公开接口。Controller 默认即「需登录」。

### ❌ 禁止 `@PreAuthorize("permitAll()")` —— 不起作用

它**不会**让接口变公开。只要路径不在 `permit-paths` 白名单里，filter chain 仍 401。
**公开接口必须在 yml 显式放行**：

```yaml
eagle:
  resource-server:
    permit-paths:
      - /products/**
      - /banners
      - /invitations/bind
```

Controller 仅加一行注释标识身份（避免与 yml 双源不一致）：

```java
// 公开接口控制器：放行规则见 application.yml → eagle.resource-server.permit-paths
@RestController
@RequestMapping("/products")
public class ProductController { ... }
```

`@PreAuthorize` 仅用于角色 / SpEL / 数据级判断（如 `hasAnyRole('admin')`、
`hasRole('admin') or #userId == authentication.principal.id`）。

## 核心 API

| 类 / 注解                            | 说明                                                                                                                                                  |
|-----------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| `@EnableEagleResourceServer`      | 启用资源服务器（与 Spring Boot 自动配置等效，二选一）                                                                                                                   |
| `EagleAuthentication`             | 自定义 `Authentication`，`getPrincipal()` 返回 `EagleUser`                                                                                                |
| `EagleJwtAuthenticationConverter` | JWT → `EagleAuthentication` 转换器                                                                                                                     |
| `SecurityUtils`                   | 静态：`getAuthentication() / getCurrentUser() / getCurrentUserId() / getCurrentUsername() / getCurrentDeptId() / hasRole(role) / hasAnyRole(roles...)` |
| `ResourceServerSecurityConfig`    | 默认 SecurityFilterChain（业务可覆盖更高优先级 Bean）                                                                                                             |
| `OpenApiConfig` / `CacheConfig`   | 由 `@EnableEagleResourceServer` 自动 Import                                                                                                            |

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
    @GetMapping("/me")
    public List<OrderResponse> mine() {
        Long userId = SecurityUtils.getCurrentUserId();
        return orderService.findByUserId(userId);
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

| key                                     | 类型     | 默认          | 说明                      |
|-----------------------------------------|--------|-------------|-------------------------|
| `eagle.resource-server.permit-paths`    | List   | `[]`        | 额外放行路径（合并默认白名单）         |
| `eagle.resource-server.auth-server-url` | String | `""`        | Swagger OAuth2 流程绝对 URL |
| `eagle.resource-server.api.title`       | String | `Eagle API` | OpenAPI 标题              |
| `eagle.resource-server.api.version`     | String | `v1.0.0`    | OpenAPI 版本              |
| `eagle.resource-server.api.description` | String | `""`        | OpenAPI 描述（空则用内置默认）     |

JWT 解码走 `spring.security.oauth2.resourceserver.jwt.issuer-uri` 或 `jwk-set-uri`。

## 常见错误

- ❌ 引入 starter + `@EnableEagleResourceServer` 却**没配 issuer-uri / jwk-set-uri** → ✅ 必须二选一,
  否则启动失败(找不到 `JwtDecoder`)
- ❌ `issuer-uri: 172.27.0.155:8081`(裸 host:port) → ✅ 必须带 scheme:`http://172.27.0.155:8081`
- ❌ `@PreAuthorize("isAuthenticated()")` → ✅ 直接删掉，filter chain 已强制登录
- ❌ `@PreAuthorize("permitAll()")` 想公开接口 → ✅ 把路径加到 `permit-paths`，注解删除
- ❌ 公开路径与管理路径共用前缀（如 `/products` 公开 + `/products/admin/**` 管理）→
   ✅ 管理接口统一 `/admin/**` 前缀，避免通配放行误开放
- ❌ 自己 `request.getHeader("Authorization")` 解析 → ✅ `SecurityUtils.getCurrentUser()`
- ❌ Token 放 URL → ✅ 仅 `Authorization: Bearer xxx`
- ❌ Feign 调用 Token 不透传 → ✅ 引入 `http-client-starter`
- ❌ 配置写 `eagle.security.oauth2.resource-server.*` → ✅ 真实是 **`eagle.resource-server.*`**
- ❌ 配置写 `public-paths` → ✅ 真实是 **`permit-paths`**
- ❌ 配置写 `enable-swagger` → ✅ 没有此字段，Swagger 默认放行

## 关联规则

- `.claude/rules/12-security.md`
- `.claude/rules/05-api.md`
- `.claude/rules/17-tenant-permission.md`
