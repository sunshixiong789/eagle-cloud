# 模块边界治理规范（Spring Modulith）

适用于使用 Spring Modulith 的模块化单体架构。微服务架构通过独立部署单元天然实现边界隔离。

## 作用

Spring Modulith 在**测试阶段**静态扫描所有类文件，当检测到以下情况时测试失败：

- 模块间循环依赖
- 访问其他模块的内部包（未通过命名接口暴露）
- 违反 `allowedDependencies` 声明的依赖规则

## 运行架构验证

```bash
# PR 前必须通过
gradle test --tests "*.ModulithArchitectureTest"
```

## 模块声明规范

### 模块根包：使用 `@ApplicationModule`

```java
@ApplicationModule(
    displayName = "订单模块",
    allowedDependencies = {
        "inventory::application-port",  // 只允许访问库存的应用层端口
        "common"
    }
)
package com.example.order;
```

### 对外暴露的子包：使用 `@NamedInterface`

```java
// 暴露应用层端口供其他域调用
@NamedInterface("application-port")
package com.example.order.application.port;
```

### 共享内核：使用 `Type.OPEN`

```java
@ApplicationModule(type = ApplicationModule.Type.OPEN)
package com.example.common;
```

### `@Modulithic` 声明共享模块

```java
@Modulithic(
    systemName = "MyApp",
    sharedModules = "common"  // 共享内核，所有模块隐式允许访问
)
@SpringBootApplication
public class MyApplication { }
```

## 新增模块的步骤

1. 在新包根目录创建 `package-info.java`，声明 `@ApplicationModule`
2. 声明允许的依赖（`allowedDependencies`）
3. 为需要对外暴露的子包创建 `package-info.java`，加 `@NamedInterface("name")`
4. 运行架构验证测试确认通过

## 当测试失败时

**错误示例：**

```
Violations:
- Module 'order' depends on non-exposed type com.example.inventory.infrastructure.WarehouseClient
  Method <...> calls method <...>
```

**排查步骤：**

1. 找到违规的 import 或方法调用
2. 判断是否是合理的跨模块依赖：
    - 如果合理 → 在被依赖包加 `@NamedInterface`，在依赖方 `allowedDependencies` 中声明
    - 如果是错误依赖 → 重构，通过 Port/Adapter 解耦

## 特性

- `ApplicationModules.of(...)` 是**纯静态分析**，不启动 Spring Boot
- 测试速度快（通常 < 10 秒），不需要数据库等运行时资源
- 可在 CI 中以很低的成本运行

## Gradle 依赖配置

```groovy
// compileOnly：仅编译期可见，提供 @ApplicationModule/@NamedInterface 注解
compileOnly 'org.springframework.modulith:spring-modulith-core'

// 测试期提供 ApplicationModules、Documenter 工具
testImplementation 'org.springframework.modulith:spring-modulith-starter-test'
```
