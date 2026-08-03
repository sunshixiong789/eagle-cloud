# 架构决策记录（ADR）

`agent-plugin/rules/` 记录的是**做什么**（what），ADR 记录**为什么**（why）。

规则里那些看起来"武断"的禁令 —— 禁物理外键、不用 `ApiResult` 包装、消费方必须独立声明事件类 ——
半年后没人记得取舍过程，就会有人以"这规定不合理"为由绕过去，或者在环境变化后仍固守已经失效的决策。
ADR 让决策可追溯、可推翻。

## 什么时候写

只写**有代价、有争议、难反悔**的决策：

- 引入 / 移除一个基础设施依赖（starter、中间件、框架）
- 定下一条会约束所有人的红线（禁 X、必须 Y）
- 在两个都说得通的方案里选了一个（Port/Adapter vs 共享 jar）
- 推翻之前的决策

**不写**：显而易见的、无争议的、改起来没成本的。ADR 不是变更日志。

## 怎么写

```bash
/new-adr <短标题>          # 生成带编号的骨架
```

命名 `NNNN-kebab-title.md`，编号连续递增，**已接受的 ADR 不修改内容** —— 要改就新写一篇
`Supersedes: NNNN`，并把旧的状态改成 `Superseded by NNNN`。这样决策的演进过程本身是可读的。

## 状态

| 状态 | 含义 |
|---|---|
| `Proposed` | 提出，待评审 |
| `Accepted` | 已采纳，规则据此生效 |
| `Superseded by NNNN` | 被后续决策取代 |
| `Deprecated` | 不再适用，且无替代 |

## 与规则的联动

ADR 被采纳后，在对应规则条目后加一行链接，例如：

```markdown
**所有表禁止 `FOREIGN KEY`** —— 理由见 [ADR-0001](../../docs/adr/0001-no-physical-foreign-keys.md)
```

反过来，ADR 的「决策」段应指明它落地到哪份规则，双向可查。

## 索引

| ADR | 标题 | 状态 |
|---|---|---|
| [0001](0001-no-physical-foreign-keys.md) | 禁止物理外键 | Accepted |
| [0002](0002-no-response-wrapper.md) | 响应不用统一包装类 | Accepted |
| [0003](0003-consumer-declares-own-event-class.md) | 集成事件消费方独立声明事件类 | Accepted |
