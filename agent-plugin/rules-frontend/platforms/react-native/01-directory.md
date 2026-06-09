# React Native / Expo — 目录布局

## 根目录

- 根 `app/`：Expo Router 文件式路由薄壳，不迁到 `src/app/`。
- `src/features/<feature>/`：业务 feature。
- `src/shared/`：通用 UI、api、hooks、lib、constants、types。
- `src/providers/`：Provider。
- `src/infrastructure/`：SecureStore、Keychain、permissions、push、native bridge 等平台适配。

## Feature 内部

约定子目录：`api/`、`queries/`、`hooks/`、`stores/`、`components/`、`screens/`、`lib/`、`types.ts`。

RN 禁用 feature barrel；跨 feature 依赖优先上移 shared 或 Screen 编排。

## RN 特征

- Screen 放 `screens/`，路由文件只 re-export Screen。
- 样式使用 NativeWind，平台颜色/权限/存储走 infrastructure。
