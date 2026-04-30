# Plugin 接入测试 Checklist

业务项目接入 `eagle-cloud` plugin 后，按以下步骤验证 plugin 是否正确生效。
建议接入后立即执行一次完整流程，确认 AI 能正确使用 starter API。

## 前置准备

- [ ] 业务项目 `.claude/settings.json` 已配置 `enabledPlugins`（参见 `README.md` 三种方式之一）
- [ ] 启动 Claude Code 会话（`claude` 命令）
- [ ] 执行 `/help` 应能看到自定义命令

## 阶段 1：Plugin 加载验证（5 分钟）

### 1.1 命令注册检查

在 Claude Code 会话中输入：

```
/help
```

**预期**：能看到以下 5 个项目级命令：

```
/check-arch         Modulith 架构验证 + 模块测试 + 全量构建一键检查
/new-module         按 Modulith + DDD 模板创建新业务模块
/new-aggregate      在指定模块中创建聚合根 + Repository + ErrorCode + ApplicationService + Controller + DTO 全栈骨架
/new-starter        按 Spring Boot 3 自动配置模板创建新 starter 模块
/add-error-code     在指定 ErrorCode 枚举追加常量并同步在 i18n messages 文件中加翻译
```

- [ ] 5 个命令全部出现

### 1.2 规则注入检查

询问 AI：

```
你现在能查阅哪些 eagle-cloud 开发规范？列出文件名。
```

**预期**：AI 应列出 `01-naming.md` 到 `30-dependency.md` 共 28 份规则。

- [ ] AI 能列出所有规则
- [ ] AI 能引用规则文件（如"详见 13-logging.md"）

### 1.3 Skill 触发验证

在业务代码中写一行触发性代码：

```java
// 让 AI 帮你完成的代码
public class TestUserService {
    public User findById(Long id) {
        // TODO: 缓存防击穿
    }
}
```

询问 AI：

```
帮我完成这个方法，用 eagle-cloud 的缓存防击穿能力。
```

**预期**：AI 应：
- 自动加载 `eagle-redis` skill
- 使用 **`cacheProtectionUtil.getWithMutex(key, ttl, loader, User.class)`**（4 参数含 Class）
- 不会编造 `getWithLock` 等不存在的方法

- [ ] AI 使用了正确的 API（`getWithMutex` 4 参数）
- [ ] AI 没有编造方法名

## 阶段 2：API 准确性验证（10 分钟）

针对 5 个高频偏差点，逐一验证 AI 是否使用正确 API。

### 2.1 DistributedLock

询问：

```
帮我加分布式锁，等待 5 秒，持锁 30 秒。
```

**预期 ✅**：

```java
return lock.tryLock("biz:key:" + id, 5L, 30L, () -> {
    // ...
});
```

**反例 ❌**：`lock.executeWithLock(key, Duration.ofSeconds(5), () -> {})`

- [ ] 使用 `tryLock`，参数是 **`long` 秒**而非 `Duration`

### 2.2 TenantContextHolder

询问：

```
异步任务里怎么设置当前租户上下文？
```

**预期 ✅**：`TenantContextHolder.setTenantId("xxx")` / `getTenantId()` / `clear()`

**反例 ❌**：`setCurrentTenantId(...)` / `getCurrentTenantId()`

- [ ] 使用正确的方法名

### 2.3 RocketMQ 消费者

询问：

```
写一个 OrderCreatedEvent 的消费者。
```

**预期 ✅**：

```java
@Component
public class OrderCreatedConsumer extends AbstractRocketMqListener<OrderCreatedEvent> {
    @Override protected String getTopic() { return "..."; }
    @Override protected Class<OrderCreatedEvent> getEventClass() { return OrderCreatedEvent.class; }
    @Override protected void handle(OrderCreatedEvent event) { ... }
}
```

**反例 ❌**：`@RocketMQMessageListener(topic = "...")` 注解、`handle(event, MessageExt msg)` 双参数

- [ ] 继承 `AbstractRocketMqListener<T>`
- [ ] 实现 3 个抽象方法
- [ ] 没有用 `@RocketMQMessageListener`
- [ ] `handle(T event)` 单参数

### 2.4 DataScope 数据范围

询问：

```
怎么实现"本部门及子部门可见"的数据权限？
```

**预期 ✅**：`DataScope.DEPT_AND_CHILD` 配合 `DataPermissionProvider`

**反例 ❌**：`DataScope.DEPT_ONLY`（不存在）、`@DataPermission(type = ...)`（无 type 字段）

- [ ] 使用 `DEPT_AND_CHILD`（不是 `DEPT_ONLY`）
- [ ] `@DataPermission(deptField, userField)`，无 `type`

### 2.5 审计字段命名

询问：

```
JPA 实体的审计字段叫什么名字？
```

**预期 ✅**：`createBy / updateBy / createTime / updateTime`

**反例 ❌**：`createdBy / updatedBy / createdAt / updatedAt`

- [ ] AI 答出正确字段名

## 阶段 3：架构约束验证（5 分钟）

### 3.1 Controller 权限注解

让 AI 写一个 Controller：

```
帮我写一个订单查询 Controller。
```

**预期 ✅**：每个方法都有 `@PreAuthorize`（详见 `05-api.md`）

- [ ] 每个 endpoint 都有 `@PreAuthorize`
- [ ] 创建接口返回 `201 Created`
- [ ] 列表用 `Page<T>` 或 `EaglePageResult<T>`

### 3.2 实体规范

让 AI 写一个聚合根：

```
帮我写一个 Order 聚合根，含订单号、金额、状态。
```

**预期 ✅**：
- 继承 `BaseAggregateRoot<Order>`
- `@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)`
- 静态工厂方法
- 业务方法替代 setter

**反例 ❌**：`@Data` / `@Builder`、暴露 setter、构造函数 public

- [ ] 没有 `@Data` / `@Builder`
- [ ] 用静态工厂方法

### 3.3 跨域依赖

询问：

```
order 模块需要调用 user 模块的查询能力，怎么写？
```

**预期 ✅**：在 `order/domain/port/` 定义接口，由 user/infrastructure/adapter 实现（六边形架构）

**反例 ❌**：直接 `import com.eagle.system.base.user.UserRepository`

- [ ] 使用 Port + Adapter
- [ ] 提及 `@NamedInterface` 暴露

## 阶段 4：Slash Command 验证（5 分钟）

### 4.1 /check-arch

```
/check-arch
```

**预期**：AI 跑 `./gradlew :xxx:test --tests "*.ModulithArchitectureTest"` 并输出报告

- [ ] 命令被正确识别和执行

### 4.2 /new-aggregate

```
/new-aggregate eagle-system-server:order:Order
```

**预期**：生成完整骨架（聚合根 + Repository + ErrorCode + ApplicationService + Controller + DTO）

- [ ] 生成的文件包结构正确
- [ ] 文件包含真实 API（如 `BaseAggregateRoot<Order>`）

## 阶段 5：版本与升级验证（2 分钟）

### 5.1 Plugin 版本

询问 AI：

```
当前 eagle-cloud plugin 是什么版本？
```

**预期**：能从 CLAUDE.md 或 plugin.json 中找到版本号 `1.0.0`

- [ ] 版本可被 AI 识别

### 5.2 Skill 列表

询问：

```
列出所有 eagle starter skill。
```

**预期**：22 个 skill 全部出现（`eagle-common` 到 `eagle-websocket`）

- [ ] 22 个 skill 全部可见

## 通过标准

- 阶段 1（加载验证）：3/3 必须通过
- 阶段 2（API 准确性）：5/5 必须通过 — 这是 plugin 价值核心
- 阶段 3（架构约束）：3/3 必须通过
- 阶段 4（Commands）：2/2 必须通过
- 阶段 5（版本）：2/2 必须通过

任一阶段失败 → 检查 plugin 加载或源数据，提 issue 到 eagle-cloud 仓库。

## 失败排查

| 现象 | 可能原因 | 排查 |
|------|---------|------|
| `/help` 看不到命令 | plugin 未加载 | 检查 `.claude/settings.json` 中 `enabledPlugins` 配置 |
| AI 用编造的 API | skill 未触发 | 在询问中明确说"用 eagle-cloud 的 X starter" |
| 全部失败 | marketplace 配置错 | 检查 marketplace `path` 指向是否正确 |
| 部分 skill 缺失 | sync.sh 未跑 | `./claude-plugin/sync.sh` 后重启会话 |

## 报告模板

完成测试后填写：

```
=== Plugin 接入测试报告 ===
日期：YYYY-MM-DD
业务项目：xxx
Plugin 版本：1.0.0
接入方式：A（git marketplace） / B（local） / C（submodule）

阶段 1（加载）：✅ / ❌
阶段 2（API 准确性）：__/5
阶段 3（架构约束）：__/3
阶段 4（Commands）：__/2
阶段 5（版本）：__/2

发现问题：
1.
2.

整体结论：✅ 可上线 / ⚠️ 需修复后再上线 / ❌ plugin 不可用
```
