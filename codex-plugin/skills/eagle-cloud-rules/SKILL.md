---
name: eagle-cloud-rules
description: Use when working in Eagle Cloud projects and needing repository rules for Java, Spring Boot 4, DDD, Spring Modulith, starters, testing, security, messaging, deployment, or code review.
---

# Eagle Cloud Rules

## 何时使用

在 Eagle Cloud 项目中进行非 trivial 的代码、测试、Gradle 或文档修改前，先使用本 skill。

## 规则索引

只加载与当前任务相关的规则文件：

- `../rules/01-naming.md` — Java、DDD、错误码和模块命名规范。
- `../rules/02-code-style.md` — Java 风格、Lombok、空值标注和格式要求。
- `../rules/03-architecture.md` — DDD 分层和六边形架构。
- `../rules/04-modulith.md` — Spring Modulith 边界和 Named Interface。
- `../rules/09-testing.md` — JUnit、Mockito、测试命名和覆盖要求。
- `../rules/10-starter.md` — Spring Boot Starter 与自动配置模式。
- `../rules/12-security.md` — OAuth2、JWT、敏感数据和审计规则。
- `../rules/15-messaging.md` — RocketMQ Topic、幂等、死信和事务消息。
- `../rules/22-git.md` — 分支、提交、PR 和发布约定。
- `../rules/25-review-checklist.md` — 完成前的最终自检清单。
- `../rules/30-dependency.md` — Gradle 依赖范围、BOM、升级和 CVE 检查。

## 工作流程

1. 识别本次修改涉及的领域，并读取匹配的规则文件。
2. 遵循现有模块结构，避免无关重构。
3. 开发过程中优先运行模块级 Gradle 验证。
4. 完成前说明修改了哪些文件，以及运行了哪些验证命令。
