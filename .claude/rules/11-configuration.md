# 配置注入规范

**禁止使用 `@Value` 注入配置属性。** 统一使用 `@ConfigurationProperties` 类型安全绑定。

## 做法

1. 创建 `@ConfigurationProperties` 类，放在 `infrastructure/config/` 下
2. 使用 `@RequiredArgsConstructor` 构造器注入 Properties 类
3. 主类启用 `@ConfigurationPropertiesScan`，无需额外注册

```java
// ✅ 正确
@ConfigurationProperties(prefix = "app.payment")
public class PaymentProperties {
    private String apiKey = "";
    private String endpoint = "";
}

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl {
    private final PaymentProperties paymentProperties;
}

// ❌ 错误
@Service
public class PaymentServiceImpl {
    @Value("${app.payment.api-key:}")
    private String apiKey;
}
```

## 原因

- 类型安全，编译期检测
- IDE 自动补全和跳转
- 支持 JSR-303 校验（`@NotBlank` 等）
- 配置项集中管理，职责清晰
- 测试时直接 new Properties 对象，无需 ReflectionTestUtils
