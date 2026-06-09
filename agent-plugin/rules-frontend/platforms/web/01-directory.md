# Web SPA — 目录布局

## 根目录

- `src/app/`：应用启动、router、全局 layout。
- `src/features/<feature>/`：业务 feature。
- `src/shared/`：无业务归属的 UI、api、hooks、lib、constants、types。
- `src/providers/`：Query、Theme、Auth bootstrap 等 Provider。
- `src/styles/`：Tailwind 入口、字体、reset，不放业务样式。

## Feature 内部

约定子目录：`api/`、`queries/`、`hooks/`、`stores/`、`components/`、`screens/`、`lib/`、`types.ts`。

- Page/Screen 放 `screens/`，由 router lazy 引入。
- `api/` 只处理 DTO 和 HTTP 调用。
- `queries/` 包 React Query hooks 和 key factory。
- `components/` 只放 feature 私有组件。
- 禁止新增 `context/`、`guard/`、`utils/` 等非约定目录；需要先登记规则。

## Web 特征

- 路由集中在 `src/app/router.tsx`。
- Web 可启用 feature 顶层 barrel，但禁止 `export *`。
- 样式三件套见 `03-styling.md`。
