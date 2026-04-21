# 安全规范（Security）

遵循 OWASP Top 10 及 OWASP 安全编码实践规范。

## 接口权限控制

所有 Controller 方法必须显式声明权限（`@PreAuthorize` 或等效注解）：

```java
// 管理员操作
@PostMapping
@PreAuthorize("hasRole('admin')")
public UserResponse createUser(...) { }

// 已登录即可
@GetMapping("/{id}")
@PreAuthorize("isAuthenticated()")
public UserResponse getUserById(...) { }

// 本人或管理员
@PutMapping("/{id}/password")
@PreAuthorize("hasRole('admin') or #id == authentication.principal.id")
public void changePassword(...) { }

// ❌ 禁止：缺少权限注解（即使全局已配置 anyRequest().authenticated()）
@GetMapping("/{id}")
public UserResponse getUserById(...) { }
```

## 身份认证

- 所有敏感接口必须经过身份认证
- Token 必须设置合理过期时间，支持刷新机制
- 禁止在 URL 参数中传递 Token（防日志泄露），统一通过 `Authorization` Header 传递
- 登录失败必须有频率限制（防暴力破解）

## CORS 配置

```java
// ✅ 正确：使用 setAllowedOriginPatterns（支持携带 credentials）
config.setAllowedOriginPatterns(List.of("*"));  // 开发环境
// 生产环境必须改为具体域名：
config.setAllowedOriginPatterns(List.of("https://app.example.com"));
config.setAllowCredentials(true);

// ❌ 禁止：setAllowedOrigins("*") 与 allowCredentials(true) 不兼容
config.setAllowedOrigins(List.of("*"));
```

## 授权与访问控制

- 遵循最小权限原则
- 查询数据时必须校验当前用户是否有权访问该资源（防越权）
- 不信任客户端传入的用户标识，应从服务端 Token/Session 中获取
- 敏感操作（删除、权限变更）需额外校验

## 输入验证与注入防护

- 所有外部输入必须通过 Bean Validation 校验后使用
- 禁止将外部参数拼接进 JPQL/SQL（使用参数化查询）
- 禁止使用反射执行外部传入的类名/方法名

## 敏感数据保护

- 密码使用 BCrypt 或 Argon2 存储，禁止明文或 MD5
- 传输层必须使用 HTTPS/TLS
- 日志、错误响应中禁止暴露堆栈信息、数据库结构等内部细节
- API Key、密钥等敏感配置通过环境变量注入，禁止硬编码

## 接口安全

- 公开接口必须在安全配置中显式声明 `permitAll()`
- 敏感接口考虑幂等控制（防重放攻击）
- 文件上传接口必须校验文件类型和大小
