# 命名约定 + Feature 词根 + Import 规则（通用）

> 平台特有的路径别名形式（`@/*` vs `@features/*`）见 `platforms/<x>/05-aliases.md`。

## 文件名（通用规则，平台微调）

| 类别 | 风格 | 示例 |
|---|---|---|
| Page / Screen 路由级组件 | PascalCase + `Page` 或 `Screen` 后缀 | `LoginPage.tsx`、`OrderDetailScreen.tsx` |
| Feature 私有组件（`@features/<f>/components/`） | PascalCase | `ProductCard.tsx`、`AssignRoleModal.tsx` |
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

## Feature 词根公约

**同一 feature 的所有文件共享词根**，使 `grep <feature>` 可看到全部相关代码。文件路径前缀统一 `@features/<词根>/` 或 `@/features/<词根>/`（按平台别名风格，见 `platforms/<x>/05-aliases.md`）。

**词根规范**：

- 单数小写连字符（kebab-case），如 `catalog`、`services-market`、`order`、`user-profile`
- 名词或名词短语，不用动词（不用 `checkout-flow`，用 `checkout`）
- 反映 bounded context 而非 UI 位置（不用 `home-tab`、`profile-page`）
- 围绕**业务能力**命名，不围绕技术分层（✅ `billing`、❌ `forms`、❌ `modals`）
- 一个词根对应一个 bounded context（详见 `05-bounded-context.md`）
- Feature 之间平级，不要建嵌套 feature（如 `auth/oauth`）——细分用文件名前缀或 feature 内子目录

**当前项目的词根清单**：见各项目自身的 `.agents/PROJECT.md`。

**何时新建 feature**：

- 出现一组新的、和现有 feature 没有共享 store / API 的页面
- 现有 feature 已 30+ 文件且内部能切出明确独立的业务边界（详见 `09-scaling.md`）

**何时不要新建**：

- 只是新增一个页面、属于已有业务域 → 加到该 feature 的 `pages/` / `screens/`
- 只是一个工具函数或组件 → 放 feature 的 `lib/` / `components/`，跨 feature 复用就上移 `@/shared/`

---

## Feature 内 import 规则

**同 feature 内**：用相对路径 `../api/foo`、`./helpers`。
**禁止** `@features/<self>/api/foo` 或 `@/features/<self>/api/foo`——绝对路径自引会让 feature 名字硬编码进自己内部，重命名痛苦。

**跨 feature 消费**：按平台 barrel 策略（详见 `05-bounded-context.md`）。

## Import 顺序

1. **外部包** —— `react`、`react-native`、`@tanstack/react-query`、第三方库
2. **共享/基础设施** —— `@shared/*`、`@infra/*`、`@providers/*`
3. **其他 feature**（按平台 barrel 规则）—— `@features/<other>` 或 `@features/<other>/...`
4. **本 feature 相对路径** —— `../api/...`、`./helpers`

类型专用 import 一律 `import type`（详见 `08-red-lines.md`）。
