---
description: 启动 Eagle 端到端开发流程 — Superpowers 6 阶段主干 + agent-plugin 知识注入
argument-hint: "[功能描述,可选;如\"用户积分系统\"]"
---

# /eagle-flow — 启动 eagle-feature-flow

显式触发 `eagle-feature-flow` skill,按 6 阶段固定顺序走完一次端到端开发流程
(Superpowers 主干 + agent-plugin 嵌入式注入)。

## 执行步骤

1. **加载 skill**

   立刻用 `Skill` 工具调用 `eagle-feature-flow`(本 plugin 提供),
   该 skill 内含完整 6 阶段定义与每阶段的 agent-plugin 注入要求。

2. **解析参数**

   - 若 `$ARGUMENTS` 非空 → 作为本次功能描述传给 Phase 1(Brainstorm),
     直接进入需求澄清,无需用户再开口
   - 若 `$ARGUMENTS` 为空 → 询问"本次要做的功能是什么?",
     收到回复后进入 Phase 1

3. **TodoWrite 创建 6 项 todo**

   - [ ] Phase 1/6: Brainstorm
   - [ ] Phase 2/6: Plan
   - [ ] Phase 3/6: TDD
   - [ ] Phase 4/6: Verify
   - [ ] Phase 5/6: Review
   - [ ] Phase 6/6: Finish

4. **按 skill 定义的顺序执行**

   每进入一个新阶段,输出一行宣告:
   ```
   [Eagle Flow] Phase N/6: <name>
   ```

   阶段间不要静默切换;阶段失败必须修复后再进入下一阶段。

## 触发示例

### 示例 1:带参数

```
/eagle-flow 用户积分系统
```

→ 直接进入 Phase 1 Brainstorm,围绕"用户积分系统"开始澄清。

### 示例 2:不带参数

```
/eagle-flow
```

→ 询问"本次要做的功能是什么?",收到回复后再进入 Phase 1。

### 示例 3:重构场景

```
/eagle-flow 把 OrderService 里的支付逻辑抽出来做成支付适配器
```

→ Phase 1 澄清重构边界 → Phase 2 plan(预计跳 Phase 3 的 scaffold)→ ...

## 与自然语言触发的等价性

以下两种方式效果完全相同:

| 方式 | 例子 |
|---|---|
| Slash command | `/eagle-flow 用户积分系统` |
| 自然语言 | `我要做一个用户积分系统` |

`eagle-feature-flow` skill 的 description 已经覆盖"新功能/加一个模块/重构 X"等触发短语,
模型会自动激活;slash command 是**显式入口**,适合:

- 新人不熟悉哪些短语会激活 flow
- 团队规范要求"启动 flow 必须用 `/eagle-flow`,以便审计"
- 用户希望明确启动而非依赖模型判断

## 跳过 / 中断 / 恢复

- **跳过某阶段**:在该阶段开始时说"跳过 Phase N(原因)" — 仅纯重构允许跳 Phase 3 scaffold
- **中断**:任意时刻说"暂停",当前阶段保留状态,下次说 `/eagle-flow` 或"继续 eagle flow"恢复
- **跨 session 恢复**:下次会话用 `/eagle-flow continue`,模型读 TodoWrite 中未完成的最低编号阶段恢复

## 参考

- `skills/eagle-feature-flow/SKILL.md` — flow 完整定义
- `USAGE.md` — 三种使用层次、典型场景走读、FAQ
- `rules/25-review-checklist.md` — Phase 5 评审依据
