# Repository Guidelines

## 项目结构与模块组织

Eagle Cloud 是一个 Gradle 多模块 Java/Spring 项目。`settings.gradle` 定义主要模块：`eagle-bom` 负责依赖版本对齐，`eagle-services/` 放置服务应用，`eagle-starter/` 放置可复用 Spring Boot Starter。各模块遵循标准目录：`src/main/java`、`src/main/resources`、`src/test/java`。Spring Boot 自动配置入口位于 `src/main/resources/META-INF/spring/`。项目文档在 `README.md` 与 `eagle-doc/`，插件和 Agent 相关资产在 `claude-plugin/`。

## 构建、测试与开发命令

当前仓库未提交 Gradle Wrapper，请使用本机 Gradle。

```bash
gradle build
gradle test
gradle :eagle-starter:eagle-websocket-starter:test
gradle :eagle-starter:eagle-rocketmq-starter:build
gradle dependencyUpdates
```

`gradle build` 编译全部模块并运行测试。开发单个 starter 或服务时，优先使用模块级任务获取更快反馈。`dependencyUpdates` 来自 Ben Manes Versions 插件，用于检查依赖升级。

## 编码风格与命名约定

项目使用 Java 25、Spring Boot 4、Groovy Gradle DSL、Lombok 和 MapStruct。包名保持在 `com.eagle.<area>` 下。类名使用清晰职责后缀，例如 `*AutoConfiguration`、`*Properties`、`*Service`、`*Repository`、`*Gateway`、`*Request`、`*Result`。Starter 模块应通过 `@AutoConfiguration` 和 properties 类暴露配置，并在 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册自动配置。保持现有 Java 4 空格缩进和简洁的 SLF4J 日志风格。

## 测试指南

测试基于 JUnit Platform 和 `spring-boot-starter-test`，Mockito 已在 Gradle 中配置为 JVM Agent。测试放在对应模块的 `src/test/java`，包结构与生产代码保持一致。测试类命名为 `*Test`。Starter 工具类优先写聚焦单元测试，自动配置逻辑补充 Spring Context 测试。提交前至少运行受影响模块的 `test`；跨模块修改需运行 `gradle build`。

## 提交与 Pull Request 规范

Git 历史使用带 scope 的 Conventional Commits，例如 `feat(auth): add Account aggregate root`、`docs(rocketmq): add messaging specification`、`chore(config): disable plugin`。每个提交聚焦一个变更，scope 优先使用模块名或领域名。

PR 应包含简明摘要、受影响模块、关联 issue 或背景说明，以及实际执行过的验证命令。修改用户可见行为或公开接口时，补充截图、请求示例或文档链接。

## 安全与配置提示

不要提交密钥。Nexus 发布配置从 Gradle properties 或环境变量读取，可参考 `gradle.properties.example`。凭据、令牌、本地端点不要写入源码或测试夹具。
