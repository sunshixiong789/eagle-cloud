# AI Agent 工程规范

> 面向 AI 编程助手（Claude Code / Codex / Cursor / Gemini / Copilot）的**通用工程规范**。
> 工程结构、依赖契约、状态边界、样式系统、红线规则、反例与扩展信号都在本文。
>
> 项目层入口文档（`CLAUDE.md` / `AGENTS.md` 之类）只放摘要 + 命令速查 + 项目当前关注点，详细规范一律以本文为准。

**适用范围**：React 单页应用 + Feature-Sliced Design（主轴）+ 角色分层（feature 内 + 全局两层）+ Shared Kernel（`src/shared/`）。
**推荐栈占位**：Vite / React / React Router / antd 或同类 UI 库 / @tanstack/react-query / Zustand / Tailwind / CSS-in-JS（如 antd-style / Emotion）。
**不是** Clean Architecture —— 没有显式 Entities / Use Cases 层，未做完整 Dependency Inversion；中小规模项目这样最划算。

---

## 🚀 30 秒 TL;DR

> AI 写第一行代码前的 10 条速记。详细规则按章节展开。

1. **角色分 6 层**：Route → Page → (Component, Hook, Query) → API → shared/api。**严禁反向依赖**。
2. **Feature 内 grep 即可看全部**：同一 feature 文件共享词根（业务域单数小写连字符）。
3. **不要跨 feature 直接 import 内部**：消费方走 `@/features/<x>`（barrel），跨 feature 复用上移到 `@/shared/`。
4. **路径别名最少化**：建议只保留 `@/*` → `./src/*`。feature 内部用相对路径，不要 `@/features/<self>/...`。
5. **路由集中声明**：路由在 `src/app/router.tsx` 通过 `useRoutes` + `React.lazy` 声明，不用文件式路由。
6. **样式三件套**：原子工具（Tailwind）写布局、CSS-in-JS 写主题感知样式、CSS Modules 写遗留组件样式；不在同一行同时用 className + `style={{ color }}`。
7. **状态三分**：服务端 → React Query；当前组件 → `useState`；跨页/持久化 → Zustand + persist。**表单 state 不进 Zustand**。
8. **基础设施单例**：HTTP 客户端（Axios / fetch wrapper）只有一个；token 通过启动期注入到 HTTP 客户端，不让 `shared/` 反向 import feature。
9. **包管理用 lock 文件锁定**（yarn / pnpm / npm 选一种，不混用）；commit message 用 conventional 前缀；**bug fix 必写 root cause**。
10. **机器校验**：lint 任务串接格式器 + feature 边界检查 + tsc，违规拦下。详见 §13。
11. **不确定 → 读对应章节**，不要凭直觉。

---

## 1. 六角色与依赖矩阵

| 角色 | 职责 | 可依赖 | **不可依赖** |
|---|---|---|---|
| **Route** | `src/app/router.tsx` 中声明：lazy import + 嵌套 + Guard | Page（仅通过 `React.lazy(() => import(...))`） | Component、Hook、Query、Store、API |
| **Page** | 编排 Hook / Query + 拼装 Component；处理 loading / error / empty | Component、Hook、Query、Store selector | 直接调 API、跨 feature 直接 import 内部 |
| **Component** | 受 props 驱动的展示 + 受控交互 | 其他 Component、`@/shared/constants/`、design token | Hook（含 `useQuery` / `useMutation`）、API、Store |
| **Hook / Query** | 数据编排（React Query / Zustand 包装）+ 派生计算 | API、Store、其他 Hook | 渲染 JSX |
| **API** | HTTP / WebSocket 调用，DTO 类型定义 | `@/shared/api/http`、`@/shared/constants/`、纯函数 | React、其他 feature 内部 |
| **Provider** | App 启动 wiring（QueryClientProvider、ThemeProvider、AuthProvider 等编排） | 任意层（composition root） | — |

**依赖方向（严禁反向）**：

```
Route ─→ Page ─┬─→ Component
               ├─→ Hook ─┬─→ API ──→ shared/api/http
               └─→ Query └─→ Store

Provider ───────────────────────────→ (composition root, 任意)
```

**跨层快速校验**：

- Component 顶部不应出现 `useQuery` / `useMutation` —— 上移到 Hook/Query 或 Page。
- API 文件不应出现 `import { useXxx } from 'react'` 或 JSX。
- `shared/api/http.ts` 不应直接 import 任何 store / router / feature 内部模块。需要 token 等 feature 提供的能力时，让 feature 在启动期把 getter 注入到 http 客户端。
- Store 不应 import 业务 API（infrastructure-level 工具允许）。

---

## 2. 目录布局

✅ 已就位、🟡 预留命名（满足触发条件再启用）：

```
<repo>/
├── src/
│   ├── app/                          # ✅ 应用外壳（非文件式路由）
│   │   ├── router.tsx                # ✅ useRoutes 集中声明
│   │   ├── layouts/                  # ✅ BasicLayout / BlankLayout / menuData
│   │   └── config/                   # ✅ 应用配置（API base、业务常量、主题默认 token）
│   │
│   ├── features/                     # ✅ 业务模块（见 §4 划分原则）
│   │   └── <feature>/
│   │       ├── api/                  # ✅ API 调用 + DTO 类型 (+ .test.ts)
│   │       ├── components/           # ✅ feature 内部组件（含 Provider 与 Guard）
│   │       ├── pages/                # ✅ 路由级组件，*Page.tsx + *Page.style.ts
│   │       ├── types.ts              # ✅ feature 私有类型（**禁止** 并建 types/ 目录）
│   │       ├── index.ts              # ✅ 对外公开 API barrel
│   │       ├── queries/              # 🟡 按需：React Query hooks + keys
│   │       ├── hooks/                # 🟡 按需：feature 私有 React hooks
│   │       ├── stores/               # 🟡 按需：feature 私有 Zustand store
│   │       ├── schemas/              # 🟡 按需：zod / 校验 schema
│   │       └── lib/                  # 🟡 按需：feature 私有非 React 工具
│   │
│   ├── shared/                       # ✅ Shared Kernel：跨 feature 通用
│   │   ├── api/
│   │   │   ├── http.ts               # ✅ HTTP 客户端单例（Axios / fetch wrapper）
│   │   │   ├── interceptors.ts       # ✅ 请求/响应拦截器
│   │   │   ├── error.ts              # ✅ 错误归一化、401 处理
│   │   │   └── types.ts              # ✅ 通用响应包结构
│   │   ├── stores/                   # ✅ 跨 feature 全局 store（如用户、主题、全局 modal 队列）
│   │   ├── components/               # ✅ 跨 feature 复用组件（不造 UI 库已有原子）
│   │   ├── hooks/                    # ✅ 通用 React hooks
│   │   ├── constants/                # ✅ 业务常量
│   │   ├── utils/                    # 🟡 按需：通用纯函数
│   │   └── types.d.ts                # ✅ 全局 ambient declarations（图片/CSS 模块、构建注入常量）
│   │
│   ├── providers/                    # ✅ Composition Root
│   │   ├── AppProvider.tsx           # ✅ 顶层编排
│   │   ├── QueryProvider.tsx         # ✅ React Query Provider（含 QueryClient 实例）
│   │   └── ThemeProvider.tsx         # ✅ 主题 / locale Provider
│   │
│   ├── styles/                       # ✅ 全局样式（仅 @font-face + 基础 reset + 原子工具入口）
│   ├── assets/                       # ✅ 图片 / 字体 / 图标
│   └── main.tsx                      # ✅ 入口
│
├── scripts/
│   └── check-feature-boundaries.mjs  # ✅ feature 边界检查（挂在 lint）
├── docs/                             # 🟡 按需：specs / 设计说明
├── tests/ or `*.test.ts(x)`          # 🟡 测试与源码同目录共存
├── vite.config.ts                    # ✅ 构建 + dev proxy + 别名
├── tsconfig.json                     # ✅ 路径别名定义
├── biome.json / eslint.config.*      # ✅ lint / format
└── package.json
```

**Feature 内部布局原则**：子目录按需建，不强制全有。但**已建的子目录必须遵循约定形状**，禁止新增非约定子目录（如 `context/`、`guard/`、`utils/` 这种）。需要新增子目录约定，**先在本文登记位置规则再建**。

---

## 3. 命名约定

### 文件名

| 类别 | 风格 | 示例 |
|---|---|---|
| 路由级组件（pages/） | PascalCase + `Page` 后缀 | `LoginPage.tsx`、`UserListPage.tsx` |
| CSS-in-JS 配套样式 | 同名 + `.style.ts` | `LoginPage.style.ts` |
| Feature 私有组件（components/） | PascalCase | `AssignRoleModal.tsx`、`ProductCard.tsx` |
| Shared 组件（shared/components/） | PascalCase 目录 + index.tsx | `Footer/index.tsx`、`RightContent/index.tsx` |
| Hook（hooks/） | kebab-case + `use-` 前缀 或 camelCase | `use-auth-context.ts`、`useWsNotifications.ts` |
| Store（stores/） | kebab-case + `.store.ts` 后缀 | `app.store.ts`、`theme.store.ts` |
| API（api/） | kebab-case + `.api.ts` 后缀 | `auth.api.ts`、`role.api.ts` |
| Queries（queries/） | kebab-case + `.queries.ts` / `.keys.ts` | `user.queries.ts`、`user.keys.ts` |
| Schemas（schemas/） | kebab-case + `.schema.ts` | `login.schema.ts` |
| Lib（lib/） | kebab-case | `pkce.ts`、`token.ts` |
| 类型 | `types.ts` 单文件 | （**不建** types/ 目录） |
| 测试 | 同源 + `.test.ts(x)` | `auth.api.test.ts` |

### 函数 / 类 / 导出

- **Hook**：`useXxx` camelCase，named export
- **Zustand store**：`useXxxStore` named export
- **Page 组件**：PascalCase，**default export**
- **Feature 组件**：PascalCase，named export
- **API 函数**：camelCase 动词起头，如 `fetchUsers`、`createRole`
- **常量**：UPPER_SNAKE_CASE，如 `BASE_URL`、`DEFAULT_TIMEOUT_MS`
- **类型 / 接口**：PascalCase，如 `UserProfile`、`ApiResponse`

### Feature 内 import 规则

- **同 feature 内：用相对路径** `../api/foo`、`./helpers`。**禁止** `@/features/<self>/api/foo`。
- **跨 feature 消费：走 barrel** `@/features/<other>`。**禁止** `@/features/<other>/api/foo` 等深路径。
- 该规则由 `scripts/check-feature-boundaries.mjs` 强制（见 §13）。

---

## 4. Feature 划分原则

> Feature 是业务域的最小独立单元。同一 feature 的所有文件共享词根，`grep <feature>` 即可看到全部相关代码。

**怎么选词根**：

- 单数、小写、连字符（kebab-case）。例：`auth`、`order`、`user-profile`。
- 围绕**业务能力**命名，不要围绕技术分层。✅ `billing`、❌ `forms`、❌ `modals`。
- 一个词根对应一个 bounded context（见 §9）。两个 feature 频繁穿插内部依赖说明它们本来是同一个 context，应当合并。
- Feature 之间是平级的，不要建嵌套 feature（如 `auth/oauth`）。需要细分时用文件名前缀（`oauth-callback.api.ts`）或在 feature 内分子目录。

**何时新建 feature**：

- 出现一组新的、和现有 feature 没有共享 store / API 的页面。
- 现有 feature 已 30+ 文件且内部能切出明确独立的业务边界（见 §12）。

**何时不要新建**：

- 只是新增一个页面、属于已有业务域 → 加到该 feature 的 `pages/`。
- 只是一个工具函数或组件 → 放 feature 的 `lib/` / `components/`，跨 feature 复用就上移 `@/shared/`。

**项目层登记**：每个项目在自己的入口文档（`CLAUDE.md` / `AGENTS.md`）里维护一份当前 feature 词根表，本文不维护具体业务词根。

---

## 5. 新增 feature 操作清单

> 新增 feature `<X>` 的标准流程。每步出 commit-able 单元。

**步骤 1：定词根** —— 按 §4 原则选定单数小写连字符词根，在项目入口文档登记。

**步骤 2：建目录**

```bash
mkdir -p src/features/<X>/{api,pages,components}
# 按需建：queries/ hooks/ stores/ schemas/ lib/（**不要预先建空目录**）
touch src/features/<X>/index.ts src/features/<X>/types.ts
```

**步骤 3：写 API（贴后端字段）**

```ts
// src/features/<X>/api/<X>.api.ts
import { http } from "@/shared/api/http";

export interface XResponse {
  id: number;
  created_at: string;        // 允许 snake_case
}

export async function fetchX(id: string): Promise<XResponse> {
  return http.get<XResponse>(`/x/${id}`).then((r) => r.data);
}
```

**步骤 4：写 Query（数据编排 + ViewModel 映射）**

```ts
// src/features/<X>/queries/<X>.keys.ts
export const xKeys = {
  all: ["x"] as const,
  detail: (id: string) => [...xKeys.all, "detail", id] as const,
};

// src/features/<X>/queries/<X>.queries.ts
import { useQuery } from "@tanstack/react-query";
import { fetchX } from "../api/<X>.api";
import { xKeys } from "./<X>.keys";

export interface XViewModel {
  id: number;
  createdAt: string;
}

export function useXQuery(id: string) {
  return useQuery({
    queryKey: xKeys.detail(id),
    queryFn: () => fetchX(id),
    select: (dto): XViewModel => ({ id: dto.id, createdAt: dto.created_at }),
  });
}
```

**步骤 5：写 Page**

```tsx
// src/features/<X>/pages/<X>Page.tsx
import { Spin, Alert, Empty } from "antd";
import { useXQuery } from "../queries/<X>.queries";

export default function XPage() {
  const { data, isPending, error } = useXQuery("123");

  if (isPending) return <Spin />;
  if (error) return <Alert type="error" message={String(error)} />;
  if (!data) return <Empty />;

  return <div>{data.createdAt}</div>;
}
```

**步骤 6：在 `index.ts` 公开 API**

```ts
// src/features/<X>/index.ts
export { useXQuery } from "./queries/<X>.queries";
export type { XViewModel } from "./queries/<X>.queries";
// pages 不必从 barrel 导出 —— router.tsx 直接 lazy import 路径
```

**步骤 7：注册路由**

```tsx
// src/app/router.tsx
const X = React.lazy(() => import("@/features/<X>/pages/<X>Page"));
// ...
{ path: "x", element: <X /> },
```

**步骤 8：加菜单**

```ts
// src/app/layouts/menuData.ts
{ path: "/x", name: "X 模块", icon: "..." }
```

**步骤 9：质量检查**

```bash
<lint script>              # 格式器 + boundaries + tsc
<test script>              # 如有测试
```

**步骤 10：提交**

```
feat(<X>): 新增 <X> 模块骨架

- pages/<X>Page.tsx 编排 useXQuery
- api/<X>.api.ts 暴露 fetchX
- queries/<X>.queries.ts 包 useQuery + DTO → ViewModel 映射
- router.tsx 注册路由 + menuData 加菜单
```

---

## 6. 类型三层

数据从后端到屏幕走三种形态，每层禁止"越级穿透"：

| 层 | 位置 | 形态 | 命名 |
|---|---|---|---|
| **DTO** | `features/<f>/api/*.api.ts` 顶部 | 贴后端原样 | 允许 `snake_case`、历史字段名 |
| **View Model** | `features/<f>/queries/*.queries.ts` 的 `select` / `transform` | 剔除冗余、扁平化 | `camelCase`、按 UI 需要展开 |
| **Component Props** | `features/<f>/components/<X>.tsx` 顶部 interface | 最小必要 | 只接收原子值，不传整段 DTO |

**铁律**：DTO 字段名（如 `created_at`、`user_role_id`）**不允许**出现在 Component props 类型或 JSX 表达式里。出现即说明 query 没做映射。

---

## 7. 状态三分决策树

```
有数据吗？
├─ 来自服务端（API / WebSocket）→ React Query
└─ 来自客户端
   ├─ 跨页面 / 跨重启需要保留 → Zustand + persist
   └─ 仅当前组件需要         → useState
```

**反例**：
- 表单输入用 Zustand（应当 `useState`，组件卸载即释放）
- 列表筛选只放本地 `useState`，返回再进丢状态（跨页面 → Zustand 或 URL 参数）
- 服务端数据进 Zustand（重复缓存，丢 React Query invalidate 能力）

**React Query 默认配置基线**（在 `providers/QueryProvider.tsx` 一次性设好）：
- `staleTime: 5min`、`refetchOnWindowFocus: false`、`retry: 1`
- mutation 不重试
- query key 用元组：`['users', filters]`

### 跨 feature invalidation 模式

A 的 mutation 完成后让 B 的 query 失效，**invalidate 操作在 Page 编排，不在 hook 内**：

❌ **错误**：A 的 hook 内 `queryClient.invalidateQueries(['<B>', ...])` —— hook 知道了 B 的 query key，违反 bounded context

✅ **正确**：

```tsx
// XPage.tsx
const queryClient = useQueryClient();
const mutate = useXMutation();

const onSubmit = async (form) => {
  await mutate.mutateAsync(form);
  queryClient.invalidateQueries({ queryKey: yKeys.all });
};
```

Page 是编排者，可以同时知道 A 与 B 的 query key。

---

## 8. Cross-cutting 模式

| 关注点 | 归属层 | 范式 |
|---|---|---|
| **错误** | API throw → Query 透传 → Page 捕获 → UI（message / Modal / inline） | Component 不写 `try/catch`，假设数据就绪 |
| **加载** | `query.isPending` 在 Page 内决定 Skeleton / Spin | Component 默认渲染非加载态 |
| **空态** | Page 处理 `data?.length === 0`，渲染 `Empty` | 不下沉到 Component |
| **401 / 登出** | `shared/api/error.ts` 统一处理，清 token + 跳转登录页 | Page / Query 不重写 |
| **Token 刷新** | `shared/api/interceptors.ts` 检测 401 + refresh，自动重试一次 | 业务调用方完全透明 |
| **主题切换** | `providers/ThemeProvider.tsx` + `shared/stores/theme.store.ts`；UI 库 `ConfigProvider` 配 token | Component 不直接读 mode |
| **副作用** | mount/unmount 一次性副作用放 Page 的 `useEffect` | 可复用的副作用抽 `hooks/use-*.ts` |

---

## 9. Bounded Context

**每个 feature 的内部模块（api / queries / stores / lib / hooks / components）禁止被其他 feature 直接 import**。

跨 feature 数据流动两条合法路径：

1. **上移**：把需要被多 feature 共享的能力放到 `@/shared/`
   - 通用 HTTP / token / 错误 → `@/shared/api/`
   - 通用 hook → `@/shared/hooks/`
   - 通用 store（user / theme / modal） → `@/shared/stores/`
   - 通用展示组件 → `@/shared/components/`
2. **Page 编排**：A 的 Page 同时调 A 的 query + B 的 query（UI 层负责编排，query 之间不互相依赖）。跨 feature invalidation 也走 Page，详见 §7。

**唯一允许的跨 feature import**：通过 barrel `@/features/<x>`（即 `index.ts` 公开的内容）。

**反例**：`@/features/dashboard/queries/...` 里 `import { foo } from '@/features/system/api/...'` —— 应当走"上移"或"Page 编排"。

**特例（合并 vs 拆分）**：当两个 feature 强耦合到无法用上述化解，说明它们本来就是同一个 context，应当合并。

---

## 10. 反例速查

依次扫一遍，命中即重构：

1. Component 内出现 `useQuery` / `useMutation` → 上提到 Page 或包成 `queries/use-*`
2. API 文件出现 `import { useXxx } from 'react'` 或 JSX
3. JSX 里出现 `data.created_at` / `data.user_role_id` 这类 DTO 原名 → Query 缺映射
4. 一个 Query 文件返回 10+ 字段 → 拆 `useXxxQuery` + `useXxxMutation` + `useXxxDerived`
5. 同一 feature 的代码分散在多目录但词根不一致 → 重命名对齐
6. Store action 内有 fetch → 移到 query 的 `useMutation.onSuccess` 触发 store
7. `shared/api/http.ts` 顶部出现 `import { useXxxStore } from '@/shared/stores/...'` 或 `import ... from '@/features/...'` → 走依赖反转
8. feature 内部用 `@/features/<self>/...` 绝对路径 → 改相对路径（边界检查会拦）
9. 跨 feature 用 `@/features/<other>/api/...` 深路径 → 改走 barrel（边界检查会拦）
10. Query 内 `queryClient.invalidateQueries(['<其他 feature>', ...])` → 移到 Page
11. 用 `import { Foo }` 引入只作为类型的导出 → 改 `import type { Foo }`
12. 在 feature 里新建 `context/` / `guard/` / `utils/` 这种非约定子目录 → 用 `components/` / `lib/` 容纳

---

## 11. 红线规则

### 11.1 样式三件套

- **原子工具类（Tailwind 等）**：写**布局类** —— flex / grid / spacing / sizing。
  *理由*：原子化、无需切样式上下文。

- **CSS-in-JS（antd-style / Emotion / styled-components）**：写**主题感知**样式（需要 token 的色 / 阴影 / 圆角等）。
  *位置*：和页面/组件同目录 `<Name>Page.style.ts` / `<Name>.style.ts`。

- **CSS Modules** (`*.module.less` / `*.module.css`)：写**遗留组件级**样式。新代码尽量避免，优先用前两者。

- **`src/styles/global.*`** 只放 `@font-face` 与基础 reset，**禁止**写业务样式。

- **不要双写**：同一元素的同一样式不要同时用 className + `style={{ }}`。
  *允许 `style={{ }}`*：动态计算的尺寸 / 颜色（来自 props、state、Dimensions 等）。

### 11.2 TypeScript

- **`tsconfig` `strict: true`**；新增代码**不允许 `any`**。需要"任意"语义用 `unknown` + 类型守卫。

- **类型专用 import 必须 `import type`**：
  ```ts
  import type { Foo } from "...";
  import { foo, type Foo } from "...";  // 混合时
  ```
  *理由*：编译期完全擦除，零运行时副作用，避免循环依赖与 tree-shaking 问题。

- **不为类型而类型**：函数返回类型与内部变量能推断就不显式标注。React 组件 props 必须显式 interface。

- **Props interface 默认不 `export`**。只有真正外部消费才 export。

- **不写 JSDoc 大段块注释**；类型即文档。需要说明"为什么"时用 1 行 `//` 注释。

### 11.3 状态管理

- **三类状态分别用三种工具**（见 §7）。

- **Zustand 用 selector 模式**：`useStore((s) => s.x)`，不要 `const store = useStore()` 全量订阅。
  *理由*：全量订阅 = 任何字段变化都 re-render，性能黑洞。

- **Zustand persist store 必须有 `_hasHydrated` 标记**，UI 在未 hydrated 前展示 fallback。
  *理由*：localStorage 异步加载，否则首帧用默认值会闪烁。

- **表单 state 不进 Zustand**。临时输入、modal 草稿、列表筛选这类用 `useState` 或表单库。

- **React Query key 用元组**：`['users', filters]`、`['order', id]`。或更推荐：集中在 `queries/<f>.keys.ts`。

- **不用 React Context 管业务 state**：跨页持久用 Zustand，组件树上下文（如 ThemeProvider、AuthProvider）才用 Context。

### 11.4 HTTP / 认证

- **HTTP 客户端只有一个实例**，放在 `shared/api/http.ts`。禁止另起。

- **Token 存取走专门的 lib（如 `features/auth/lib/token`）**，不要散落在各处直接读 `localStorage`。

- **`shared/api/` 禁止反向 import `features/`**。需要 token 等 feature 提供的能力时，让 feature 在启动期通过 setter 注入到 http 客户端（如 `setTokenGetter(() => ...)`）。

### 11.5 Bug 修复流程

- **先找根因，禁止盲修症状**。能用一句话描述"哪个变量在哪个 effect 被错误赋值"才开始动代码。

- **一次改一个变量**。多个可疑点不要一起改；改一处验一处。

- **三次失败必停**。同一 bug 第 3 次尝试失败后，**禁止**第 4 次盲修，停下来质疑：假设错？架构错？复现条件理解错？

- **Bug 修复 commit 只动相关代码**。不要顺手重构、不要顺手改格式、不要顺手 rename 无关变量。

- **commit message 必须写清 root cause**，不要只说"修复 xx 问题"。模板：

  ```
  fix(auth): refreshAccessToken 在非安全上下文 crypto 不可用导致登录回退失败

  Root cause: 浏览器非 HTTPS 环境下 window.crypto.subtle 为 undefined，
  generateCodeChallenge 抛 TypeError；上层 catch 直接 setCurrentUser(null)
  让用户被踢回登录页，但实际只是 PKCE 计算失败。
  ```

- **找 regression 用 `git log -p <file>`** 加二分回溯，先定位**引入提交**再决定怎么改。

---

## 12. 扩展信号（Scaling Signals）

当前 FSD 已够用。如果未来出现下列信号，再考虑进一步精细化：

| 信号 | 应对 |
|---|---|
| 单个 feature 文件数 > 30 | feature 内引入 `widgets/` 或 `entities/` 子分层（趋近完整 FSD） |
| 多 feature 都需要某领域规则（如权限计算） | 抽 `@/shared/domain/<rule>.ts` 纯函数库 |
| 出现真正的领域聚合行为（审批流、退款状态机） | 引入显式 Use Case 层 `features/<f>/use-cases/*.ts`（趋近 Clean Architecture） |
| HTTP / 存储 等基础设施需要切实现 | 引入 `infrastructure/` + Port 模式（趋近完整 Hexagonal） |
| 团队规模 > 3 人或 feature 数 > 15 | 评估 monorepo / 子包拆分 |

---

## 13. 依赖校验工具

§1 的依赖矩阵需要部分翻译为机器校验，最少包含：

- **格式器 + lint**（Biome / ESLint）
- **Feature 边界检查**（`scripts/check-feature-boundaries.mjs` 或 `dependency-cruiser` / `eslint-plugin-boundaries`）
- **类型检查**（`tsc --noEmit`）

三者串成一个 lint 命令，CI 必跑。

**`scripts/check-feature-boundaries.mjs` 强制规则**：

1. **禁止跨 feature 深 import**：feature A 不能 `import ... from '@/features/<B>/<内部>'`，必须走 `@/features/<B>` barrel。
2. **禁止 feature 内部用绝对路径**：feature A 内 `import ... from '@/features/A/...'` 一律改成相对路径。

**未机器校验，仅靠 review / 约定**（可按需补全）：

- Component 不能直接 `useQuery` / `useMutation`
- API 文件不能 import React
- `shared/api/http.ts` 反向依赖 feature
- Store 不能直接 fetch

需要补全 → 升级方案：引入 `dependency-cruiser`（`.dependency-cruiser.cjs` 配置）或 ESLint + `eslint-plugin-boundaries`，与现有格式器并行。

---

## 附录：路径别名

推荐只保留一个根别名：

| 别名 | 指向 |
|---|---|
| `@/*` | `./src/*` |

理由：别名多了反而模糊层级关系。feature 内部一律相对路径（强制 `@/features/<self>/...` 改回相对，见 §3）。

**项目命令、环境变量、技术栈具体版本** 见各项目根目录 `README.md` 与入口文档（`CLAUDE.md` / `AGENTS.md`），本文不维护。
