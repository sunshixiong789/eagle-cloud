# Web SPA — 目录布局

> 通用 FSD 原则见 `core/01-architecture.md`。本文只列 Web 特有部分。

```
<repo>/
├── src/
│   ├── app/                          # ✅ 应用外壳（非文件式路由）
│   │   ├── router.tsx                # ✅ useRoutes 集中声明（含 React.lazy）
│   │   ├── layouts/                  # ✅ BasicLayout / BlankLayout / menuData
│   │   └── config/                   # ✅ 应用配置（API base、业务常量、主题默认 token）
│   │
│   ├── features/                     # ✅ 业务模块
│   │   └── <feature>/
│   │       ├── api/                  # ✅ API 调用 + DTO 类型 (+ .test.ts)
│   │       ├── components/           # ✅ feature 内部组件（含 Provider 与 Guard）
│   │       ├── pages/                # ✅ 路由级组件，*Page.tsx + *Page.style.ts
│   │       ├── types.ts              # ✅ feature 私有类型（**禁止**并建 types/ 目录）
│   │       ├── index.ts              # ✅ 对外公开 API barrel（Web 启用 barrel）
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
│   ├── infrastructure/               # 🟡 按需：analytics / Service Worker / IndexedDB wrapper
│   │
│   ├── styles/                       # ✅ 全局样式（仅 @font-face + 基础 reset + Tailwind 入口）
│   ├── assets/                       # ✅ 图片 / 字体 / 图标
│   └── main.tsx                      # ✅ 入口（挂 AppProvider + <RouterProvider />）
│
├── scripts/
│   └── check-feature-boundaries.mjs  # ✅ feature 边界检查（挂在 lint）
├── public/                           # ✅ 静态资源（不经构建器处理）
├── docs/                             # 🟡 specs / 设计说明
├── vite.config.ts                    # ✅ 构建 + dev proxy + 别名
├── tsconfig.json                     # ✅ 路径别名定义
├── biome.json 或 eslint.config.*     # ✅ lint / format
├── tailwind.config.ts                # ✅ Tailwind tokens
└── package.json
```

## Feature 内部布局

```
src/features/<name>/
├── api/         # 必有：API 调用 + DTO + .test.ts
├── pages/       # 必有：路由级组件 *Page.tsx + 可选 *Page.style.ts
├── components/  # 通常有：feature 内部复用展示组件
├── queries/     # 通常有：keys.ts + queries.ts（按需）
├── stores/      # 按需：feature 私有 state
├── hooks/       # 按需：feature 私有 React hooks
├── lib/         # 按需：feature 私有非 React 工具
├── schemas/     # 按需：zod / 校验 schema
├── types.ts     # 按需：跨文件共享类型
└── index.ts     # 必有：对外公开 API barrel
```

**已建的子目录必须遵循约定形状**，禁止新增非约定子目录（如 `context/`、`guard/`、`utils/`）。需要新增约定子目录 → **先在本文登记位置规则再建**。

## Web 特征

- 路由集中在 `src/app/router.tsx`，通过 `React.lazy` + `useRoutes` 声明（详见 `02-routing.md`）
- `src/infrastructure/` 通常不建——大多数项目外部 IO 直接写到 `shared/api/` 或 `shared/lib/`；需要按需启用
- feature `index.ts` barrel **启用**——跨 feature 一律 `@/features/<x>` 走 barrel（详见 `core/05-bounded-context.md`）
- 样式三件套：Tailwind atom + CSS-in-JS theme + CSS Modules legacy（详见 `03-styling.md`）

## 不采用清单

| 目录 / 模式 | 理由 |
|---|---|
| `src/types/` | feature 内只用 `types.ts` 单文件；跨 feature 放 `shared/types/` 或 `shared/types.d.ts` |
| feature `context/` `guard/` `utils/` 等非约定子目录 | 用 `components/` / `lib/` 容纳 |
| feature 内 barrel `export *` | 必须写明确的 named exports |
| 文件式路由（如 Next.js Pages Router 风格） | 本规范面向纯 CSR SPA；用 Next.js 等框架的项目另立规则 |
