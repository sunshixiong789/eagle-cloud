# AGENTS.md

Codex 在本仓库工作时只把本文件当作入口索引。Claude Code 专用说明见 `CLAUDE.md`；Eagle 平台细则按需读取
`.agents/rules/` 和 `agent-plugin/skills/`，不要一次性展开全部规则。

## 项目快照

- Eagle Cloud：DDD + 六边形架构 + Spring Modulith 模块化单体，面向未来微服务拆分。
- Java 25 / Gradle 8.x（Groovy DSL）；仓库未提交 Gradle Wrapper，命令使用本机 `gradle`。
- Spring Boot 4.0.6 / Spring Cloud 2025.1.1 / Spring Cloud Alibaba 2025.1.0.0 / Spring Modulith 2.0.5。
- Hibernate 7.2.6、JPA、Spring Security OAuth2、SpringDoc、Redisson、RocketMQ、XXL-JOB、Seata、MinIO。

## 模块

- `eagle-bom`：依赖版本对齐。
- `eagle-services/`：服务应用，业务模块按 `interfaces / application / domain / infrastructure` 分层。
- `eagle-starter/`：可复用 Spring Boot Starter。
- `eagle-doc/`：项目文档。
- `agent-plugin/`：rules、commands、starter skills。

## 常用命令

```bash
gradle build
gradle test
gradle :eagle-starter:eagle-websocket-starter:test
gradle :eagle-starter:eagle-rocketmq-starter:build
gradle dependencyUpdates
```

开发期优先跑受影响模块的 `test`；跨模块、公共契约、Gradle 或 starter 变更后跑 `gradle build`。

## 后端硬约束

- 分层依赖保持 `interfaces -> application -> domain <- infrastructure`。
- 跨模块协作优先用 Port、领域事件、`@NamedInterface` 暴露接口；不要直接穿透其他模块内部实现。
- 聚合根继承 `BaseAggregateRoot<T>`；子实体继承 `BaseEntity`；领域事件基于 `BaseEvent`。
- 错误码枚举实现 `ErrorCode`，通过工厂方法创建 `AppException`。
- Starter 使用 `@AutoConfiguration`、类型安全 `Properties`，并注册到
  `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。
- 测试不依赖真实 DB、Redis、Nacos、网络或文件系统；提交前至少验证受影响模块。

## 前端约束（仓库出现前端代码时）

- 优先遵循现有框架、组件库、状态管理和目录结构，不另起设计体系。
- API 契约以 DTO / OpenAPI / 后端错误码为准；前端展示使用 i18n key 或统一错误码映射，不写死后端消息。
- 鉴权、租户、分页、上传、缓存等交互遵守后端对应规则文件；不要绕过后端安全与数据边界。
- 修改 UI 后启动本地页面并做浏览器验证；只改类型或纯工具函数时可用单元测试替代。

## 按场景读取规则

后端规则位于 `.agents/rules/`（软链接为 `.claude/rules/`），共 8 份：

| 场景 | 规则 |
| --- | --- |
| 命名 / Java 风格 / Lombok / 测试 / 依赖 | `00-core.md` |
| Java 25 语言基线（record / sealed / 模式匹配 / 虚拟线程） | `01-java25.md` |
| DDD 分层 / Modulith / 领域事件 / 集成事件 | `02-architecture.md` |
| REST / OpenAPI / 异常 / 错误码 / i18n | `03-api-error.md` |
| JPA / 索引 / 事务 / 并发 / Schema | `04-data.md` |
| 安全 / 租户 / 数据权限 / 日志 | `05-security.md` |
| Spring Boot 4 / Jackson 3 / starter / HTTP 客户端 | `06-boot4.md` |
| 高频陷阱 / 存量违例台账 / PR 自检 | `07-checklist.md` |
| 缓存 / 消息 / 分布式事务 / 调度 / 存储 / 韧性 | 对应 starter skill（`eagle-redis` / `eagle-rocketmq` / `eagle-seata` / `eagle-scheduler` / `eagle-oss-minio` / `eagle-resilience`） |

前端规则位于 `agent-plugin/rules-frontend/`（本仓库无前端代码，供下游前端项目使用）。

只读取本次任务相关规则。规则里若只是通用编程常识，以现有代码风格和模型默认能力处理；若包含 Eagle 专有 API、命名、边界、配置或踩坑记录，必须遵守。

## Starter Skill

涉及具体 starter 时，除规则外读取对应 skill：RocketMQ、Redis、JPA、多租户、资源服务器、OpenAPI、WebSocket、MinIO、
Scheduler、Seata、Sentinel、AI 等均在 `agent-plugin/skills/` 下。

## 项目级命令文档

Codex 不自动执行 Claude slash command，但实现同类任务时可参考：

- `/check-arch`：Modulith 架构验证、模块测试、全量构建。
- `/new-module`：创建 DDD 业务模块。
- `/new-aggregate`：创建聚合根全栈骨架。
- `/new-starter`：创建 Spring Boot 4 starter。
- `/add-error-code`：追加 ErrorCode 并同步 i18n。

## Git / 安全

- Commit 使用带 scope 的 Conventional Commits，例如 `feat(auth): add account aggregate root`。
- 不提交密钥、令牌、本地端点和真实凭据；Nexus 发布配置来自 Gradle properties 或环境变量。
- 不覆盖用户已有改动，不执行破坏性 Git 命令。

## Codex 工作方式

- 开始修改前先看相关文件和规则，不凭记忆套模板。
- 使用 `rg` / `rg --files` 搜索。
- 改动保持聚焦，避免无关重构和格式化噪音。
- 完成前运行能证明变更有效的验证命令；无法运行时说明原因。
