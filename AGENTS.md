# Eagle Cloud 项目规则

本文件是 Claude Code 与 Codex 共用的项目规则入口。只按当前任务读取相关规则和 Skill reference，不要一次性加载全部内容。

## 项目基线

- 架构：DDD、六边形架构、Spring Modulith 模块化单体，保留未来微服务拆分能力。
- 技术：Java 25、Gradle 8.x（Groovy DSL）、Spring Boot 4.0.6、Spring Cloud 2025.1.1、Spring Modulith 2.0.5、Hibernate 7.2.6。
- `eagle-services/` 放置服务应用；业务模块遵循 `interfaces / application / domain / infrastructure` 分层。
- `eagle-starter/` 放置可复用 Spring Boot Starter；`eagle-bom` 负责依赖版本对齐。

## 工作方式

- 修改前阅读相邻代码、构建配置、测试以及本文件索引的相关规则，不凭记忆套模板。
- 使用 `rg` / `rg --files` 搜索；遵循目标模块已有结构、命名和实现模式。
- 只修改任务相关文件，不覆盖用户已有改动，不做无关重构或全局格式化。
- 先定位根因再修复，不以吞异常、放宽校验、删除测试或硬编码配置掩盖问题。
- 完成后优先运行受影响模块测试；公共契约、Gradle、跨模块或 Starter 变更再运行完整构建。无法运行时说明原因。

## 后端硬约束

- 分层依赖保持 `interfaces -> application -> domain <- infrastructure`。
- 跨模块协作优先使用 Port、领域事件或 `@NamedInterface` 暴露接口，不直接穿透其他模块内部实现。
- 聚合根继承 `BaseAggregateRoot<T>`，子实体继承 `BaseEntity`，领域事件基于 `BaseEvent`。
- 错误码枚举实现 `ErrorCode`，通过工厂方法创建对应 `AppException`。
- Starter 使用 `@AutoConfiguration` 和类型安全 Properties，并注册到 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。
- 配置通过 `application*.yml`、环境变量和配置属性类管理；不提交密钥、令牌、真实凭据或本地 `.env`。
- 外部 API 使用明确的请求/响应 DTO，不暴露持久化实体；变更接口、事件、Schema 或配置键时处理兼容性和仓库内调用方。
- 数据库、缓存、消息和远程调用需考虑幂等性、事务边界、超时、重试、失败处理和可观测性。

## 测试与注释

- 默认测试不得依赖真实数据库、Redis、Nacos、RabbitMQ、网络或文件系统；使用 Mockito、Fake 或 fixture 隔离基础设施。
- 基础设施相关测试必须放入显式、单独执行的集成或冒烟测试集。
- 修复缺陷时补充能复现问题的测试，测试名表达业务场景和预期结果。
- 注释保持克制，只在复杂业务规则、架构约束、兼容性处理或非直观副作用处，用简短中文说明原因和边界。
- 不逐个为组件、函数或 Hook 写复述代码的注释；同步更新相关注释，删除过期注释和注释掉的代码。

## 按需读取 Rules

规则真源位于 `.agents/rules/`；Claude 通过 `.claude/rules/` 兼容链接读取同一内容。

| 场景 | 规则 |
| --- | --- |
| 命名、Java 风格、Lombok、测试、依赖 | `00-core.md` |
| Java 25：record、sealed、模式匹配、虚拟线程 | `01-java25.md` |
| DDD 分层、Modulith、领域事件、集成事件 | `02-architecture.md` |
| REST、OpenAPI、异常、错误码、i18n | `03-api-error.md` |
| JPA、索引、事务、并发、Schema | `04-data.md` |
| 安全、租户、数据权限、日志 | `05-security.md` |
| Spring Boot 4、Jackson 3、Starter、HTTP 客户端 | `06-boot4.md` |
| 高频陷阱、存量违例、PR 自检 | `07-checklist.md` |
| 内聚、耦合、可维护性 | `08-quality.md` |

只读取本次任务相关规则。Eagle 专有 API、配置、架构边界和踩坑记录必须遵守；与当前代码不一致时，以代码和测试为准并指出差异。

## 按需读取 Skill

统一 Skill 真源位于 `.agents/skills/eagle-cloud/SKILL.md`；Claude 通过 `.claude/skills/eagle-cloud` 兼容链接发现同一 Skill。

涉及 Eagle Cloud Starter、基础设施或端到端开发流程时，先读取统一 `SKILL.md`，再按其路由表只读取实际需要的 `references/*.md`。不要预加载全部 20 份 reference。

## Git 与安全

- Commit 使用带 scope 的 Conventional Commits，例如 `feat(auth): add account aggregate root`。
- 不执行破坏性 Git 命令，不覆盖或清理用户已有改动。
- 认证、租户、上传、缓存和消息功能不得绕过已有安全与数据边界。
