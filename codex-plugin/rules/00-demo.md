# Codex Rules Demo

## 适用场景

当 Codex 在 Eagle Cloud 项目中修改 Java、Gradle 或 Spring Boot Starter 相关代码时，优先参考本规则。

## 基本原则

- 修改前先阅读相关模块的 `build.gradle`、生产代码和测试代码。
- 保持现有包结构、命名风格和 DDD 分层，不引入无关重构。
- Starter 模块优先使用 `@AutoConfiguration`、`*Properties` 和 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。
- 新增或修改行为时，优先补充对应模块下的 `src/test/java` 测试。

## 命令示例

```bash
gradle :eagle-starter:eagle-websocket-starter:test
gradle :eagle-starter:eagle-rocketmq-starter:build
gradle build
```

## 输出要求

完成任务时说明：

- 改动了哪些文件。
- 运行了哪些验证命令。
- 如果未运行测试，说明原因。
