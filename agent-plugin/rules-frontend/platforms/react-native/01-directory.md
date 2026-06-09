# React Native (Expo Router) — 目录布局

> 通用 FSD 原则见 `core/01-architecture.md`。本文只列 RN/Expo 特有部分。

```
<repo>/
├── app/                              # ✅ Expo Router 路由根（不迁 src/app/）
│   ├── _layout.tsx                   # ✅ 挂 AppProvider + Stack 声明
│   ├── (tabs)/                       # ✅ 底部 tab
│   │   ├── _layout.tsx
│   │   └── <tab>.tsx                 # ✅ 1 行 re-export 壳，指向 @features/<f>/screens/...
│   ├── <route>/[id].tsx              # ✅ 1 行 re-export 壳
│   └── ...                           # ✅ 其余 modal 路由都是薄壳
│
├── src/
│   ├── features/                     # ✅ 业务 bounded context
│   │   └── <feature>/
│   │       ├── api/                  # ✅ API 调用 + DTO + .test.ts
│   │       ├── hooks/                # ✅ React Query hooks + 派生计算
│   │       ├── stores/               # ✅ Zustand store（feature 私有 state）
│   │       ├── components/           # ✅ feature 内部复用组件
│   │       ├── screens/              # ✅ 屏幕组件，被 app/ 薄壳 import
│   │       ├── queries/              # 🟡 按需：query/mutation > 5 个时拆 keys + queries
│   │       ├── schemas/              # 🟡 按需：runtime 输入校验时启用
│   │       ├── types.ts              # 🟡 按需：跨文件共享类型 ≥ 3 个时创建
│   │       └── index.ts              # ❌ 不采用：Metro tree-shaking 不友好，barrel 禁用
│   │
│   ├── shared/                       # ✅ Shared Kernel
│   │   ├── api/
│   │   │   ├── http.ts               # ✅ fetch 封装 + AbortController + HttpAuthBridge
│   │   │   ├── oauth.ts              # ✅ OAuth client_id 取值器 + OAuth2Token 类型
│   │   │   ├── error.ts              # 🟡 按需：错误类型 ≥ 2 个时拆
│   │   │   ├── interceptors.ts       # 🟡 按需：第三方拦截器
│   │   │   └── types.ts              # 🟡 按需：公共类型
│   │   ├── stores/                   # ✅ 跨 feature 共享 store
│   │   │   └── persist-storage.ts    # ✅ AsyncStorage 跨平台 wrapper（非 store，是工具）
│   │   ├── ui/                       # ✅ 跨 feature 复用纯展示组件（kebab-case 文件名）
│   │   ├── hooks/                    # ✅ 通用 hook（color-scheme、theme 等）
│   │   ├── constants/                # ✅ theme tokens、env、storage-keys
│   │   ├── utils/                    # 🟡 按需：format / date / money 等
│   │   ├── schemas/                  # 🟡 按需：跨 feature 通用校验
│   │   └── types/                    # 🟡 按需：跨 feature 通用类型
│   │
│   ├── infrastructure/               # ✅ 必有 Hexagonal Adapters
│   │   ├── storage/                  # ✅ SecureStore 等持久化封装
│   │   ├── native/                   # ✅ 原生桥（如平台 SDK）
│   │   ├── analytics/                # 🟡 按需：Mixpanel / Amplitude / 自研事件
│   │   ├── permissions/              # 🟡 按需：相机 / 位置 / 通知授权
│   │   └── notifications/            # 🟡 按需：Push / 本地通知
│   │
│   ├── providers/                    # ✅ Composition Root
│   │   ├── AppProvider.tsx           # ✅ 顶层聚合 + configureHttp wiring
│   │   ├── QueryProvider.tsx         # ✅ QueryClient + QueryClientProvider
│   │   └── ThemeProvider.tsx         # ✅ Navigation theme + nwColorScheme.set(mode)
│   │
│   ├── app/                          # ❌ 不采用：Expo Router 用根 app/；切到 src/app/ 需
│   │                                 #     EXPO_ROUTER_APP_ROOT 配置，零收益高风险
│   │
│   ├── assets/                       # ❌ 不采用：保留根 /assets/（Expo asset 加载惯例）
│   │
│   └── config/                       # ❌ 不采用：env 走 EXPO_PUBLIC_*、app config 用 app.json、
│                                     #     theme tokens 在 @shared/constants/ + tailwind.config.js
│
├── assets/                           # ✅ 图片 / 字体 / 图标
├── docs/                             # ✅ specs / plans 历史
├── __mocks__/                        # ✅ Jest mocks
├── app.json                          # ✅ Expo config
├── tailwind.config.js                # ✅ NativeWind tokens
├── tsconfig.json                     # ✅ 路径别名定义
├── jest.config.js                    # ✅ logic + rn 双 project
└── ...
```

## Feature 内部布局

```
src/features/<name>/
├── api/         # 必有：API 调用 + DTO + .test.ts
├── screens/     # 必有：屏幕组件（被 app/ 薄壳 re-export）
├── hooks/       # 通常有：use-* React Query / 派生
├── components/  # 按需：feature 内部复用
├── stores/      # 按需：feature 私有 state
├── queries/     # 按需：keys + queries（query 数量 > 5 时拆）
├── schemas/     # 按需：zod 校验
└── types.ts     # 按需：跨文件共享类型
```

**barrel `index.ts` 禁用**（Metro tree-shaking 不友好）。跨 feature 引用走直接路径（详见 `core/05-bounded-context.md`）。

## RN 特征

- 路由用 Expo Router 的根 `app/` 文件夹（**不迁 `src/app/`**——改路径需配置 `EXPO_ROUTER_APP_ROOT`，零收益高风险）
- `src/infrastructure/` 是事实标配（SecureStore、Push、平台桥、跨平台 storage wrapper）
- 跨 feature 通过 `@features/<x>/screens/...` 等直接路径引用
- 样式：NativeWind className + `theme.X` 用于 RN 原生 prop 颜色（详见 `03-styling.md`）

## 不采用清单

| 目录 / 模式 | 理由 |
|---|---|
| `src/app/` | Expo Router 用根 `app/`；改路径零收益高风险 |
| `src/assets/` | 与 Expo asset 加载惯例不符，保持根 `/assets/` |
| `src/config/` | env 走 `EXPO_PUBLIC_*`，app config 用 `app.json`，theme tokens 在 `@shared/constants/` + `tailwind.config.js` |
| feature `index.ts` barrel | Metro tree-shaking 不友好；引起循环依赖与首屏体积膨胀 |
| feature 内 `context/` `guard/` `utils/` 等非约定子目录 | 用 `components/` / 上提到 `@shared/` |
| `shared/query/` | QueryClient + Provider 直接在 `@providers/QueryProvider.tsx`，单点 composition |
