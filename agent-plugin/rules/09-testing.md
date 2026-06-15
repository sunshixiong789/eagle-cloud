# 测试规范

默认使用 JUnit 5 + Mockito。测试重点是证明业务行为、架构边界和 Eagle starter 集成约束，而不是复述实现。

## 必跑范围

- 普通变更：运行受影响模块的 `test`。
- Modulith 边界、跨模块依赖、Named Interface 变更：运行 `*ModulithArchitectureTest`。
- 公共 starter、BOM、Gradle 或公共契约变更：运行 `gradle build`。

## 单元测试

- 测试位于对应模块 `src/test/java`，包路径与被测类一致，命名 `{Name}Test`。
- 优先纯单元测试，不连接真实 DB、Redis、Nacos、网络、文件系统或外部服务。
- 领域模型业务方法、应用服务关键分支、异常路径、边界条件必须覆盖。
- 只 mock 外部依赖，不 mock 被测类本身。

## Spring Context 测试

- 仅用于自动配置、条件装配、Web 层切片等需要 Spring 容器证明的场景。
- Starter 自动配置用 `ApplicationContextRunner` 或等价轻量方式优先。
- 需要基础设施的 smoke test 默认应 `@Disabled("manual infrastructure test")`，避免常规 CI 依赖本地环境。

## 禁止清单

- 单元测试连接真实基础设施。
- 测试之间依赖执行顺序。
- 用 `Thread.sleep()` 等待异步结果；使用 Awaitility、虚拟时钟或可控同步点。
- 提交注释掉的测试代码；确需保留时用 `@Disabled("reason")`。
