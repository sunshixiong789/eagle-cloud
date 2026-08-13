# Eagle Feature Flow

仅当用户明确输入 `/eagle-flow`，或明确说“按 eagle flow 走”“启动 eagle flow”“走 eagle flow”时使用。普通新增功能、修复缺陷或重构请求不要自动进入此流程。

## 六阶段流程

1. **需求澄清**：确认目标、边界、验收条件和关键取舍；存在实质性歧义时先讨论设计。
2. **计划**：读取当前任务相关的 `.agents/rules/*.md`，再按统一 Skill 路由读取需要的 `references/*.md`；把实现、测试和验证步骤写进计划。
3. **TDD 实现**：先写能证明行为的失败测试，再做最小实现；需要新模块、聚合根、Starter 或错误码时，遵循当前项目结构手工创建，不依赖已删除的 slash command。
4. **验证**：先运行受影响模块测试；涉及 Modulith 时运行对应 `ModulithArchitectureTest`；跨模块、公共契约、Gradle 或 Starter 变更后运行 `gradle build`。
5. **评审**：读取 `.agents/rules/07-checklist.md` 和相关领域规则，检查架构边界、API、数据、安全、并发、日志、测试和兼容性。
6. **收尾**：检查差异和未验证项；如用户要求提交，使用带 scope 的 Conventional Commit，并给出准确的变更与验证摘要。

## 按阶段加载知识

- 计划阶段：只读取与任务相关的 rules，不加载全部规则。
- 实现阶段：从当前 Skill 的路由表选择最少的 references。例如 RabbitMQ 读 `amqp.md`，JPA 读 `data-jpa.md`，鉴权读 `resource-server.md`。
- 验证阶段：依据实际模块选择 Gradle 任务，不假设仓库存在 Wrapper、插件命令或固定模块名。
- 评审阶段：除 `07-checklist.md` 外，只补读与改动风险相关的规则。

## 必须保持

- 每个阶段的产出要能被下一阶段验证，不能用“已完成”替代证据。
- 测试不得依赖真实数据库、Redis、Nacos、RabbitMQ、网络或文件系统，除非它是显式、单独执行的集成/冒烟测试。
- 当前代码、测试与旧 reference 不一致时，以代码和测试为准，并同步修正文档或指出差异。
- 不执行旧插件路径、旧 slash command、RocketMQ API 或不存在的 `./gradlew` 命令。
