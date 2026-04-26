# Eagle Resource Server Starter

资源服务器启动器，为 Eagle Cloud 微服务提供 OAuth2 资源服务器功能。

## 功能特性

- 基于 Spring Security OAuth2 Resource Server
- JWT Token 验证和解析
- 自动提取用户信息和权限
- 支持方法级别的权限控制
- CORS 跨域配置
- 无状态会话管理
- 自动配置，开箱即用

## 快速开始

### 1. 添加依赖

在你的微服务模块的 `build.gradle` 中添加：

```gradle
dependencies {
    implementation project(':eagle-starter:eagle-resource-server-starter')
}
```

### 2. 配置

在 `application.yml` 中添加配置：

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          # 授权服务器地址（必须配置）
          issuer-uri: http://localhost:8080
          # 或者直接配置 JWK Set URI
          # jwk-set-uri: http://localhost:8080/oauth2/jwks

eagle:
  security:
    oauth2:
      resource-server:
        enabled: true
        issuer-uri: http://localhost:8080
        public-paths:
          - /public/**
          - /actuator/health
        enable-swagger: true
```

### 3. 使用

#### 3.1 获取当前登录用户

```java
import util.com.eagle.resource.server.SecurityUtils;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public EagleUser getCurrentUser() {
        return SecurityUtils.getCurrentUser();
    }

    @GetMapping("/my-id")
    public Long getCurrentUserId() {
        return SecurityUtils.getCurrentUserId();
    }
}
```

#### 3.2 方法级别权限控制

```java
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    // 需要 ADMIN 角色
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public List<User> getAllUsers() {
        // ...
    }

    // 需要 ADMIN 或 MANAGER 角色
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/reports")
    public List<Report> getReports() {
        // ...
    }

    // 自定义权限表达式
    @PreAuthorize("hasRole('USER') and #id == authentication.principal.id")
    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        // ...
    }
}
```

#### 3.3 编程式权限检查

```java
import util.com.eagle.resource.server.SecurityUtils;

public class SomeService {

    public void doSomething() {
        if (SecurityUtils.hasRole("ADMIN")) {
            // 管理员操作
        }

        if (SecurityUtils.hasAnyRole("ADMIN", "MANAGER")) {
            // 管理员或经理操作
        }

        Long currentUserId = SecurityUtils.getCurrentUserId();
        // 使用当前用户 ID
    }
}
```

## 配置说明

### Eagle 配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `eagle.security.oauth2.resource-server.enabled` | Boolean | true | 是否启用资源服务器 |
| `eagle.security.oauth2.resource-server.issuer-uri` | String | http://localhost:8080 | 授权服务器地址 |
| `eagle.security.oauth2.resource-server.jwk-set-uri` | String | null | JWK Set URI（可选） |
| `eagle.security.oauth2.resource-server.public-paths` | String[] | ["/public/**", "/actuator/health", "/actuator/info"] | 公开路径 |
| `eagle.security.oauth2.resource-server.enable-swagger` | Boolean | true | 是否允许访问 Swagger 文档 |

### Spring Security 配置

| 配置项 | 类型 | 说明 |
|--------|------|------|
| `spring.security.oauth2.resourceserver.jwt.issuer-uri` | String | JWT 签发者 URI（必须配置） |
| `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` | String | JWK Set URI（可选） |

## 架构说明

### JWT Token 结构

资源服务器期望的 JWT Token 包含以下 Claims：

```json
{
  "sub": "username",
  "id": 1,
  "loginName": "admin",
  "userName": "管理员",
  "depId": 1,
  "depName": "技术部",
  "phone": "13800138000",
  "roles": ["ADMIN", "USER"],
  "iss": "http://localhost:8080",
  "exp": 1234567890
}
```

### 组件说明

- **ResourceServerSecurityConfig**: 资源服务器安全配置，配置 JWT 验证和授权规则
- **EagleJwtAuthenticationConverter**: JWT 转换器，将 JWT Token 转换为 Spring Security 的 Authentication 对象
- **SecurityUtils**: 安全工具类，提供获取当前用户信息的便捷方法
- **ResourceServerProperties**: 配置属性类
- **ResourceServerAutoConfiguration**: 自动配置类

### 安全特性

1. **无状态认证**: 使用 JWT Token，不依赖 Session
2. **CSRF 保护**: 已禁用（JWT 无状态认证不需要）
3. **CORS 支持**: 默认配置允许跨域请求
4. **方法安全**: 支持 `@PreAuthorize`、`@Secured`、`@RolesAllowed` 注解
5. **公开端点**: 支持配置不需要认证的公开路径

## 与授权服务器集成

资源服务器需要与 Eagle System Server（授权服务器）配合使用：

1. 授权服务器负责：
   - 用户认证
   - 颁发 JWT Token
   - 提供 JWK Set 端点（公钥）

2. 资源服务器负责：
   - 验证 JWT Token
   - 提取用户信息和权限
   - 保护 API 端点

## 测试

### 获取 Token

```bash
curl -X POST http://localhost:8080/oauth/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "username=admin" \
  -d "password=123456" \
  -d "client_id=eagle-client" \
  -d "client_secret=secret"
```

### 访问受保护的 API

```bash
curl -X GET http://localhost:8081/api/users/me \
  -H "Authorization: Bearer <your-jwt-token>"
```

## 故障排查

### 1. Token 验证失败

**问题**: 返回 401 Unauthorized

**可能原因**:
- `issuer-uri` 配置错误
- 授权服务器不可访问
- Token 已过期
- Token 签名验证失败

**解决方案**:
- 检查 `spring.security.oauth2.resourceserver.jwt.issuer-uri` 配置
- 确保授权服务器正常运行
- 重新获取 Token
- 检查授权服务器和资源服务器的密钥是否一致

### 2. 权限不足

**问题**: 返回 403 Forbidden

**可能原因**:
- 用户没有所需的角色或权限
- `@PreAuthorize` 表达式错误

**解决方案**:
- 检查用户的角色配置
- 验证 JWT Token 中的 `roles` Claim
- 检查权限表达式是否正确

### 3. CORS 错误

**问题**: 浏览器报 CORS 错误

**解决方案**:
- 检查 `ResourceServerSecurityConfig` 中的 CORS 配置
- 确保前端请求包含正确的 `Origin` 头
- 检查是否需要自定义 CORS 配置

## 最佳实践

1. **生产环境配置**:
   - 使用 HTTPS
   - 配置合适的 Token 过期时间
   - 使用环境变量管理敏感配置

2. **权限设计**:
   - 使用细粒度的角色和权限
   - 遵循最小权限原则
   - 定期审查权限配置

3. **性能优化**:
   - 启用 JWT Token 缓存
   - 合理配置 JWK Set 缓存时间
   - 使用连接池

4. **监控和日志**:
   - 记录认证失败事件
   - 监控 Token 验证性能
   - 定期审计访问日志

## 版本兼容性

- Spring Boot: 4.0.3
- Spring Security: 7.x
- Java: 25

## 许可证

Copyright © 2026 Eagle Cloud
