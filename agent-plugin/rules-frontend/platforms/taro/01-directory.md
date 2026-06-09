# Taro — 目录布局

## 根目录

- `src/app.config.ts`：全局页面注册和窗口配置。
- `src/pages/`：Taro 框架页面入口薄壳。
- `src/features/<feature>/`：业务 feature。
- `src/shared/`：通用 UI、api、hooks、lib、constants、types。
- `src/providers/`：Provider。
- `src/infrastructure/`：Taro/wx/my/tt 等平台 API 适配。

## Feature 内部

约定子目录：`api/`、`queries/`、`hooks/`、`stores/`、`components/`、`screens/`、`lib/`、`types.ts`。

Taro 禁用 feature barrel；跨 feature 依赖优先 shared 或 Screen 编排。

## Taro 特征

- `src/pages/<route>/index.tsx` 只 re-export 对应 Screen。
- 每个页面需要 `index.config.ts` 时放在 `pages` 同级目录。
- 多端差异代码放 `infrastructure/` 或 Taro 条件编译文件，不散落在业务组件。
- `config/index.ts` compiler 与实际构建链保持同步。
