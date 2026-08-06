# Spring Boot 4 / Java 25 陷阱

本仓库是 **Spring Boot 4.0.6 + Java 25 + Hibernate 7 + Jackson 3**。主流语料以 Spring Boot 3 / Jackson 2 为主，以下写法**凭直觉写必然出错**，写代码前先对照本表。

## Jackson 3：包名一分为二（最高频错误）

Jackson 3 把 **databind / core 迁到 `tools.jackson.*`**，但**注解仍留在 `com.fasterxml.jackson.annotation.*`**。同一个类里两个包同时出现是正常的。

```java
// ✅ 核心类：tools.jackson
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.ser.std.ToStringSerializer;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;

// ✅ 注解：仍是 com.fasterxml（不要"顺手改成" tools.jackson）
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

// ❌ Jackson 2 写法，本仓库不存在
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
```

### 异常改为 unchecked

`JsonProcessingException`（checked）→ **`JacksonException`（unchecked，继承 `RuntimeException`）**。方法签名不需要也不应该声明 `throws JacksonException`：

```java
// ✅
public byte[] serialize(Object value) {
    try {
        return mapper.writeValueAsBytes(value);
    } catch (JacksonException e) {
        throw new SerializationException("Redis JSON serialize failed", e);
    }
}
```

### 构造方式

```java
// ✅ 需要定制时用 builder
ObjectMapper mapper = JsonMapper.builder()
        .findAndAddModules()          // 取代 Jackson 2 的 findAndRegisterModules()
        .build();

// ✅ 默认配置直接 new 仍然可用
ObjectMapper mapper = new ObjectMapper();   // tools.jackson.databind.ObjectMapper
```

## 自动配置注册

```text
✅ src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
   每行一个全限定类名
```

- 自动配置类用 **`@AutoConfiguration`**，不是 `@Configuration`
- 排序用 `@AutoConfiguration(after = OtherConfig.class)`
- 条件注解：`@ConditionalOnClass`（必加，防缺依赖报错）、`@ConditionalOnMissingBean`（允许使用方覆盖）
- 可选依赖用 `@ConditionalOnClass(name = "全限定名")` 字符串形式避免编译期依赖，注入用 `ObjectProvider<T>`

### 禁止 `eagle.xxx.enabled` 总开关

**starter 引入即生效**——依赖坐标本身就是开关，不要求使用方额外写 yml 才能用。

```java
// ❌ 禁：引入了 starter 却因为没写 yml 而不生效，是最常见的"配了没用"投诉来源
@ConditionalOnProperty(name = "eagle.redis.enabled", havingValue = "true", matchIfMissing = true)

// ✅ 全仓 6 处 @ConditionalOnProperty 都是这三类，没有一处总开关
@ConditionalOnProperty(prefix = "spring.cache", name = "type", havingValue = "redis")    // 选实现
@ConditionalOnProperty(prefix = "eagle.id-generator", name = "type", havingValue = "tsid") // 选提供商
@ConditionalOnProperty(name = "eagle.tracing.zipkin.endpoint")                            // 配了才装
```

判据：条件应表达「**装配哪一种**」或「**配了这个才有意义**」，而不是「**要不要装**」。
使用方要禁用某个 starter，正确做法是移除依赖，或用 `spring.autoconfigure.exclude`。

### util 类必须显式 `@Bean` 注册

starter 里的工具类只标 `@Component` **不会**被业务服务扫到（业务服务的 `@ComponentScan`
根包是自己的 `com.eagle.{svc}`，扫不到 `com.eagle.redis.util`）。必须在 `@AutoConfiguration` 里显式注册：

```java
// ❌ 只有 @Component —— 业务 service 注入时报 NoSuchBeanDefinitionException
@Component
public class CacheProtectionUtil { ... }

// ✅ 在自动配置类里显式 @Bean
@Bean
@ConditionalOnMissingBean
public CacheProtectionUtil cacheProtectionUtil(RedisTemplate<String, Object> redisTemplate) {
    return new CacheProtectionUtil(redisTemplate);
}
```

**注意**：`META-INF/spring.factories` 并未完全废弃 —— `EnableAutoConfiguration` 这一项迁到了 `.imports`，但 `ApplicationListener` / `EnvironmentPostProcessor` / `ApplicationContextInitializer` 仍走 `spring.factories`。原先靠它的 `eagle-elasticsearch-starter` / `eagle-seata-starter` 已移除，但这条机制本身有效：**新 starter 若要注册这三类扩展点，仍必须用 `spring.factories`，不能只写 `.imports`。**

## HTTP 客户端：`RestTemplate` 已退场

本仓库 `RestTemplate` 使用数为 **0**。同步调用统一用 `RestClient` + `@HttpExchange` HTTP Service Interface（`eagle-restclient-starter`），响应式用 `WebClient`（`eagle-webclient-starter`）。

```java
@HttpExchange("/api/v1/inventory")
public interface InventoryClient {
    @GetExchange("/{productId}/stock")
    StockResponse getStock(@PathVariable Long productId);
}
```

客户端接口放调用方 `infrastructure/remote/`，Bean 由 `EagleRestServiceClientFactory` 创建。下游 HTTP 错误由 `EagleResponseErrorHandler` 自动转成项目异常体系（400→`DomainException`、404→`NotFoundException`、409→`ConflictException`、其余→`ServiceException`），**调用方无需 try-catch**。

JWT 由拦截器自动透传，**禁止手动拼装** `Authorization` 头（租户 ID 与 Seata XID 的透传已随对应 starter 移除）。

## Spring Security 7：只有 lambda DSL

链式 `.and()` 写法已移除：

```java
// ✅
http.csrf(AbstractHttpConfigurer::disable)
    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .authorizeHttpRequests(authorize -> authorize
            .requestMatchers(permitPaths).permitAll()
            .anyRequest().authenticated())
    .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

// ❌ 已移除
http.csrf().disable().and().authorizeRequests()...
```

`authorizeRequests()` → `authorizeHttpRequests()`；`antMatchers()` → `requestMatchers()`。

## 其他版本相关

| 项 | 本仓库 |
|---|---|
| Java | **25**（Gradle `JavaLanguageVersion.of(25)`） |
| Spring Boot | 4.0.6 |
| Spring Cloud / Alibaba | 2025.1.1 / 2025.1.0.0 |
| Spring Modulith | 2.0.5 |
| Hibernate | 7.2.6（`jakarta.persistence.*`，不是 `javax.*`） |
| 校验注解 | `jakarta.validation.*` |

- 所有模块的 AOT 任务（`processAot` / `processTestAot`）**已禁用**
- Hibernate 字节码增强已启用（`enableAssociationManagement`）
- Mockito 以 JVM Agent 方式加载（规避 JDK 21+ 动态 Agent 警告）

## 不确定时

版本相关 API 拿不准就**先读仓库里的同类实现**（`grep` 一个已有用法），不要凭 Boot 3 记忆写。必要时用 context7 查官方文档。
