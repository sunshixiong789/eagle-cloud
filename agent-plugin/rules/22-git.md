# Git 工作流规范

## 分支

- 主干为 `main`，通过 PR 合入；不使用长期 `develop`。
- 功能分支短生命周期，建议 `feature/{name}`、`fix/{name}`、`chore/{name}`、`release/v{version}`。
- 共享分支禁止 rebase 和普通 `git push --force`；确需覆盖个人分支时用 `--force-with-lease`。

## Commit

使用带 scope 的 Conventional Commits：

```text
feat(auth): add account aggregate root
fix(tenant): propagate tenant context to async tasks
docs(rocketmq): clarify dlq handling
```

- scope 优先使用模块、starter 或领域名。
- 每个 commit 聚焦一个可编译变更。
- 不把业务变更、格式化、重命名、依赖升级混在同一 commit。
- 依赖升级单独 PR，见 `30-dependency.md`。

## PR

- PR 描述包含背景、变更内容、影响范围、实际执行的验证命令、风险与回滚。
- 功能分支合入 `main` 默认 squash；release 分支回合主干可保留 merge commit。
- CI 未通过、测试未跑明、含密钥或夹带无关改动的 PR 不合并。

## 标签

- 版本 Tag 使用 `v{major}.{minor}.{patch}`，预发版使用 `-rc.N`。
- Tag 信息包含摘要、迁移说明、配置变更、回滚方式。

## 禁止清单

- 合并未通过 CI 的 PR。
- 长期堆积大分支。
- 提交密钥、令牌、本地端点、构建产物、IDE 私有配置。
- 未说明原因的空 commit、临时 commit、WIP commit 合入主干。
