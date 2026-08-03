# FSD-lite 架构、依赖方向与 slice 边界

原则：**先按业务复杂度拆分，再选择层级**。不要为了"看起来像 FSD"强制创建 `entities`、`widgets` 或过细 slice。

## 统一目录层

```text
src/
├── app/                 # bootstrap、providers、router adapter
├── pages/               # 页面级编排；RN/Taro 文件可命名 Screen
├── widgets/             # 可选：跨 feature 的业务 UI 块
├── features/            # 用户动作 / 业务能力
├── entities/            # 可选：稳定核心业务实体
├── shared/              # ui / api / lib / config / i18n / assets
└── infrastructure/      # 平台能力 adapter
```

- `providers/` 不作顶层目录，放 `src/app/providers/`
- `entities/` 和 `widgets/` 按复杂度引入，轻应用可不创建
- 架构术语统一叫 Page；RN/Taro 文件名可继续用 `XxxScreen`

平台 shell 位置各不相同（Web 的 `router.tsx` / Expo 的根 `app/` / Taro 的 `src/pages/`），见 `04-platforms.md`。**Shell 只挂载页面入口，不写业务逻辑。**

## 角色矩阵

| 角色 | 位置 | 职责 | 不可依赖 |
| --- | --- | --- | --- |
| Route Shell | 平台入口 | 路由声明、layout、guard、params 透传 | feature 内部 hook/store/api |
| Page / Screen | `pages/` 或 feature 页面入口 | 编排 query、store、component、loading/error/empty | 直接调底层 HTTP |
| Widget | `widgets/` | 跨 feature 的业务 UI 组合 | feature 内部私有模块 |
| Feature | `features/` | 用户动作、业务流程、feature 私有 UI/state/api | 其他 feature 内部 |
| Entity | `entities/` | 稳定业务对象的模型、展示、基础查询 | feature 业务流程 |
| Shared | `shared/` | 无业务归属的基础能力 | feature / entity / page |
| Infrastructure | `infrastructure/` | 平台 IO、storage、native、analytics、payment | React UI、业务 feature |

## 依赖方向

```text
app -> pages -> widgets -> features -> entities -> shared
                         -> infrastructure
```

- 上层可依赖下层；**下层禁止反向依赖上层**
- 同层 slice 之间不深 import，只能通过 public API，或上移 shared / 由 Page 编排
- `shared/api/http.ts` 不直接 import store、router、feature —— token、tenant、401 handler 由 `app/providers` **启动期注入**（依赖反转）

## Feature 内部结构

```text
features/<feature>/
├── api/          # 只处理 DTO 和 IO，不 import React runtime
├── queries/      # React Query hooks + key factory；DTO -> ViewModel
├── hooks/        # 业务派生逻辑
├── stores/       # feature 私有 Zustand store
├── components/   # feature 私有组件，只渲染 props
├── lib/          # 私有纯函数
└── types.ts
```

**禁止**新增 `context/`、`guard/`、`utils/` 等非约定子目录 —— 用 `components/` 或 `lib/` 容纳。

## Slice 边界与 Public API

每个 `pages / widgets / features / entities` 下的 slice 默认只暴露 public API，内部目录不可被外部深 import。

### 跨 slice 协作的四条出路

| 情况 | 做法 |
|---|---|
| 可复用且无业务归属 | 上移 `shared/` |
| 页面级组合 | 由 Page 同时编排多个 feature/entity |
| 稳定业务对象 | 下沉到 `entities/<entity>` |
| 确需跨 slice 复用 | 通过该 slice public API 暴露，保持 named exports |

### Barrel 策略按平台分叉

| 平台 | 策略 |
|---|---|
| Web | 可用 slice 顶层 barrel，跨 slice 只 import 顶层 public API，**禁止深路径** |
| React Native / Taro | **默认禁用 barrel**（Metro/Taro 解析与 tree-shaking 问题）；public API 用明确文件路径表达 |

**一律禁止 `export *`**，只允许 named exports 明确公开能力。

## 禁止共享

- feature 内部 store、service、私有 hook、私有 component
- 为复用一个函数就深 import 其他 slice 内部文件
- `shared` 反向依赖 pages / widgets / features / entities

## Infrastructure

对外暴露纯函数、类实例或 adapter，供 shared/api 或 feature 通过依赖注入使用。

- Web：analytics、Service Worker、IndexedDB、WebRTC
- RN：SecureStore / Keychain、permissions、push、native bridge
- Taro：`Taro.*` / `wx.*` / `my.*`、支付、文件、登录等跨端适配
