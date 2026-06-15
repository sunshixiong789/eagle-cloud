# Web SPA — 目录布局

## 根目录

- `src/app/`：应用启动、router、全局 layout。
- `src/pages/`：页面级编排。
- `src/widgets/`：可选，跨 feature 的业务 UI 块。
- `src/features/<feature>/`：业务 feature。
- `src/entities/<entity>/`：可选，稳定核心业务实体。
- `src/shared/`：无业务归属的 UI、api、hooks、lib、constants、types。
- `src/app/providers/`：Query、Theme、Auth bootstrap 等 Provider。
- `src/infrastructure/`：可选，analytics、storage、browser API adapter。
- `src/styles/`：Tailwind 入口、字体、reset，不放业务样式。

## Feature 内部

Feature 约定子目录：`api/`、`queries/`、`hooks/`、`stores/`、`components/`、`lib/`、`types.ts`。

- 页面级编排优先放 `src/pages/<page>/`，简单项目也可暂放 feature 页面入口。
- `api/` 只处理 DTO 和 HTTP 调用。
- `queries/` 包 React Query hooks 和 key factory。
- `components/` 只放 feature 私有组件。
- 禁止新增 `context/`、`guard/`、`utils/` 等非约定目录；需要先登记规则。

## Web 特征

- 路由集中在 `src/app/router.tsx`。
- Web 可启用 slice 顶层 barrel，但禁止 `export *`。
- 样式三件套见 `03-styling.md`。
