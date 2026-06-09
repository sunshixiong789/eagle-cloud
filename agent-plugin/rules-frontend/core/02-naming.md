# 命名约定 + Slice 词根 + Import 规则（通用）

> 平台特有的路径别名形式（`@/*` vs `@features/*`）见 `platforms/<x>/05-aliases.md`。

## 文件名（通用规则，平台微调）

| 类别 | 风格 | 示例 |
|---|---|---|
| Page / Screen 路由级组件 | PascalCase + `Page` 或 `Screen` 后缀 | `LoginPage.tsx`、`OrderDetailScreen.tsx` |
| Slice 私有组件（`features/entities/widgets/pages` 内） | PascalCase | `ProductCard.tsx`、`AssignRoleModal.tsx` |
| Shared 组件 | Web 用 PascalCase 目录 + `index.tsx`；RN/Taro 用 kebab-case 文件 | Web: `Footer/index.tsx`；RN: `app-image.tsx` |
| Hook | kebab-case + `use-` 前缀（推荐）或 camelCase | `use-catalog.ts`、`useAuthContext.ts` |
| Store | kebab-case + `.store.ts` 后缀 | `user.store.ts`、`<feature>-ui.store.ts` |
| API | kebab-case + `.api.ts` 后缀 | `auth.api.ts`、`catalog.api.ts` |
| Queries（按需拆 keys + queries） | kebab-case + `.queries.ts` / `.keys.ts` | `user.queries.ts`、`user.keys.ts` |
| Schemas | kebab-case + `.schema.ts` | `login.schema.ts` |
| Lib | kebab-case | `pkce.ts`、`token.ts` |
| 类型独立文件 | kebab-case 或 `types.ts` | `types.ts`、`product.types.ts` |
| 测试 | 同源 + `.test.{ts,tsx}` | `auth.api.test.ts` |
| 平台特定（RN） | 后缀 `.ios.tsx` / `.android.tsx` / `.web.ts` / `.native.ts` | `icon-symbol.ios.tsx` |
| CSS-in-JS 配套（Web） | 同名 + `.style.ts` | `LoginPage.style.ts` |

> **为什么 Web 与 RN/Taro 命名风格略有差异？** 历史遗留 + 各自生态约定混用的结果（RN 早期教程偏 kebab-case，Web React 主流 PascalCase）。**项目内一致即可**，不强求跨平台统一。

## 函数 / 类 / 导出

- **Hook**：`useXxx` camelCase，**named export**
- **Zustand store**：`useXxxStore`，named export
- **Component**：PascalCase 函数
  - Page / Screen：**default export**
  - Feature components / shared components：通常 named export
- **API 函数**：camelCase 动词起头：`fetchProducts`、`listBanners`、`getProductDetail`、`createRole`
- **常量**：UPPER_SNAKE_CASE：`BASE_URL`、`DEFAULT_TIMEOUT_MS`
- **类型 / 接口**：PascalCase：`Product`、`OAuth2Token`、`RequestOptions`、`UserProfile`

---

## Slice 词根公约

**同一 slice 的文件共享词根**，使 `grep <slice>` 可看到全部相关代码。文件路径前缀按层级使用 `@features/<词根>/`、`@entities/<词根>/`、`@/features/<词根>/` 等平台别名。

**词根规范**：

- 单数小写连字符（kebab-case），如 `catalog`、`services-market`、`order`、`user-profile`
- 名词或名词短语，不用动词（不用 `checkout-flow`，用 `checkout`）
- 反映业务语义而非 UI 位置（不用 `home-tab`）
- 围绕**业务能力**命名，不围绕技术分层（✅ `billing`、❌ `forms`、❌ `modals`）
- `features` 表示动作 / 流程，`entities` 表示稳定业务对象，`pages` 表示页面编排，`widgets` 表示大块业务 UI
- 同层 slice 平级，不要建嵌套 slice（如 `auth/oauth`）——细分用文件名前缀或 slice 内子目录

**当前项目的词根清单**：见各项目自身的 `.agents/PROJECT.md`。

**何时新建 slice**（业务优先，不为套 FSD 层级而拆）：

- 新页面编排：`pages/<page>`
- 新用户动作 / 业务流程：`features/<feature>`
- 长期稳定业务对象：`entities/<entity>`
- 跨 feature 的大块业务 UI：`widgets/<widget>`
- 无业务归属能力：`shared/`

**何时不要新建**：

- 只是一个私有工具函数或组件 → 放当前 slice 的 `lib/` / `components/`
- 只是跨 slice 复用的基础工具 → 上移 `shared/`

---

## Slice 内 import 规则

**同 slice 内**：用相对路径 `../api/foo`、`./helpers`。
**禁止** `@features/<self>/api/foo`、`@entities/<self>/...` 或 `@/features/<self>/...` 这类绝对路径自引。

**跨 slice 消费**：只能走 public API；具体 barrel 策略见 `05-bounded-context.md`。

## Import 顺序

1. **外部包** —— `react`、`react-native`、`@tanstack/react-query`、第三方库
2. **共享/基础设施** —— `@shared/*`、`@infra/*`、`@app/*`
3. **其他 slice public API** —— `@entities/*`、`@features/*`、`@widgets/*`、`@pages/*`
4. **本 slice 相对路径** —— `../api/...`、`./helpers`

类型专用 import 一律 `import type`（详见 `08-red-lines.md`）。
