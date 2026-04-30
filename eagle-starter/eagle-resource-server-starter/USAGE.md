# eagle-resource-server-starter — OAuth2 资源服务器（JWT 鉴权）

## 何时使用

- 业务服务接收 JWT Token 进行鉴权（不是签发 Token）
- 默认所有接口需登录，配合 `@PreAuthorize` 做角色 / 权限控制
- 与 `eagle-system-server`（Authorization Server）配合

## 何时不要使用

- Authorization Server（用 OAuth2 Authorization Server）
- 服务间无需用户上下文的纯内部调用（仍建议用 mTLS / API Key）

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-resource-server-starter')
```

```yaml
spring.security.oauth2.resourceserver.jwt:
  issuer-uri: ${OAUTH2_ISSUER:http://eagle-system-server:8081}

eagle.security.oauth2.resource-server:
  enabled: true
  issuer-uri: ${OAUTH2_ISSUER:http://eagle-system-server:8081}
  public-paths:
    - /actuator/health
    - /actuator/info
    - /v3/api-docs/**
    - /swagger-ui/**
  enable-swagger: true
```

主应用类加 `@EnableEagleResourceServer`：

```java
@EnableEagleResourceServer
@SpringBootApplication
public class MyServerApplication { }
```

## 核心 API

| 类 / 注解 | 用途 |
|---|---|
| `@EnableEagleResourceServer` | 启用资源服务器（默认所有接口需鉴权）|
| `EagleAuthentication` | 自定义 `Authentication`（含 `userId / tenantId / roles / permissions`）|
| `EagleJwtAuthenticationConverter` | JWT → `EagleAuthentication` 转换器 |
| `SecurityUtils` | `getCurrentUser() / getCurrentUserId() / getCurrentUsername() / getCurrentDeptId() / hasRole() / hasAnyRole()` |
| `ResourceServerSecurityConfig` | 默认 SecurityFilterChain（业务可覆盖） |
| `EagleUser`（来自 common-starter） | 用户上下文 DTO |

## 最小示例

```java
@EnableEagleResourceServer
@SpringBootApplication
public class OrderServerApplication { }

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public List<OrderResponse> mine() {
        Long userId = SecurityUtils.getCurrentUserId();
        return orderService.findByUserId(userId);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('order:create')")
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest req) {
        return orderService.create(req);
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<OrderResponse> all(@SpringQueryMap Pageable p) {
        return orderService.findAll(p);
    }
}

// 编程式权限检查
if (!SecurityUtils.hasRole("ADMIN")) {
    throw CommonErrorCode.ACCESS_DENIED.toDomainException();
}
```

## 配置项

| key | 类型 | 默认 | 说明 |
|---|---|---|---|
| `eagle.security.oauth2.resource-server.enabled` | boolean | `true` | 总开关 |
| `eagle.security.oauth2.resource-server.issuer-uri` | String | — | 授权服务器地址 |
| `eagle.security.oauth2.resource-server.public-paths` | List | actuator/health 等 | 公开路径白名单 |
| `eagle.security.oauth2.resource-server.enable-swagger` | boolean | `true` | 自动放行 swagger 路径 |

## 自定义 SecurityFilterChain

需要更复杂规则时，业务方可定义优先级更高的 Bean：

```java
@Configuration
public class CustomSecurityConfig {
    @Bean
    @Order(0)   // 早于 starter 默认链
    public SecurityFilterChain customChain(HttpSecurity http) throws Exception {
        return http.securityMatcher("/api/custom/**")
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/custom/public/**").permitAll()
                .requestMatchers("/api/custom/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .build();
    }
}
```

## 测试

```java
@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {
    @Autowired private MockMvc mockMvc;

    @Test @WithMockUser(roles = {"ADMIN"})
    void admin_can_access() throws Exception {
        mockMvc.perform(get("/api/v1/orders/admin/all"))
               .andExpect(status().isOk());
    }
}
```

## 常见错误

- ❌ Controller 没加 `@PreAuthorize` → ✅ 必须显式声明（详见 `05-api.md`）
- ❌ 自己解析 Token 拿 userId → ✅ 用 `SecurityUtils.getCurrentUserId()`
- ❌ Token 放到 URL 中 → ✅ 仅 `Authorization: Bearer xxx`
- ❌ 公开接口在代码中判断放行 → ✅ 配 `public-paths`
- ❌ Feign 调用 Token 不透传 → ✅ 用 `http-client-starter` 的 `FeignAuthInterceptor`

## 关联规则

- `.claude/rules/12-security.md` — JWT、密码、敏感字段、CORS
- `.claude/rules/05-api.md` — `@PreAuthorize` 强制要求
- `.claude/rules/17-tenant-permission.md` — 租户 ID 透传
- `.claude/rules/11-feign.md` — Feign Token 自动透传
