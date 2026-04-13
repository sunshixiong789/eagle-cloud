# Eagle Resource Server Starter - 使用指南

## 概述

Eagle Resource Server Starter 是一个用于保护微服务 API 的 OAuth2 资源服务器启动器。它基于 Spring Security OAuth2 Resource Server，提供 JWT Token 验证、用户信息提取和权限控制功能。

## 快速集成

### 步骤 1: 添加依赖

在你的微服务模块的 `build.gradle` 中添加依赖：

```gradle
dependencies {
    implementation project(':eagle-starter:eagle-resource-server-starter')
}
```

### 步骤 2: 配置 application.yml

```yaml
spring:
  application:
    name: your-service-name
  security:
    oauth2:
      resourceserver:
        jwt:
          # 授权服务器地址（必须配置）
          issuer-uri: http://localhost:8080

eagle:
  security:
    oauth2:
      resource-server:
        enabled: true
        issuer-uri: http://localhost:8080
        public-paths:
          - /public/**
          - /actuator/health
          - /actuator/info
        enable-swagger: true
```

### 步骤 3: 创建受保护的 API

```java
package com.eagle.yourservice.controller;

import com.eagle.common.dto.EagleUser;
import com.eagle.resource.util.SecurityUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/me")
    public EagleUser getCurrentUser() {
        return SecurityUtils.getCurrentUser();
    }

    /**
     * 需要 ADMIN 角色才能访问
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public List<User> getAllUsers() {
        // 业务逻辑
        return userService.findAll();
    }

    /**
     * 只能访问自己的数据
     */
    @PreAuthorize("#id == authentication.principal.claims['id']")
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);
    }
}
```

## 详细使用说明

### 1. 获取当前用户信息

使用 `SecurityUtils` 工具类：

```java
import com.eagle.resource.util.SecurityUtils;

// 获取完整用户对象
EagleUser user = SecurityUtils.getCurrentUser();

// 获取用户 ID
Long userId = SecurityUtils.getCurrentUserId();

// 获取用户名
String username = SecurityUtils.getCurrentUsername();

// 获取部门 ID
Long deptId = SecurityUtils.getCurrentDeptId();
```

### 2. 权限控制

#### 2.1 使用注解进行权限控制

```java
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    // 需要 ADMIN 角色
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dashboard")
    public Dashboard getDashboard() {
        // ...
    }

    // 需要 ADMIN 或 MANAGER 角色
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/reports")
    public List<Report> getReports() {
        // ...
    }

    // 复杂的权限表达式
    @PreAuthorize("hasRole('ADMIN') or (#userId == authentication.principal.claims['id'])")
    @PutMapping("/users/{userId}")
    public User updateUser(@PathVariable Long userId, @RequestBody User user) {
        // ...
    }
}
```

#### 2.2 编程式权限检查

```java
import com.eagle.resource.util.SecurityUtils;

@Service
public class UserService {

    public void deleteUser(Long userId) {
        // 检查是否有 ADMIN 角色
        if (!SecurityUtils.hasRole("ADMIN")) {
            throw new AccessDeniedException("需要管理员权限");
        }

        // 或者检查多个角色
        if (!SecurityUtils.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
            throw new AccessDeniedException("权限不足");
        }

        // 业务逻辑
        userRepository.deleteById(userId);
    }
}
```

### 3. 配置公开端点

有些端点不需要认证，可以在配置中指定：

```yaml
eagle:
  security:
    oauth2:
      resource-server:
        public-paths:
          - /public/**
          - /api/auth/login
          - /api/auth/register
          - /actuator/health
          - /actuator/info
```

### 4. 自定义安全配置

如果需要更复杂的安全配置，可以创建自己的配置类：

```java
package com.eagle.yourservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class CustomSecurityConfig {

    /**
     * 自定义安全配置
     * 使用 @Order(0) 确保在默认配置之前执行
     */
    @Bean
    @Order(0)
    public SecurityFilterChain customSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/custom/**")
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/api/custom/public/**").permitAll()
                .requestMatchers("/api/custom/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
```

## 测试

### 1. 获取 Access Token

首先从授权服务器获取 Token：

```bash
curl -X POST http://localhost:8080/oauth/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "username=admin" \
  -d "password=123456" \
  -d "client_id=eagle-client" \
  -d "client_secret=secret"
```

响应示例：

```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "refresh_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### 2. 使用 Token 访问受保护的 API

```bash
# 获取当前用户信息
curl -X GET http://localhost:8081/api/users/me \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."

# 访问需要 ADMIN 角色的端点
curl -X GET http://localhost:8081/api/admin/dashboard \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### 3. 单元测试

```java
package com.eagle.yourservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testPublicEndpoint() throws Exception {
        mockMvc.perform(get("/public/info"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void testAuthenticatedEndpoint() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void testAdminEndpointWithoutRole() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isForbidden());
    }
}
```

## 常见问题

### Q1: 如何处理 Token 过期？

A: Token 过期后，客户端应该使用 Refresh Token 获取新的 Access Token：

```bash
curl -X POST http://localhost:8080/oauth/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=refresh_token" \
  -d "refresh_token=<your-refresh-token>" \
  -d "client_id=eagle-client" \
  -d "client_secret=secret"
```

### Q2: 如何在 Feign 客户端中传递 Token？

A: 创建一个 Feign 拦截器：

```java
package com.eagle.yourservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class FeignTokenInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            String token = jwtAuth.getToken().getTokenValue();
            template.header("Authorization", "Bearer " + token);
        }
    }
}
```

### Q3: 如何自定义 JWT 验证逻辑？

A: 可以创建自定义的 `JwtDecoder`：

```java
package com.eagle.yourservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class JwtConfig {

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri("http://localhost:8080/oauth2/jwks")
                .build();

        // 自定义验证逻辑
        decoder.setJwtValidator(token -> {
            // 添加自定义验证
            return OAuth2TokenValidatorResult.success();
        });

        return decoder;
    }
}
```

### Q4: 如何处理跨域问题？

A: 资源服务器已经配置了默认的 CORS 策略。如果需要自定义，可以覆盖 `corsConfigurationSource` Bean：

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of("https://your-frontend.com"));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

## 最佳实践

1. **使用 HTTPS**: 生产环境必须使用 HTTPS 传输 Token
2. **Token 过期时间**: 设置合理的 Token 过期时间（建议 15-60 分钟）
3. **最小权限原则**: 只授予用户必要的权限
4. **日志记录**: 记录所有认证和授权失败的事件
5. **监控**: 监控 Token 验证性能和失败率
6. **缓存**: 合理配置 JWK Set 缓存，减少对授权服务器的请求

## 相关文档

- [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)
- [JWT 规范](https://datatracker.ietf.org/doc/html/rfc7519)
- [OAuth 2.0 规范](https://datatracker.ietf.org/doc/html/rfc6749)
