# Eagle Cloud Claude Code Plugin

> 把 `eagle-cloud` 基础架子的开发规范、28 个 starter 使用指南、项目级脚手架命令打包为一个 Claude Code Plugin。
> 业务项目接入后，AI 在编码时自动获得全部约定与 API 知识。

## 内容清单

| 类型              | 数量 | 说明                                                                            |
|-----------------|----|-------------------------------------------------------------------------------|
| Rules（自动注入）     | 30 | DDD / Modulith / 安全 / 日志 / 缓存 / 消息 / 事务 / 多租户 / 性能 / 部署 / 容错弹性 / 事件驱动 等全部开发规范 |
| Commands（slash） | 6  | `/eagle-flow` `/check-arch` `/new-module` `/new-aggregate` `/new-starter` `/add-error-code` |
| Skills（按需加载）    | 29 | 28 个 starter skill + 1 个端到端编排 skill `eagle-feature-flow`                       |

## 文档导航

- **[USAGE.md](./USAGE.md)** ★ 团队使用指南(开发者必读) — 5 分钟上手、3 种使用层次、典型场景走读、FAQ、故障排查
- [DEPLOYMENT.md](./DEPLOYMENT.md) — 私有 marketplace 部署、CI 配置、权限管理
- [INTEGRATION-TEST.md](./INTEGRATION-TEST.md) — 接入后的验收 checklist
- [CHANGELOG.md](./CHANGELOG.md) — 版本变更记录

## 目录结构

```
claude-plugin/
├── plugin.json               # Plugin 元数据
├── marketplace.json          # 私有 marketplace 索引
├── README.md                 # 本文件
├── CLAUDE.md                 # 注入业务项目的总入口
├── sync.sh                   # 从仓库源同步内容（开发期使用）
├── rules/                    # 30 份规则文件
├── commands/                 # 6 个 slash commands
└── skills/                   # 28 个 starter skill
    ├── eagle-common/SKILL.md
    ├── eagle-redis/SKILL.md
    └── ...
```

## 维护流程（开发者侧）

修改源 → 同步到 plugin → commit → 发布。

```bash
# 1) 修改 .claude/rules/, .claude/commands/, eagle-starter/*/USAGE.md
vim eagle-starter/eagle-redis-starter/USAGE.md

# 2) 跑同步
./claude-plugin/sync.sh

# 3) 验证差异
git diff claude-plugin/

# 4) 提交
git add claude-plugin/
git commit -m "docs(plugin): update redis usage"

# 5) 升级版本（plugin.json 和 marketplace.json）
# 按 SemVer：bug 修复 → patch；新增 skill/rule → minor；breaking change → major
```

## 业务项目接入方式

### 方式 A：私有 Marketplace（推荐团队级）

1. 把 `eagle-cloud` 仓库（含 `claude-plugin/`）发布到内部 Git 服务（**GitHub / GitLab / Gitee / Gitea** 均可）
2. 业务项目在 `.claude/settings.json` 启用（按 Git 服务选择 URL）：

```json
{
  "marketplaces": {
    "eagle-cloud-internal": {
      "type": "git",
      "url": "git@gitee.com:your-org/eagle-cloud.git",
      "path": "claude-plugin",
      "ref": "main"
    }
  },
  "enabledPlugins": {
    "eagle-cloud@eagle-cloud-internal": true
  }
}
```

各 Git 服务 URL 示例：

| 服务            | URL 格式                                             |
|---------------|----------------------------------------------------|
| Gitee（国内访问最快） | `git@gitee.com:eagle/eagle-cloud.git`              |
| GitHub        | `git@github.com:eagle/eagle-cloud.git`             |
| GitLab 自建     | `git@gitlab.your-domain.com:eagle/eagle-cloud.git` |
| Gitea 自建      | `git@gitea.your-domain.com:eagle/eagle-cloud.git`  |

3. Claude Code 启动时自动拉取并加载

详细部署步骤（含权限、CI 配置）见 `DEPLOYMENT.md`。

### 方式 B：本地 plugin（开发调试 / 单人项目）

1. 业务项目本地 clone `eagle-cloud` 仓库
2. `.claude/settings.json` 引用本地路径：

```json
{
  "marketplaces": {
    "eagle-cloud-local": {
      "type": "local",
      "path": "/path/to/eagle-cloud/claude-plugin"
    }
  },
  "enabledPlugins": {
    "eagle-cloud@eagle-cloud-local": true
  }
}
```

### 方式 C：Submodule + 软链（兜底）

适合无法搭建 marketplace 的场景：

```bash
cd your-business-project
git submodule add https://git.your-domain.com/eagle/eagle-cloud.git .eagle-cloud

# 软链 plugin 内容到 .claude/
ln -s ../.eagle-cloud/claude-plugin/rules .claude/rules-eagle
ln -s ../.eagle-cloud/claude-plugin/commands/check-arch.md .claude/commands/check-arch.md
# ... 其他需要的文件

# 业务项目自身的 CLAUDE.md 中引用
echo "
## Eagle Cloud 规范
开发规范见 .claude/rules-eagle/，starter 使用见 .eagle-cloud/claude-plugin/skills/
" >> CLAUDE.md
```

## Skill 触发机制

每个 SKILL.md 的 frontmatter 有 `description` 字段（精心写的英文触发关键词）。AI 在以下情况自动加载对应 skill：

- 业务代码涉及该 starter 的功能（如代码 import 了 `RedisDistributedLock` 会触发 `eagle-redis`）
- 用户提问涉及该领域（如"怎么做幂等？"会触发 `eagle-idempotency`）
- 检测到该 starter 的依赖在 `build.gradle` 中

**查看当前已加载的 skill**：在 Claude Code 会话中，AI 会在系统提示中列出可用 skill 名称。

## 版本兼容矩阵

| Plugin 版本 | eagle-cloud（基建版本） | Spring Boot | 说明   |
|-----------|-------------------|-------------|------|
| 1.0.0     | 当前主干（2026-04-30）  | 4.0.3       | 初始版本 |

**`ref` 建议使用 `"main"`**，直接跟踪主干，无需提前打 Tag。如团队有版本冻结需求，也可指定 commit SHA（如 `"ref": "a5e251f"`）。

## 升级建议

| 变更类型                   | 处理                                   |
|------------------------|--------------------------------------|
| 新增 starter             | minor 版本升级，业务项目无感升级即可                |
| 修改既有 API（破坏性）          | **major 版本升级**，发布前在 CHANGELOG 列明迁移指南 |
| 修复 USAGE.md 错别字 / 补充示例 | patch 版本升级                           |

## 验证

接入后在业务项目 Claude Code 会话中验证：

```
/check-arch                                # 应能识别命令
SecurityUtils.getCurrentUserId()           # 输入此代码，AI 应自动加载 eagle-resource-server skill
"我要做缓存击穿防护"                       # AI 应自动加载 eagle-redis skill 并使用 getWithMutex(4 参)
```

## 反馈

发现 USAGE/规则错漏 → 在 `eagle-cloud` 仓库提 PR 修改源（`.claude/rules/` 或 `eagle-starter/{name}/USAGE.md`），CI 跑
`sync.sh` 后合并即可。

## 许可

Apache-2.0
