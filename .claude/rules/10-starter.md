# Starter 自动配置规范

## 模块结构

```
eagle-starter/eagle-{feature}-starter/
├── src/main/java/com/eagle/{feature}/
│   ├── config/                         # @AutoConfiguration 类
│   ├── properties/                     # @ConfigurationProperties 类
│   └── ...                             # 功能实现
├── src/main/resources/META-INF/spring/
│   └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
└── build.gradle
```

## 自动配置类

使用 `@AutoConfiguration`（Spring Boot 3.x+），禁止使用旧的 `@Configuration` + `spring.factories`：

```java
@AutoConfiguration
@ConditionalOnClass(SomeLibrary.class)
@EnableConfigurationProperties(FeatureProperties.class)
public class FeatureAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SomeService someService(FeatureProperties properties) {
        return new SomeService(properties);
    }
}
```

**命名**：`{Feature}AutoConfiguration`

**常用条件注解：**

- `@ConditionalOnClass` — 类路径存在时生效（必加，防止缺少依赖报错）
- `@ConditionalOnProperty(name = "eagle.xxx.enabled", havingValue = "true", matchIfMissing = true)` — 配置开关
- `@ConditionalOnMissingBean` — 允许使用方覆盖默认实现
- `@ConditionalOnClass(name = "fully.qualified.Name")` — 可选依赖用字符串避免编译错误

**排序**：需要在其他配置之后加载时用 `@AutoConfiguration(after = OtherConfig.class)`

## 注册文件

在 `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 中注册，每行一个全限定类名：

```
com.eagle.feature.config.FeatureAutoConfiguration
```

## Properties 类

```java
@Data
@ConfigurationProperties(prefix = "eagle.{feature}")
public class FeatureProperties {
    private boolean enabled = true;
    // 字段使用合理默认值
}
```

**命名**：`{Feature}Properties`，prefix 统一使用 `eagle.{feature}` 前缀

**注册方式**：在 `@AutoConfiguration` 类上加 `@EnableConfigurationProperties(FeatureProperties.class)`，不使用
`@ConfigurationPropertiesScan`

## build.gradle 依赖范围

```gradle
dependencies {
    api project(':eagle-starter:eagle-common-starter')   // 暴露给消费方
    api 'org.springframework.boot:spring-boot-starter-xxx'  // 核心依赖用 api
    compileOnly 'some:optional-lib'                      // 可选依赖用 compileOnly
}
```

- 核心依赖用 `api`（暴露传递依赖给消费模块）
- 可选依赖用 `compileOnly`（配合 `@ConditionalOnClass` 按需加载）
- `bootJar.enabled = false`、`jar.enabled = true`（starter 是库不是应用）

## 可选依赖的松耦合

对可选依赖使用 `ObjectProvider` 注入，避免硬依赖：

```java
@Bean
public SomeInterceptor interceptor(ObjectProvider<Tracer> tracerProvider) {
    return new SomeInterceptor(tracerProvider.getIfAvailable());
}
```
