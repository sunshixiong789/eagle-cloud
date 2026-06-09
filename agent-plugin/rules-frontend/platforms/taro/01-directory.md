# Taro (4.x 多端) — 目录布局

> 通用 FSD 原则见 `core/01-architecture.md`。本文只列 Taro 特有部分。
>
> Taro 支持编译到：微信 weapp / 支付宝 alipay / 字节 tt / 百度 swan / QQ / 京东 jd / H5 / RN / 鸿蒙混合。**同一份 src 多端构建**。

```
<repo>/
├── src/
│   ├── app.tsx                       # ✅ App 入口（挂 AppProvider，useLaunch 生命周期）
│   ├── app.config.ts                 # ✅ defineAppConfig：pages 数组 + window 配置 + tabBar
│   ├── app.css                       # ✅ Tailwind v4 入口（theme + utilities，不导入 preflight）
│   ├── index.html                    # 🟡 H5 模板（其他端忽略）
│   │
│   ├── pages/                        # ✅ Taro 框架的页面入口（**1 行 re-export 薄壳**）
│   │   ├── index/
│   │   │   ├── index.tsx             # ✅ export { default } from '@features/<f>/screens/...'
│   │   │   └── index.config.ts       # ✅ definePageConfig：navigationBarTitleText 等
│   │   └── product/
│   │       ├── index.tsx
│   │       └── index.config.ts
│   │
│   ├── features/                     # ✅ 业务 bounded context
│   │   └── <feature>/
│   │       ├── api/                  # ✅ API 调用 + DTO（包 Taro.request）
│   │       ├── hooks/                # 🟡 按需：React 派生 hook
│   │       ├── queries/              # ✅ React Query keys + queries
│   │       ├── stores/               # ✅ Zustand store
│   │       ├── components/           # ✅ feature 内部组件
│   │       ├── screens/              # ✅ 屏幕组件，被 pages/<route>/index.tsx 薄壳引用
│   │       ├── types.ts              # 🟡 按需
│   │       └── index.ts              # ❌ barrel 禁用（构建器与小程序限制）
│   │
│   ├── shared/                       # ✅ Shared Kernel
│   │   ├── api/
│   │   │   └── http.ts               # ✅ Taro.request 封装 + ApiError + HttpAuthBridge
│   │   ├── stores/                   # ✅ 跨 feature store
│   │   ├── ui/ 或 components/        # ✅ 跨 feature 复用展示组件
│   │   ├── hooks/                    # ✅ 通用 hook
│   │   ├── constants/                # ✅ tokens、env、storage-keys
│   │   └── utils/                    # 🟡 按需
│   │
│   ├── infrastructure/               # ✅ 推荐有：跨端兼容层、原生 API 封装
│   │   ├── storage/                  # Taro.setStorageSync wrapper
│   │   ├── native/                   # wx.login / 支付 / 文件 / 跨端兼容
│   │   └── analytics/                # 🟡 按需
│   │
│   ├── providers/                    # ✅ Composition Root
│   │   ├── AppProvider.tsx           # ✅ 顶层聚合 + configureHttp wiring
│   │   ├── QueryProvider.tsx         # ✅ QueryClient
│   │   └── ThemeProvider.tsx         # 🟡 按需
│   │
│   └── ...
│
├── config/                           # ✅ Taro CLI 配置（不是 webpack.config.js）
│   ├── index.ts                      # ✅ Taro 主配置：alias、compiler、mini.webpackChain 挂 weapp-tw
│   ├── dev.ts                        # ✅ dev 环境覆盖
│   └── prod.ts                       # ✅ prod 环境覆盖
│
├── types/                            # ✅ ambient declarations
│   └── global.d.ts                   # ✅ NodeJS.ProcessEnv 中声明 TARO_APP_* 变量
│
├── .env.development                  # ✅ dev 环境（TARO_APP_API、TARO_APP_ID）
├── .env.test                         # ✅ test 环境
├── .env.production                   # ✅ prod 环境
├── project.config.json               # ✅ 小程序项目配置（开发者工具用）
├── babel.config.js                   # ✅ compiler 必须与 config/index.ts 同步为 'webpack5'（示例见下）
├── postcss.config.js                 # ✅ @tailwindcss/postcss 注册
├── tailwind.config.js                # 🟡 Tailwind v4 大部分配置已迁移到 app.css
├── tsconfig.json                     # ✅ 路径别名（必须与 config/index.ts 双向同步）
└── package.json                      # ✅ scripts 含 dev:weapp/build:h5/... 多端命令
```

## Feature 内部布局

```
src/features/<name>/
├── api/         # 必有：API 调用 + DTO
├── screens/     # 必有：屏幕组件（被 src/pages/<route>/ 薄壳引用）
├── queries/     # 推荐有：keys + queries
├── stores/      # 按需
├── components/  # 按需
├── hooks/       # 按需
└── types.ts     # 按需
```

**barrel `index.ts` 禁用**（构建器 tree-shaking 受小程序包大小限制影响；与 RN 风格一致）。

## Taro 特征

### 1. `src/pages/` ≠ Web 的 `pages/`

Taro 的 `src/pages/<route>/index.tsx` 是**框架强制要求的页面入口**——必须存在、必须在 `app.config.ts` 的 `pages` 数组里注册。
它**只放 1 行 re-export**，实际业务在 `@features/<f>/screens/<X>Screen.tsx`：

```tsx
// src/pages/product/index.tsx —— 框架入口，1 行薄壳
export { default } from '@features/product/screens/ProductListScreen';
```

```ts
// src/pages/product/index.config.ts —— 页面级配置
export default definePageConfig({
  navigationBarTitleText: '商品列表',
});
```

业务 Screen 还是按通用 FSD 在 `@features/product/screens/ProductListScreen.tsx`。

### 2. 多端构建

```bash
yarn dev:weapp           # 微信小程序
yarn dev:alipay          # 支付宝小程序
yarn dev:h5              # H5 浏览器
yarn dev:swan / tt / qq / jd / harmony-hybrid / rn   # 其他平台
yarn build:<platform>    # 生产构建
```

每个平台产物在 `dist/`。

### 3. Tailwind v4 + weapp-tailwindcss

小程序 class 命名不能含 `:` / `[` / `]` 等 Tailwind 默认转义字符，需要 `weapp-tailwindcss` 在打包时做 class 转义。详见 `03-styling.md`。

### 4. 环境变量

业务变量**必须以 `TARO_APP_` 开头**，否则 Taro 不会注入到客户端 bundle。新增任何 `TARO_APP_*` 变量后**必须同步**在 `types/global.d.ts` 的 `NodeJS.ProcessEnv` 里加声明。

### 5. compiler 同步铁律

`config/index.ts` 的 `compiler.type` 与 `babel.config.js` 的 `presets.taro.compiler` **必须一致**——都是 `'webpack5'`。任何一处不同步都会让构建器与 babel 不在同一 transpiler 链上工作。

```js
// babel.config.js
module.exports = {
  presets: [
    ['taro', {
      framework: 'react',
      ts: true,
      compiler: 'webpack5',     // ← 与 config/index.ts compiler.type 同步
    }]
  ]
};
```

```ts
// config/index.ts
export default defineConfig<'webpack5'>(async (merge, {}) => ({
  compiler: {
    type: 'webpack5',            // ← 与 babel.config.js 同步
    // ...
  },
}));
```

新建 Taro 项目脚手架默认是 `'webpack5'`；除非确认要升级 / 降级，不要单独改一处。

## 不采用清单

| 目录 / 模式 | 理由 |
|---|---|
| `src/app/` 集中路由文件 | Taro 路由由框架强制文件式（`app.config.ts` + `src/pages/`） |
| feature `index.ts` barrel | 构建器与小程序包大小限制 |
| feature 内 `context/` / `guard/` / `utils/` 非约定子目录 | 用 `components/` / 上提 `@shared/` |
| 在 `src/pages/<route>/index.tsx` 内写业务 | 必须 1 行 re-export，业务一律在 `@features/<f>/screens/` |
| 直接调 `wx.xxx` / `my.xxx` / `tt.xxx` | 走 Taro API（`Taro.login`、`Taro.request`）或 `@infra/native/` 跨端封装 |
