# FSD-lite 架构与依赖矩阵

适用 Web / React Native / Taro。参考 Feature-Sliced Design 的分层与 public API 思想，但不是完整照搬 FSD。
Eagle 前端采用业务优先的 FSD-lite：统一业务边界，平台入口保留差异。

原则：先按业务复杂度拆分，再选择层级。不要为了“看起来像 FSD”强制创建 `entities`、`widgets` 或过细 slice。

## 统一目录层

```text
src/
├── app/                 # app bootstrap, providers, router adapter
├── pages/               # 页面级编排；RN/Taro 文件可命名 Screen
├── widgets/             # 可选：跨 feature 的业务 UI 块
├── features/            # 用户动作 / 业务能力
├── entities/            # 可选：稳定核心业务实体
├── shared/              # ui / api / lib / config / i18n / assets
└── infrastructure/      # 平台能力 adapter
```

- `providers/` 不作为顶层目录，放在 `src/app/providers/`。
- `entities/` 和 `widgets/` 按复杂度引入；轻应用可以暂不创建。
- 架构术语统一叫 Page；RN/Taro 文件名可继续用 `XxxScreen`。

## 平台 Shell

- Web：`src/app/router.tsx` 是路由 shell。
- Expo / React Native：根 `app/` 是 Expo Router shell，不迁到 `src/app/`。
- Taro：`src/pages/` + `src/app.config.ts` 是 Taro shell。

Shell 文件只挂载 `src/pages` 或 feature 页面入口，不写业务逻辑。

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

- 上层可依赖下层；下层禁止反向依赖上层。
- 同层 slice 之间不互相深 import，只能通过 public API 或上移 shared/page 编排解决。
- `shared/api/http.ts` 不直接 import store、router、feature；token、tenant、401 handler 由 `app/providers` 启动期注入。

## Feature 内部角色

Feature 内可按需保留：

```text
api/ queries/ hooks/ stores/ components/ lib/ types.ts
```

- API/Service 只处理 DTO 和 IO，不 import React runtime。
- Query/Hook 负责 DTO -> ViewModel、缓存和业务派生。
- Component 只渲染 props，不直接请求 API、不导航、不读写 token。

## Infrastructure

- Web：analytics、Service Worker、IndexedDB、WebRTC 等按需启用。
- RN：SecureStore / Keychain、permissions、push、native bridge。
- Taro：`Taro.*`、`wx.*`、`my.*`、支付、文件、登录等跨端适配。

Infrastructure 对外暴露纯函数、类实例或 adapter，供 shared/api 或 feature 通过依赖注入使用。
