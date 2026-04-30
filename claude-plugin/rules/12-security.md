# 安全规范（Security）

适用于 OAuth2 授权服务器（`eagle-system-server`）+ 资源服务器（`eagle-resource-server-starter`）+ 网关（
`eagle-gateway-server`）三层安全架构。

## 认证体系

- 终端入口由网关统一鉴权，下游服务通过 `eagle-resource-server-starter` 校验 JWT
- 业务服务**禁止**自行实现 Token 解析，必须复用 starter 提供的 JWT Decoder
- 内部服务调用通过 `eagle-feign-starter` 自动透传 `Authorization` 头，禁止手动拼装

## JWT / Token 处理

```java
// ✅ 从 SecurityContext 取当前用户
@GetMapping("/me")
public UserResponse currentUser(@AuthenticationPrincipal Jwt jwt) {
    Long userId = jwt.getClaim("user_id");
    return userQueryService.findById(userId);
}

// ❌ 禁止：自己解码 Token
String token = request.getHeader("Authorization").substring(7);
Claims claims = Jwts.parser().parseClaimsJws(token).getBody();
```

- Access Token 默认 30 分钟，Refresh Token 默认 7 天，由授权服务器统一签发
- Token 仅签名，不加密；**禁止**在 Token 中携带敏感信息（密码、身份证、手机号明文）
- Token 失效需立即从 Redis 撤销表（`eagle:auth:revoked:{jti}`）写入

## 密码与凭证

- 密码使用 BCrypt（cost ≥ 12），**禁止** MD5 / SHA1 / 明文存储
- 密码字段使用 `@JsonProperty(access = WRITE_ONLY)` 防止序列化输出
- 第三方密钥（DB、OSS、SMS、支付）必须通过 Jasypt 加密存储于 `application.yml`
- **禁止**在代码、日志、Git 中出现明文凭证；提交前用 `git secrets` 或 IDE 插件扫描

```java
// ✅ 密码字段示例
@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
@Column(name = "password_hash", nullable = false, length = 100)
private String passwordHash;
```

## 权限控制

- 所有 Controller 方法必须显式 `@PreAuthorize`（详见 `05-api.md`）
- 数据级越权使用 SpEL：`@PreAuthorize("hasRole('admin') or #userId == authentication.principal.id")`
- 多租户场景额外通过 `eagle-tenant-starter` 强制租户 ID 过滤（详见 `17-tenant-permission.md`）
- 行级数据权限通过 `eagle-row-security-starter` 注解声明，**禁止**在 SQL 中手动拼接

## 敏感数据脱敏

输出脱敏使用 Jackson 序列化器，**禁止**在 Controller 手动 substring：

```java
// ✅ 通过 @Sensitive 注解（项目自定义）输出脱敏
@Sensitive(strategy = SensitiveStrategy.MOBILE)
private String mobile;     // 138****1234

@Sensitive(strategy = SensitiveStrategy.ID_CARD)
private String idCard;     // 110***********1234
```

需要脱敏的字段类型：手机号、邮箱、身份证、银行卡、地址、姓名（保留首字符）、Access Token、密码、API Key。

## 输入安全

| 风险     | 防护手段                                                      |
|--------|-----------------------------------------------------------|
| SQL 注入 | 统一使用 JPA `@Query` 命名参数 / Repository 方法名查询，**禁止**字符串拼接 SQL |
| XSS    | 前端框架默认转义；后端富文本走 OWASP Java HTML Sanitizer 白名单过滤           |
| 文件上传   | 限制 MIME / 后缀白名单 / 大小（详见 `26-file-storage.md`），重命名为 UUID   |
| SSRF   | 外部 URL 调用前校验域名白名单，**禁止**直接拿用户输入做 HTTP 请求                  |
| 反序列化   | **禁止** `ObjectInputStream` 反序列化用户输入；JSON 用 Jackson 默认配置   |

## CORS / CSRF

```java
// ✅ 生产环境必须显式枚举允许的域名
config.setAllowedOriginPatterns(List.of(
                                        "https://*.eagle.com",
    "https://eagle-admin.example.com"
));
        config.

setAllowCredentials(true);

// ❌ 生产禁止：通配符 + credentials 同时开启
config.

setAllowedOrigins(List.of("*"));      // 与 allowCredentials 冲突
```

- 前后端分离 + JWT 模式下，CSRF 默认关闭（无浏览器 Cookie 状态）
- 如使用 Cookie 鉴权，必须开启 CSRF Token + `SameSite=Lax`

## 安全响应头

由网关统一注入，下游服务无需重复：

```
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
Referrer-Policy: strict-origin-when-cross-origin
Content-Security-Policy: default-src 'self'
```

## 审计日志

以下操作**必须**写入审计日志（异步、独立表 `t_audit_log`）：

- 登录 / 登出 / 密码修改 / Token 刷新
- 角色 / 权限分配
- 敏感数据导出（用户列表、订单列表）
- 删除聚合根
- 跨租户管理操作

审计字段：
`operatorId / operatorName / tenantId / action / resourceType / resourceId / ip / userAgent / occurredAt / result`

## 速率限制

- 登录 / 注册 / 短信验证码：使用 `RedisRateLimiter`（5 次/分钟/IP）
- 业务接口：网关层 Sentinel 配置，超限返回 `429 Too Many Requests`
- **禁止**自行实现限流计数器

## 禁止清单

- 禁止 `printStackTrace()`、`System.out.println` 输出异常（暴露堆栈）
- 禁止在 URL 中传递 Token（被日志/Referer/历史记录捕获）
- 禁止在异常 message 中暴露内部信息（数据库表名、堆栈、SQL）
- 禁止使用 `MD5` / `SHA1` 做密码或凭证摘要
- 禁止 `Random` 生成 Token / 验证码，必须用 `SecureRandom`
- 禁止在测试中使用生产环境密钥
