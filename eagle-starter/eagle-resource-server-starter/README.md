# eagle-resource-server-starter

为业务服务提供 OAuth2 资源服务器能力：校验 JWT、把 claims 转成 `EagleUser`，并接入 `SecurityUtils` / `@PreAuthorize`。

本 starter **不签发 Token**。授权服务器是 `eagle-auth-service`。

更完整的配置与示例见 [USAGE.md](USAGE.md)。

## 功能

- Spring Security OAuth2 Resource Server（Servlet 与 WebFlux 两套 chain）
- JWT → `EagleAuthentication`，`principal` 为 `EagleUser`
- 方法级权限：`@PreAuthorize("hasRole('admin')")` 等
- 公开路径由 yml `eagle.resource-server.permit-paths` 追加（合并默认白名单）
- 无状态会话；JWT 模式下 CSRF 默认关闭

## 快速开始

### 1. 依赖

starter 不替你选 Web 栈：

```gradle
implementation 'com.eagle:eagle-resource-server-starter'

// Servlet
implementation 'org.springframework.boot:spring-boot-starter-webmvc'
implementation 'com.eagle:eagle-openapi-starter'
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui'

// 或 WebFlux（不要和 webmvc 同时引）
// implementation 'org.springframework.boot:spring-boot-starter-webflux'
// implementation 'org.springdoc:springdoc-openapi-starter-webflux-ui'
```

### 2. 配置

`issuer-uri` 与 `jwk-set-uri` 必须配其一，否则没有 `JwtDecoder`，启动失败。推荐 `jwk-set-uri`（懒加载，auth 未就绪也能先起来）。值必须是完整 URL。

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: ${AUTH_SERVER_JWK_SET_URI:http://localhost:9090/oauth2/jwks}

eagle:
  resource-server:
    permit-paths:
      - /sms/code
      - /accounts/register
```

默认已放行：`/public/**`、`/actuator/health`、`/actuator/info`、`/swagger-ui/**`、`/v3/api-docs/**`。

主类可加 `@EnableEagleResourceServer`（与自动配置等效，二选一即可）。

**不要**再写这些已失效的键：

- `eagle.security.oauth2.resource-server.*`（已改为 `eagle.resource-server.permit-paths`）
- `eagle.resource-server.enabled`（starter 引入即生效，没有总开关）

### 3. 取当前用户

```java
import com.eagle.resource.server.util.SecurityUtils;
import com.eagle.resource.server.security.EagleUser;

EagleUser user = SecurityUtils.getCurrentUser();
Long id = SecurityUtils.getCurrentUserId();

@GetMapping("/me")
public UserResponse me(@AuthenticationPrincipal EagleUser principal) { ... }
```

不要自己解 Token，也不要用 `@AuthenticationPrincipal Jwt`。

### 4. 权限

filter chain 默认 `anyRequest().authenticated()`。只有角色 / SpEL / 数据级判断才写 `@PreAuthorize`：

```java
@PreAuthorize("hasRole('admin')")
@PreAuthorize("hasAnyRole('super_admin','operator')")
@PreAuthorize("#accountId == authentication.principal.id")
```

不要新增 `@PreAuthorize("isAuthenticated()")`（多余）或 `@PreAuthorize("permitAll()")`（不会让接口变公开）。

自定义 `SecurityFilterChain` 时必须显式接回 `EagleJwtAuthenticationConverter`，否则 principal 退化成原生 `Jwt`，所有 `hasRole(...)` 静默变成 403。

## 与授权服务器的关系

| 职责 | 服务 |
|---|---|
| 登录、发 Token、JWKS | `eagle-auth-service`（`:9090`，经网关 `:8080`） |
| 验 Token、取 `EagleUser` | 本 starter（system 以及后续业务服务） |

取 Token 走标准端点，例如短信登录：

```http
POST http://localhost:8080/oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=sms_code&phone=13800001234&code=123456&client_id=eagleWeb
```

不要再用 `/oauth/token` + `password` grant，那套已经不存在。

## 版本

跟随仓库 BOM：Spring Boot 4.1.x、Spring Security 7、Java 25。
