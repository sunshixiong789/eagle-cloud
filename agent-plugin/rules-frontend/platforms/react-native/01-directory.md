# React Native / Expo — 目录布局

## 根目录

- 根 `app/`：Expo Router 文件式路由 shell，不迁到 `src/app/`。
- `src/app/`：app bootstrap、providers、全局初始化。
- `src/pages/`：页面级编排；文件可命名 `XxxScreen`。
- `src/widgets/`：可选，跨 feature 的业务 UI 块。
- `src/features/<feature>/`：业务 feature。
- `src/entities/<entity>/`：可选，稳定核心业务实体。
- `src/shared/`：通用 UI、api、hooks、lib、constants、types。
- `src/infrastructure/`：SecureStore、Keychain、permissions、push、native bridge 等平台适配。

## Feature 内部

Feature 约定子目录：`api/`、`queries/`、`hooks/`、`stores/`、`components/`、`lib/`、`types.ts`。

RN 禁用 slice barrel；跨 slice 依赖优先上移 shared、entities 或 Page 编排。

## RN 特征

- 根 `app/**` 路由文件只 re-export `src/pages` 或 feature 页面入口。
- 样式使用 NativeWind，平台颜色/权限/存储走 infrastructure。
