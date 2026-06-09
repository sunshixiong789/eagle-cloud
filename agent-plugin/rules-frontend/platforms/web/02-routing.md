# Web SPA — 路由

## 约定

- 集中在 `src/app/router.tsx`，使用 `useRoutes` + `React.lazy`。
- 路由文件只声明路径、懒加载 Page、layout、guard 和 404。
- Page 负责解析 URL params、导航和多 feature 编排。
- Component 不直接 `useNavigate` / `useLocation` 做业务跳转。
- 404 放最后。

## Guard

Guard 用普通组件包裹 element，例如 `AuthGuard` / `PermissionGuard`。不要与 React Router data API 的 loader/action 混用。

## 禁止清单

- 路由 import feature 内部 hook/store/api。
- Component 内散落导航。
- 同一路径在 router、menu、权限配置写出不同字符串。
