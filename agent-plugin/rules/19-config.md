# 配置管理规范（Configuration）

技术栈：Spring Boot Properties + Nacos 配置中心 + Jasypt 加密。

## 配置类（强类型绑定）

**禁止 `@Value`**，统一 `@ConfigurationProperties`（详见 `02-code-style.md`）：

```java
// ✅ 标准 Properties 类
@Data
@ConfigurationProperties(prefix = "eagle.payment")
public class PaymentProperties {
    /** 网关基础 URL */
    private String gatewayUrl;
    /** 默认超时（毫秒） */
    private int timeoutMs = 5000;
    /** 商户密钥（生产必须 ENC()）*/
    private String merchantKey;
    /** 重试策略 */
    private final Retry retry = new Retry();

    @Data
    public static class Retry {
        private int maxAttempts = 3;
        private Duration backoff = Duration.ofMillis(500);
    }
}
```

- 前缀统一 `eagle.{feature}`，全小写、kebab 不用驼峰
- 字段必须有合理默认值（除非完全不可省略）
- 复杂类型用嵌套静态类，**不**用 `Map<String, Object>`
- 单位类型用 `Duration / DataSize`（自动解析 `5s` / `100ms` / `4MB`）
- 在 `@AutoConfiguration` 上 `@EnableConfigurationProperties(XxxProperties.class)`

## profile 分层

```
src/main/resources/
├── application.yml              # 全局通用（无环境差异）
├── application-dev.yml          # 开发环境
├── application-test.yml         # 测试环境
├── application-staging.yml      # 预发
└── application-prod.yml         # 生产
```

- 启动通过 `--spring.profiles.active=prod` 或 `SPRING_PROFILES_ACTIVE=prod` 选择
- **禁止**在 `application.yml` 中硬编码 `spring.profiles.active`
- **禁止**测试代码读取生产配置

## Nacos 配置中心

```yaml
# bootstrap.yml — 仅放接入 Nacos 必需字段
spring:
  application:
    name: eagle-system-server
  cloud:
    nacos:
      config:
        server-addr: ${NACOS_SERVER:127.0.0.1:8848}
        namespace: ${NACOS_NAMESPACE:dev}
        group: EAGLE
        file-extension: yml
        shared-configs:
          - data-id: eagle-common.yml
            refresh: true
```

**dataId 命名**：`{spring.application.name}-{profile}.yml`，例：`eagle-system-server-prod.yml`。

**namespace 规划**：

| Namespace | 用途           |
|-----------|--------------|
| `dev`     | 开发           |
| `test`    | 测试           |
| `staging` | 预发           |
| `prod`    | 生产（严格权限）     |
| `public`  | 跨环境共享配置（极少用） |

## 敏感字段加密（Jasypt）

```yaml
# application-prod.yml
spring:
  datasource:
    password: ENC(WtbnVsbAAAAAdt...)   # 加密值
  redis:
    password: ENC(GHa97KLM...)

eagle:
  payment:
    merchant-key: ENC(...)
```

- 密码、密钥、Token、第三方凭证**必须 ENC()** 加密
- Jasypt 主密钥通过环境变量 `JASYPT_ENCRYPTOR_PASSWORD` 注入
- **禁止**主密钥写入 Git / 配置文件 / 镜像
- 加解密命令：`./gradlew jasyptEncrypt -Pinput=xxx -Ppassword=$JASYPT_ENCRYPTOR_PASSWORD`

## 配置热刷新

```java
// ✅ 显式声明可刷新
@RefreshScope
@Component
@RequiredArgsConstructor
public class FeatureFlagsHolder {
    private final FeatureFlagProperties properties;
}
```

- **禁止**给 DataSource、JPA EntityManager 等基础设施加 `@RefreshScope`
- 刷新场景仅限：业务开关、限流阈值、白名单、第三方 URL
- 配置变更需触发**测试**（自动化或手工），不可裸推生产

## 配置项分级

| 级别      | 分类                    | 示例                |
|---------|-----------------------|-------------------|
| L1 启动配置 | 启动时不可变                | DB 连接、Redis 地址、端口 |
| L2 业务配置 | 重启生效                  | 业务开关默认值、缓存 TTL    |
| L3 动态配置 | 实时生效（`@RefreshScope`） | 限流阈值、灰度比例         |

L1 走 `bootstrap.yml`，L2 走 `application.yml`，L3 走 Nacos 动态推送。

## 环境变量优先级

Spring Boot 配置加载顺序（后者覆盖前者）：

```
application.yml
  → application-{profile}.yml
  → Nacos 共享配置
  → Nacos 应用配置
  → 命令行参数 (--key=value)
  → 环境变量 (KEY=value)
```

- **生产**通过环境变量注入敏感字段（容器化天然支持）
- **本地开发**通过 IDE 启动配置注入，**不要**写入 `application-dev.yml`
- 环境变量命名：`SPRING_DATASOURCE_PASSWORD` / `EAGLE_PAYMENT_MERCHANT_KEY`

## 验证

`@ConfigurationProperties` + Bean Validation 启动期校验：

```java

@Data
@Validated
@ConfigurationProperties(prefix = "eagle.payment")
public class PaymentProperties {
    @NotBlank
    private String gatewayUrl;
    @Min(1000)
    @Max(60000)
    private int timeoutMs = 5000;
    @NotNull
    private Duration backoff = Duration.ofMillis(500);
}
```

启动期校验失败直接 fail-fast，**禁止**运行时才发现配置错误。

## 禁止清单

- 禁止 `@Value("${...}")` 注入到字段（无类型安全、无 IDE 跳转）
- 禁止配置文件出现明文密码、密钥、Token
- 禁止测试中硬编码生产 URL / IP
- 禁止 `application.yml` 包含某个 profile 独有的字段（污染所有环境）
- 禁止运行时 `System.setProperty(...)` 修改 Spring Boot 配置（启动后已加载完成）
- 禁止跨环境复用 Nacos namespace
