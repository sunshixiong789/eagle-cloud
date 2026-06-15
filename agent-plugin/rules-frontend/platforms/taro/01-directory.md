# Taro — 目录布局

## 根目录

- `src/app.config.ts`：全局页面注册和窗口配置。
- `src/pages/`：Taro 框架页面入口 shell。
- `src/app/`：app bootstrap、providers、全局初始化。
- `src/pages-fsd/` 或 `src/features/*/pages/`：可选业务 Page 层；若项目不想引入新目录，可直接用 feature 页面入口。
- `src/widgets/`：可选，跨 feature 的业务 UI 块。
- `src/features/<feature>/`：业务 feature。
- `src/entities/<entity>/`：可选，稳定核心业务实体。
- `src/shared/`：通用 UI、api、hooks、lib、constants、types。
- `src/infrastructure/`：Taro/wx/my/tt 等平台 API 适配。

## Feature 内部

Feature 约定子目录：`api/`、`queries/`、`hooks/`、`stores/`、`components/`、`lib/`、`types.ts`。

Taro 禁用 slice barrel；跨 slice 依赖优先上移 shared、entities 或 Page 编排。

## Taro 特征

- `src/pages/<route>/index.tsx` 只 re-export 对应业务 Page/Screen。
- 每个页面需要 `index.config.ts` 时放在 `pages` 同级目录。
- 多端差异代码放 `infrastructure/` 或 Taro 条件编译文件，不散落在业务组件。
- `config/index.ts` compiler 与实际构建链保持同步。
