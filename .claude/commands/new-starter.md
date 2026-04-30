---
description: 按 Spring Boot 3 自动配置模板创建新 starter 模块
argument-hint: "<feature-name>，例 webhook（生成 eagle-webhook-starter）"
---

# /new-starter — 创建新 Starter 模块

按 `10-starter.md` 规范创建一个完整的自动配置 starter，自动注册到 settings.gradle、生成 build.gradle、AutoConfiguration、Properties、imports 文件。

## 输入

- `$ARGUMENTS` = `<feature-name>`（kebab-case，例 `webhook` / `feature-flag` / `notification-sms`）
- 缺省时交互询问：feature 名 / 是否需要 `@ConditionalOnClass` 检测的可选依赖列表

## 执行步骤

### 1. 创建目录

```
eagle-starter/eagle-{feature}-starter/
├── build.gradle
└── src/main/
    ├── java/com/eagle/{feature-no-dash}/
    │   ├── config/
    │   │   └── {Feature}AutoConfiguration.java
    │   └── properties/
    │       └── {Feature}Properties.java
    └── resources/META-INF/spring/
        └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

### 2. build.gradle

```groovy
description = '{Feature} Starter — 简短描述'

dependencies {
    api project(':eagle-starter:eagle-common-starter')

    // 核心依赖（按需添加）
    // api 'org.springframework.boot:spring-boot-starter-xxx'

    annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'

    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
}

bootJar.enabled = false
jar.enabled = true
```

（依赖版本由根 BOM 管控，不写版本号）

### 3. {Feature}Properties.java

```java
package com.eagle.{feature_no_dash}.properties;

@Data
@Validated
@ConfigurationProperties(prefix = "eagle.{feature}")
public class {Feature}Properties {

    /** 是否启用 */
    private boolean enabled = true;

    // 业务字段...
}
```

### 4. {Feature}AutoConfiguration.java

```java
package com.eagle.{feature_no_dash}.config;

@AutoConfiguration
@ConditionalOnClass(/* 必备类，如 SomeLib.class */)
@ConditionalOnProperty(prefix = "eagle.{feature}", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({Feature}Properties.class)
@RequiredArgsConstructor
@Slf4j
public class {Feature}AutoConfiguration {

    private final {Feature}Properties properties;

    @Bean
    @ConditionalOnMissingBean
    public {Feature}Service {feature}Service() {
        log.info("Initializing {} service", "{feature}");
        return new {Feature}Service(properties);
    }
}
```

### 5. AutoConfiguration.imports

```
com.eagle.{feature_no_dash}.config.{Feature}AutoConfiguration
```

### 6. 注册 settings.gradle

在 `settings.gradle` 加入：

```groovy
include 'eagle-starter:eagle-{feature}-starter'
```

### 7. 注册到 BOM（如对外发布）

在 `eagle-bom/build.gradle` constraints 中：

```groovy
api project(':eagle-starter:eagle-{feature}-starter')
```

### 8. README 占位（可选）

`eagle-starter/eagle-{feature}-starter/README.md`：使用方法、配置项、扩展点。

### 9. 单元测试骨架

```java
@SpringBootTest(classes = {Feature}AutoConfiguration.class)
class {Feature}AutoConfigurationTest {
    @Autowired private {Feature}Service service;

    @Test void should_load_service_bean() {
        assertNotNull(service);
    }
}
```

### 10. 验证编译

```bash
./gradlew :eagle-starter:eagle-{feature}-starter:compileJava
```

## 后续提示

完成后输出：

1. 已生成的文件清单
2. 配置示例（消费方在 application.yml 中需要写什么）
3. 提示：在 BOM 注册 + 编写文档
4. 强制提醒：是否使用 `ObjectProvider` 处理可选依赖

## 参考规则

- `10-starter.md` — Starter 模板与命名
- `19-config.md` — Properties 类规范
- `30-dependency.md` — 依赖范围
