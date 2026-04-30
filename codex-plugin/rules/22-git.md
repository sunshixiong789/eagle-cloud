# Git 工作流规范

## 分支模型（Trunk-based + Release）

```
main                  # 主干：所有变更最终汇入，永远可发布
├── feature/{name}    # 功能分支（短期，1–3 天）
├── fix/{name}        # Bug 修复分支
├── hotfix/{name}     # 生产紧急修复（从 release/ 分出）
└── release/v{ver}    # 发布分支（按版本切，仅修复）
```

- **禁止**长期 `develop` 分支（合并冲突灾难）
- 功能分支**生命周期 ≤ 3 天**，超期必须合并或拆分
- `main` 受保护：必须 PR + Review + CI 绿才允许合入

## 分支命名

| 类型 | 命名                        | 示例                                |
|----|---------------------------|-----------------------------------|
| 功能 | `feature/{ticket}-{slug}` | `feature/EAGLE-123-add-payment`   |
| 修复 | `fix/{ticket}-{slug}`     | `fix/EAGLE-456-order-status-race` |
| 热修 | `hotfix/{ticket}-{slug}`  | `hotfix/EAGLE-789-token-expire`   |
| 发布 | `release/v{semver}`       | `release/v1.4.0`                  |
| 重构 | `refactor/{slug}`         | `refactor/extract-payment-port`   |

- 全小写、kebab-case
- 关联工单号（无工单号不应有分支）
- **禁止** `dev` / `tmp` / `wip` / 拼音 / 个人名

## Commit Message（Conventional Commits）

格式：

```
<type>(<scope>): <subject>

<body>

<footer>
```

**type 取值：**

| type       | 说明          |
|------------|-------------|
| `feat`     | 新功能         |
| `fix`      | Bug 修复      |
| `refactor` | 重构（不改外部行为）  |
| `perf`     | 性能优化        |
| `docs`     | 文档          |
| `test`     | 测试          |
| `build`    | 构建系统 / 依赖   |
| `ci`       | CI 配置       |
| `chore`    | 杂项（无业务逻辑变更） |
| `revert`   | 回滚提交        |

**scope**：模块/功能名（小写）。常用：`auth / base / order / payment / starter / build / docs`。

**subject**：祈使句、首字母小写、≤ 72 字符、结尾不加句号。

```bash
# ✅ 示例
feat(auth): add SMS login flow with rate limiting
fix(order): correct status transition for refund-then-cancel
refactor(starter): extract eagle-bom platform module
perf(base): replace N+1 query with @EntityGraph in user list
docs(rules): add caching and messaging guidelines
build(deps): bump spring-boot to 4.0.4
ci: enable modulith verification gate

# ❌ 反例
update                                  # 无 type / scope / 主题
fix bug                                 # 主题无信息
feat: 用户登录                            # 不要 type 没有 scope（除非全局）
feat(auth): Added SMS login.            # 过去时 + 句号
```

**body**（可选）：换行后的详细说明，70 字符换行。说明"为什么"，不重复"做了什么"。

**footer**（可选）：

```
BREAKING CHANGE: 变更内容
Closes #123, #456
Refs EAGLE-789
```

## Commit 颗粒度

- **一个 commit 做一件事**：原子提交，便于 cherry-pick / revert
- **不要混合**：业务变更 + 格式化 + 重命名应分多 commit
- 每 commit 必须**自包含可编译**（不能"编译失败的中间态"）
- 单 PR commit 数 ≤ 20（更多需 squash / rebase）

```bash
# ✅ 正确：拆分提交
git commit -m "feat(payment): add WeChat Pay adapter"
git commit -m "test(payment): cover WeChat refund edge cases"
git commit -m "docs(payment): document WeChat Pay setup"

# ❌ 错误：一锅炖
git commit -m "feat(payment): WeChat support and bug fixes and refactor"
```

## Pull Request

- **标题**：与首条 commit 一致（`feat(auth): ...`）
- **描述**：使用 `25-review-checklist.md` 中的 PR 模板
- **大小**：建议 < 400 行变更，超过需拆分
- **关联工单**：描述中必填 `Closes EAGLE-123`
- **审批**：至少 1 人 Approve；改动 starter / 架构需 2 人

### Squash vs Merge

| 场景                      | 策略                              |
|-------------------------|---------------------------------|
| 功能分支 → main             | **Squash and merge**（保持主干线性、清晰） |
| Release 分支 → main       | **Merge commit**（保留发布历史）        |
| Hotfix → main + release | **Cherry-pick**                 |

## Rebase vs Merge

**自己的功能分支**：

```bash
# ✅ 推荐：rebase 同步主干（线性历史）
git fetch origin
git rebase origin/main

# 解决冲突后
git rebase --continue
git push --force-with-lease       # 安全的 force push
```

- **禁止** `git push --force` 到共享分支（用 `--force-with-lease`）
- **禁止** rebase 已合并到 main 的 commit
- 共享分支只用 `merge`，不 rebase

## .gitignore 必须忽略

```
# 构建输出
build/
out/
target/
*.class
*.jar
*.war

# IDE
.idea/workspace.xml
.idea/tasks.xml
.idea/usage.statistics.xml
.idea/shelf/
.vscode/
*.iml

# OS
.DS_Store
Thumbs.db

# 凭证（关键！）
*.pem
*.key
*.p12
.env
.env.*
gradle.properties               # 如含 nexus 凭证
src/main/resources/application-local.yml

# 日志
*.log
logs/

# 临时
*.tmp
*.swp
```

## 提交前 hook（推荐）

```bash
# .githooks/pre-commit
./gradlew :spotlessCheck             # 格式
./gradlew :checkstyle                # 风格
git secrets --pre_commit_hook        # 凭证扫描
```

启用：`git config core.hooksPath .githooks`

## 标签（Tag）

发布版本打 tag，遵循 SemVer：

```bash
git tag -a v1.4.0 -m "Release 1.4.0"
git push origin v1.4.0
```

| 版本变化                 | 触发条件       |
|----------------------|------------|
| Major（1.x.x → 2.0.0） | 破坏性 API 变更 |
| Minor（1.4.x → 1.5.0） | 新功能、向后兼容   |
| Patch（1.4.0 → 1.4.1） | Bug 修复     |

## 禁止清单

- 禁止 `git push --force` 到共享分支
- 禁止提交大文件（> 50MB）；用 Git LFS
- 禁止提交编译产物 / IDE 私有文件 / 临时文件
- 禁止提交真实凭证（`.env` / `*.pem` / 加密密钥明文）
- 禁止 commit message 写中文标点（半角统一）
- 禁止合并未通过 CI 的 PR
- 禁止改写已发布的 tag
- 禁止单 commit 跨多个无关模块
