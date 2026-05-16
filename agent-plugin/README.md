# Eagle Cloud Agent Plugin

> 把 `eagle-cloud` 基础架子的开发规范、28 个 starter 使用指南、项目级脚手架命令打包成 **同时支持 Claude Code 与 Codex CLI** 的 agent plugin。
> 业务项目接入后，AI（Claude 或 Codex）在编码时自动获得全部约定与 API 知识。

## 内容清单

| 类型              | 数量 | 说明                                                                            |
|-----------------|----|-------------------------------------------------------------------------------|
| Rules（自动注入）     | 30 | DDD / Modulith / 安全 / 日志 / 缓存 / 消息 / 事务 / 多租户 / 性能 / 部署 / 容错弹性 / 事件驱动 等全部开发规范 |
| Commands（slash） | 6  | `/eagle-flow` `/check-arch` `/new-module` `/new-aggregate` `/new-starter` `/add-error-code` |
| Skills（按需加载）    | 29 | 28 个 starter skill + 1 个端到端编排 skill `eagle-feature-flow`，每个含 `SKILL.md` + Codex marketplace 用 `agents/openai.yaml` |

## 安装

### Claude Code

```
/plugin marketplace add https://gitee.com/sunjones/eagle-cloud.git
/plugin install eagle-cloud@eagle-cloud
```

* 第 1 步：让 Claude Code 从这个 git 仓库读取根目录的 `.claude-plugin/marketplace.json`
* 第 2 步：从该 marketplace 安装 `eagle-cloud` 插件（`source` 指向 `./agent-plugin`）
* 安装完成后**重启会话**即可加载

本地调试（不发到 Git）：

```
/plugin marketplace add /Users/you/path/to/eagle-cloud
/plugin install eagle-cloud@eagle-cloud
```

### Codex CLI

```bash
codex plugin marketplace add https://gitee.com/sunjones/eagle-cloud.git
codex plugin install eagle-cloud@eagle-cloud
```

或固定到具体 ref / tag：

```bash
codex plugin marketplace add https://gitee.com/sunjones/eagle-cloud.git --ref main
codex plugin marketplace add sunjones/eagle-cloud@v1.1.0          # GitHub 短形式
```

本地调试：

```bash
codex plugin marketplace add /Users/you/path/to/eagle-cloud
codex plugin install eagle-cloud@eagle-cloud
```

会话内浏览已安装插件 / skill：

```
/plugins
```

### 验证安装

| 测试         | Claude Code                                                                 | Codex CLI                                                          |
|------------|------------------------------------------------------------------------------|--------------------------------------------------------------------|
| Slash 命令识别 | 输入 `/check-arch`，应该补全到 Eagle Cloud 提供的命令                                  | 输入 `/check-arch`，同上                                            |
| 自动加载 skill | 输入 `SecurityUtils.getCurrentUserId()`，AI 应自动加载 `eagle-resource-server` skill | 同上                                                               |
| 关键词触发      | 提问"怎么做缓存击穿防护？" → AI 用 `CacheProtectionUtil.getWithMutex(key, ttl, loader, type)` 4 参数 | 同上                                                               |
| 入口文档识别     | 业务项目根目录看到 `CLAUDE.md` 出现 Eagle 规范条目                                       | 业务项目根目录看到 `AGENTS.md` 出现 Eagle 规范条目                          |

### 兜底方案：Submodule + 软链

适合无法搭建 marketplace 或工具暂不支持的场景：

```bash
cd your-business-project
git submodule add https://gitee.com/sunjones/eagle-cloud.git .eagle-cloud

# Claude
ln -s ../.eagle-cloud/agent-plugin/rules .claude/rules-eagle
ln -s ../.eagle-cloud/agent-plugin/commands/check-arch.md .claude/commands/check-arch.md

# Codex
ln -s ../.eagle-cloud/agent-plugin/rules .codex/rules-eagle
ln -s ../.eagle-cloud/agent-plugin/commands/check-arch.md .codex/commands/check-arch.md

# 业务项目自身的 CLAUDE.md / AGENTS.md 中追加引用
cat >> CLAUDE.md <<'EOF'

## Eagle Cloud 规范
开发规范见 .claude/rules-eagle/，starter 使用见 .eagle-cloud/agent-plugin/skills/
EOF
```

## 文档导航

- **[USAGE.md](./USAGE.md)** ★ 团队使用指南（开发者必读）— 5 分钟上手、3 种使用层次、典型场景走读、FAQ、故障排查
- [DEPLOYMENT.md](./DEPLOYMENT.md) — 私有 marketplace 部署、CI 配置、权限管理
- [INTEGRATION-TEST.md](./INTEGRATION-TEST.md) — 接入后的验收 checklist
- [CHANGELOG.md](./CHANGELOG.md) — 版本变更记录

## 目录结构

```
eagle-cloud/                                  仓库根（monorepo）
├── .claude-plugin/marketplace.json           Claude Code marketplace（指向 ./agent-plugin）
└── agent-plugin/                             插件本体
    ├── .claude-plugin/plugin.json            Claude Code plugin manifest
    ├── .codex-plugin/plugin.json             Codex CLI plugin manifest（含 interface 元数据）
    ├── CLAUDE.md                             Claude 入口（注入业务项目）
    ├── AGENTS.md                             Codex 入口（注入业务项目，独立文件）
    ├── LICENSE                               Apache 2.0
    ├── README.md / USAGE.md / DEPLOYMENT.md / INTEGRATION-TEST.md / CHANGELOG.md
    ├── rules/                                30 份开发规范
    ├── commands/                             6 个 slash commands（Claude/Codex 都自动注册）
    ├── skills/                               29 个 skill，每个含 SKILL.md + agents/openai.yaml
    └── scripts/sync.sh                       从 eagle-starter/*/USAGE.md 重新生成 skills
```

## Skill 触发机制

每个 `SKILL.md` 的 frontmatter 有 `description` 字段（精心写的英文触发关键词）。Codex 还额外读 `agents/openai.yaml` 的 `display_name` + `short_description` 作 marketplace UI 展示。两个工具自动加载 skill 的时机：

- 业务代码涉及该 starter 的功能（如代码 import 了 `RedisDistributedLock` 会触发 `eagle-redis`）
- 用户提问涉及该领域（如"怎么做幂等？"会触发 `eagle-idempotency`）
- 检测到该 starter 的依赖在 `build.gradle` 中

**查看当前已加载的 skill**：在 Claude Code 或 Codex CLI 会话中，AI 会在系统提示中列出可用 skill 名称；Codex 用户也可在会话内打 `/plugins` 浏览。

## 维护流程（开发者侧）

修改源 → 同步 skills → commit → 发布。

```bash
# 1) 修改源：在 agent-plugin/rules/ 直接编辑 rules，或修改 eagle-starter/*/USAGE.md
vim eagle-starter/eagle-redis-starter/USAGE.md

# 2) 重新生成 skills（rules 与 commands 不再二次同步，直接在 agent-plugin/ 内编辑）
bash ./agent-plugin/scripts/sync.sh

# 3) 验证差异
git diff agent-plugin/

# 4) 提交
git add agent-plugin/
git commit -m "docs(plugin): update redis usage"

# 5) 升级版本（同步改 agent-plugin/.claude-plugin/plugin.json 与 .codex-plugin/plugin.json
#    以及根 /.claude-plugin/marketplace.json 中的 version 字段）
# 按 SemVer：bug 修复 → patch；新增 skill/rule → minor；breaking change → major
```

> **注意**：`agent-plugin/CLAUDE.md` 与 `agent-plugin/AGENTS.md` 是两份独立的指令文件（不再 symlink），共享 ~95% 内容。修改其中一份时务必对照另一份保持同步——首部声明和"Codex 使用提示"那一节允许不同，其它（rules / commands / starter skill / 高频陷阱）应一致。

## 版本兼容矩阵

| Plugin 版本 | eagle-cloud（基建版本） | Spring Boot | 说明                                        |
|-----------|-------------------|-------------|---------------------------------------------|
| 1.1.0     | 当前主干（2026-05-16）  | 4.0.6       | Codex CLI 支持、AGENTS.md 独立、agents/openai.yaml |
| 1.0.0     | 2026-04-30 主干     | 4.0.6       | 初始版本，仅 Claude Code                       |

**`ref` 建议使用 `"main"`**，直接跟踪主干，无需提前打 Tag。如团队有版本冻结需求，也可指定 commit SHA（如 `"ref": "a5e251f"`）或 tag。

## 升级建议

| 变更类型                   | 处理                                   |
|------------------------|--------------------------------------|
| 新增 starter             | minor 版本升级，业务项目无感升级即可                |
| 修改既有 API（破坏性）          | **major 版本升级**，发布前在 CHANGELOG 列明迁移指南 |
| 修复 USAGE.md 错别字 / 补充示例 | patch 版本升级                           |

## 反馈

发现 USAGE/规则错漏 → 在 `eagle-cloud` 仓库提 PR 修改源（`agent-plugin/rules/` 或 `eagle-starter/{name}/USAGE.md`），CI 跑 `sync.sh` 后合并即可。

## 许可

Apache-2.0（见 [LICENSE](./LICENSE)）
