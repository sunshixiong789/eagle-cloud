# 私有 Marketplace 部署指南

把 `eagle-cloud` Plugin 发布到内部 Git 服务，让团队所有业务项目通过 Marketplace 引用。

## 部署架构

```
                    ┌─────────────────────────────┐
                    │  eagle-cloud (源仓库)        │
                    │  └─ claude-plugin/           │
                    │       ├─ plugin.json         │
                    │       ├─ marketplace.json    │
                    │       ├─ rules/              │
                    │       ├─ commands/           │
                    │       └─ skills/             │
                    └──────────┬──────────────────┘
                               │ git clone / pull
                               ↓
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
   ┌────▼────────┐    ┌────────▼────────┐   ┌─────────▼────────┐
   │ 业务项目 A   │    │ 业务项目 B       │   │ 业务项目 C        │
   │ 引用插件     │    │ 引用插件         │   │ 引用插件          │
   └─────────────┘    └─────────────────┘   └──────────────────┘
```

## 选项 1：GitLab 部署（推荐，国内最常见）

### 1.1 推送 eagle-cloud 仓库

```bash
# 在 GitLab 创建项目：eagle/eagle-cloud（或 group/eagle-cloud）
cd /path/to/eagle-cloud
git remote add gitlab git@gitlab.your-domain.com:eagle/eagle-cloud.git
git push gitlab main
```

### 1.2 设置访问权限

GitLab 项目设置 → **Members** → 添加团队成员（至少 Reporter 角色，可读即可）。

如需个人访问令牌（PAT）认证：
- GitLab → User Settings → Access Tokens → 创建 `read_repository` scope token
- 把 token 加到业务项目开发者的 `~/.netrc` 或 git credential helper

### 1.3 打 Tag（推荐）

业务项目应锁定 Plugin 版本，避免主干变更影响：

```bash
git tag -a v1.0.0 -m "Eagle Cloud Plugin 1.0.0"
git push gitlab v1.0.0
```

### 1.4 业务项目接入

`.claude/settings.json`：

```json
{
  "marketplaces": {
    "eagle-cloud-internal": {
      "type": "git",
      "url": "git@gitlab.your-domain.com:eagle/eagle-cloud.git",
      "path": "claude-plugin",
      "ref": "v1.0.0"
    }
  },
  "enabledPlugins": {
    "eagle-cloud@eagle-cloud-internal": true
  }
}
```

升级时改 `ref: "v1.1.0"` 即可。

## 选项 2：GitHub Enterprise / GitHub.com（私有仓库）

### 2.1 推送

```bash
gh repo create eagle/eagle-cloud --private
git remote add origin git@github.com:eagle/eagle-cloud.git
git push origin main
git push origin v1.0.0
```

### 2.2 业务项目接入

```json
{
  "marketplaces": {
    "eagle-cloud-internal": {
      "type": "git",
      "url": "git@github.com:eagle/eagle-cloud.git",
      "path": "claude-plugin",
      "ref": "v1.0.0"
    }
  },
  "enabledPlugins": {
    "eagle-cloud@eagle-cloud-internal": true
  }
}
```

## 选项 3：Gitea（自托管 + 轻量）

完全等价于 GitLab，仅 URL 不同：

```json
{
  "marketplaces": {
    "eagle-cloud-internal": {
      "type": "git",
      "url": "git@gitea.your-domain.com:eagle/eagle-cloud.git",
      "path": "claude-plugin",
      "ref": "v1.0.0"
    }
  }
}
```

## 选项 4：Gitee（国内推荐，访问速度快）

Gitee（码云）是国内最常用的 Git 服务之一，免费私有仓库 + 国内 CDN 访问稳定。

### 4.1 推送

```bash
# 在 Gitee 创建仓库：eagle/eagle-cloud（私有 / 企业版）
# https://gitee.com/projects/new

cd /path/to/eagle-cloud
git remote add gitee git@gitee.com:eagle/eagle-cloud.git

# 首次推送（含 tag）
git push gitee main
git push gitee --tags
```

### 4.2 配置访问权限

#### 个人账户

- Gitee → 个人设置 → **私人令牌（Personal Access Token）**
- 创建 token，勾选 `projects` 范围（只读权限即可）
- 业务项目开发者本机配置 SSH Key 或 HTTPS + token

#### 企业账户（推荐团队级）

- Gitee 企业版 → 项目 → **成员管理**
- 邀请团队成员（最低 `观察者` 角色，可读即可）
- 启用 **企业 SSO** 后，开发者本地 git 凭证由 SSO 管理

### 4.3 业务项目接入

`.claude/settings.json`：

```json
{
  "marketplaces": {
    "eagle-cloud-internal": {
      "type": "git",
      "url": "git@gitee.com:eagle/eagle-cloud.git",
      "path": "claude-plugin",
      "ref": "v1.0.0"
    }
  },
  "enabledPlugins": {
    "eagle-cloud@eagle-cloud-internal": true
  }
}
```

或 HTTPS 形式：

```json
{
  "url": "https://gitee.com/eagle/eagle-cloud.git"
}
```

### 4.4 Gitee CI 选项

**选项 A — Gitee Workflow**（兼容 GitHub Actions 子集，推荐）

把 `.github/workflows/plugin-sync-check.yml` 复制一份到 `.gitee/workflows/plugin-sync-check.yml`：

```bash
mkdir -p .gitee/workflows
cp .github/workflows/plugin-sync-check.yml .gitee/workflows/
```

Gitee Workflow 默认就支持 actions/checkout 等标准 action，无需修改。

**选项 B — Gitee Go**

适合需要更复杂 pipeline 的企业场景：

`.gitee-go.yaml`：

```yaml
name: plugin-sync-check
trigger:
  push:
    branches: [main]
  pull_request:
    branches: [main]

stages:
  - validate

validate:
  stage: validate
  image: alpine:3.20
  steps:
    - apk add --no-cache bash git jq findutils
    - chmod +x ./claude-plugin/sync.sh
    - ./claude-plugin/sync.sh
    - |
      if ! git diff --quiet claude-plugin/; then
        echo "❌ claude-plugin/ is OUT OF SYNC"
        git diff claude-plugin/
        exit 1
      fi
    - jq -e '.name and .version' claude-plugin/plugin.json
    - jq -e '.plugins[0].version' claude-plugin/marketplace.json
```

**选项 C — Webhook + 自建 Jenkins / Drone**

Gitee 支持 webhook，可触发自建 CI 系统跑同步校验。配置路径：项目 → **管理** → **WebHooks**。

### 4.5 Gitee Pages 文档站点（可选）

如果想为 Plugin 文档建独立站点，可启用 Gitee Pages：

- 仓库 → **服务** → **Gitee Pages** → 启用
- 选 `claude-plugin/` 目录或建立独立 `docs/` 分支
- 部署后访问：`https://eagle.gitee.io/eagle-cloud/`

### 4.6 与 GitHub/GitLab 的差异

| 特性 | Gitee | GitHub | GitLab |
|------|-------|--------|--------|
| 国内访问速度 | ✅ 优 | ⚠️ 偶尔慢 | ⚠️ 自建可优化 |
| 免费私有仓库 | ✅（5 人内）| ✅ 个人 | ✅（自建） |
| Tag 引用 | ✅ | ✅ | ✅ |
| Webhook | ✅ | ✅ | ✅ |
| CI 配置文件 | `.gitee/workflows/*.yml` 或 `.gitee-go.yaml` | `.github/workflows/*.yml` | `.gitlab-ci.yml` |
| Marketplace `type` | `git` | `git` | `git` |

**Plugin 内容本身完全相同**，只是 Git 服务的 URL 与 CI 配置文件路径不同。

## 选项 5：仅同步 plugin 子目录到独立仓库（高阶）

如果希望业务项目只 clone plugin 而非整个 eagle-cloud 仓库：

### 4.1 创建独立 plugin 仓库

```bash
# 用 git subtree 抽取 claude-plugin/ 到新仓库
cd /path/to/eagle-cloud
git subtree split --prefix=claude-plugin -b plugin-only
git push gitlab plugin-only:main \
    --remote-name=eagle-cloud-plugin \
    --url=git@gitlab.your-domain.com:eagle/eagle-cloud-plugin.git
```

之后 eagle-cloud 主仓库的 plugin 变更需通过 CI 同步到独立仓库。

### 4.2 业务项目接入

```json
{
  "marketplaces": {
    "eagle-cloud-internal": {
      "type": "git",
      "url": "git@gitlab.your-domain.com:eagle/eagle-cloud-plugin.git",
      "path": ".",
      "ref": "v1.0.0"
    }
  }
}
```

**优点**：clone 体积小、权限隔离（业务项目无需访问主仓库）
**缺点**：维护双仓库同步 pipeline

## 自动同步 Pipeline（选项 4 配套）

`.github/workflows/sync-plugin-repo.yml`：

```yaml
name: Sync plugin to standalone repo

on:
  push:
    branches: [main]
    paths: ['claude-plugin/**']
  push:
    tags: ['v*']

jobs:
  sync:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with: { fetch-depth: 0 }

      - name: Push subtree to plugin repo
        env:
          PLUGIN_REPO: git@gitlab.your-domain.com:eagle/eagle-cloud-plugin.git
          DEPLOY_KEY: ${{ secrets.PLUGIN_REPO_DEPLOY_KEY }}
        run: |
          mkdir -p ~/.ssh
          echo "$DEPLOY_KEY" > ~/.ssh/id_ed25519
          chmod 600 ~/.ssh/id_ed25519
          ssh-keyscan gitlab.your-domain.com >> ~/.ssh/known_hosts

          git subtree push --prefix=claude-plugin "$PLUGIN_REPO" main
          git push "$PLUGIN_REPO" --tags
```

## 团队推广步骤

1. **小范围试点**（1 周）
   - 选 1-2 个新业务项目接入，按 `INTEGRATION-TEST.md` 全流程验证
   - 收集开发者反馈
   - 修复发现的 USAGE/规则偏差，发版 1.0.x

2. **公司内宣讲**（30 分钟）
   - 演示一个典型业务流程：用 `/new-aggregate` 起项目 + AI 自动用正确 API 写代码
   - 强调 5 个高频陷阱（参见 `INTEGRATION-TEST.md` 阶段 2）
   - 讲解版本升级机制

3. **存量项目接入**
   - 旧项目按需接入（无强制要求）
   - 修复 PR 时让 AI 加载 plugin 检查规范

4. **形成闭环**
   - PR 模板加一行："本次变更是否需要更新 Plugin？"
   - 季度 review 一次 USAGE 准确性

## CI/CD 集成（业务项目侧）

业务项目 CI 中可加一个 sanity check：

```yaml
# .github/workflows/plugin-loaded.yml
- name: Verify plugin loaded
  run: |
    test -f .claude/settings.json
    jq -e '.enabledPlugins["eagle-cloud@eagle-cloud-internal"]' .claude/settings.json
```

确保团队成员未误改配置。

## 升级流程

### Plugin 侧（eagle-cloud 维护者）

1. 修改源（`.claude/rules/` 或 `eagle-starter/*/USAGE.md`）
2. `./claude-plugin/sync.sh`
3. 更新版本：
   - `claude-plugin/plugin.json` 中的 `version`
   - `claude-plugin/marketplace.json` 中的 `version`
   - `claude-plugin/CHANGELOG.md` 添加新条目
4. `git commit && git tag v1.x.0 && git push --tags`
5. CI 自动校验通过后，通知业务项目

### 业务项目侧

1. 收到通知后，修改 `.claude/settings.json` 的 `ref`：
   ```json
   "ref": "v1.0.0"  →  "ref": "v1.1.0"
   ```
2. 重启 Claude Code 会话（自动拉取新版）
3. 按 `INTEGRATION-TEST.md` 阶段 1 + 阶段 2 验证关键 API

## 监控与反馈

### 监控

- **Plugin 仓库 Issues**：业务项目反馈 USAGE 错漏 / 规则建议
- **CI 运行情况**：`plugin-sync-check.yml` 是否经常失败 → 说明开发者忘记 sync

### 度量指标

| 指标 | 目标 |
|------|------|
| 接入项目数 | 半年内 ≥ 10 个 |
| Plugin 升级延迟 | 业务项目升到最新 minor 版本 ≤ 30 天 |
| API 编造率 | 接入后 AI 生成代码中编造的 starter API 数 / 总数 ≤ 5% |
| Issue 关闭速度 | USAGE 错漏 ≤ 3 天修复发布 |

## 安全考虑

- **Plugin 内容是声明式文档**，不含可执行业务代码 → 安全风险极低
- **认证**：使用 SSH Key 或 Deploy Token，不要硬编码 password
- **审计**：plugin 仓库 push 应限制为维护者团队（GitLab Protected Branch / GitHub Branch Protection）
- **隔离**：plugin 仓库与业务代码仓库分离权限（业务团队读，维护团队写）

## 常见问题

**Q1: 业务项目 CI 失败，因为拉不到 plugin 仓库？**
A: 配置 deploy key 或 service account；CI runner 需有访问 plugin 仓库的权限。

**Q2: 团队不同分支在用不同 plugin 版本会冲突吗？**
A: 不会。Plugin 是只读文档，每个 Claude Code 会话独立加载。

**Q3: 离线开发能用吗？**
A: 首次启动时会拉取并缓存到本地（`~/.claude/marketplaces/`）。之后离线可用，直到 ref 变更需要重拉。

**Q4: 业务项目 CLAUDE.md 与 plugin CLAUDE.md 冲突时怎么办？**
A: 业务项目 CLAUDE.md 优先级更高（项目级 > plugin）。Plugin 只补充约定，不覆盖业务自定义。

**Q5: 如何快速测试 plugin 改动而不发版？**
A: 业务项目临时改用 local marketplace（`README.md` 方式 B），指向本地 plugin 目录。
