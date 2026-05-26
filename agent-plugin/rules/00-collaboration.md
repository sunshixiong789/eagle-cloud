# 协作元规则(Collaboration)

跨技术栈、高优先级,先于所有技术规则。

## 0.1 回答使用中文

对话文本、PR / commit 正文、文档正文 → 简体中文。
代码、命令、文件路径、技术名词(`Spring Modulith`、`@ApplicationModule`、`PR` 等) → 英文原样。

用户用英文提问时跟随英文,不主动切换。

## 0.2 禁止 `@Value` 注入配置

统一 `@ConfigurationProperties(prefix = "eagle.xxx")`,详见 [`02-code-style.md`](02-code-style.md) §"配置注入" + [`19-config.md`](19-config.md)。

```java
// ❌ 禁
@Value("${eagle.payment.gateway-url}") private String url;

// ✅ 用
@Data @Validated @ConfigurationProperties(prefix = "eagle.payment")
public class PaymentProperties {
    @NotBlank private String gatewayUrl;
    private Duration timeout = Duration.ofSeconds(5);
}
```

例外:`@Value("classpath:xxx")` 注入 `Resource`(优先考虑 `ResourceLoader`)。

**PR 前自检**(无输出即合规):

```bash
grep -rEn '@Value\s*\(\s*"\$\{' --include="*.java" eagle-services eagle-starter | grep -v /build/
```
