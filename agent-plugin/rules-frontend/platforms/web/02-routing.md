# Web SPA — 路由

集中式：所有路由在 `src/app/router.tsx` 一处声明，配合 `React.lazy` + `useRoutes`。

## 路由声明骨架

```tsx
// src/app/router.tsx
import { Suspense, lazy } from 'react';
import { useRoutes, Navigate } from 'react-router-dom';
import { BasicLayout } from './layouts/BasicLayout';
import { BlankLayout } from './layouts/BlankLayout';
import { AuthGuard } from '@/features/auth';

const Dashboard = lazy(() => import('@/features/dashboard/pages/DashboardPage'));
const UserList  = lazy(() => import('@/features/user/pages/UserListPage'));
const Login     = lazy(() => import('@/features/auth/pages/LoginPage'));

export function AppRouter() {
  const element = useRoutes([
    { path: '/login', element: <BlankLayout><Login /></BlankLayout> },
    {
      path: '/',
      element: <AuthGuard><BasicLayout /></AuthGuard>,
      children: [
        { index: true, element: <Navigate to="/dashboard" replace /> },
        { path: 'dashboard', element: <Dashboard /> },
        { path: 'users', element: <UserList /> },
      ],
    },
    { path: '*', element: <NotFound /> },
  ]);

  return <Suspense fallback={<PageSkeleton />}>{element}</Suspense>;
}
```

## 关键约定

1. **路由文件本身不写业务**——Page 组件全部 lazy import 自 `@/features/<f>/pages/`。
2. **Guard 用包装组件**：`AuthGuard`、`PermissionGuard` 写成普通 React 组件，包在 element 外层。**禁止**用 `loader` / `action`（与 react-router data API 选其一，不混用）。
3. **菜单数据与路由解耦**：菜单展示数据放 `src/app/layouts/menuData.ts`（路径 + 名称 + 图标 + 权限码），与 `router.tsx` 的路由表平行维护；通过 `path` 字符串关联，不互相 import。
4. **Suspense 顶层一次**：整个 `<AppRouter />` 包一层 Suspense，不在每个 Route 单独 Suspense。
5. **404 必须放最后**：`{ path: '*', element: <NotFound /> }` 兜底。

## Lazy import 命名

```ts
// ✅ 名字保持与 Page 组件一致
const UserListPage = lazy(() => import('@/features/user/pages/UserListPage'));

// ❌ 重命名 lazy 后变量会破坏 grep 一致性
const UserList = lazy(() => import('@/features/user/pages/UserListPage'));
```

约定：`const <PageName> = lazy(() => import(...))`，与文件名 + 默认导出名严格一致。

## Chunk 分组（可选）

对页面数量 30+ 的项目，单页 chunk 太碎会拖累 HTTP/2 连接复用。按业务域分组：

```ts
const UserList = lazy(() =>
  import(/* webpackChunkName: "user" */ '@/features/user/pages/UserListPage')
);
const UserDetail = lazy(() =>
  import(/* webpackChunkName: "user" */ '@/features/user/pages/UserDetailPage')
);
```

Vite 用 Rollup `manualChunks`：

```ts
// vite.config.ts
export default defineConfig({
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('/features/user/')) return 'feat-user';
          if (id.includes('/features/order/')) return 'feat-order';
        },
      },
    },
  },
});
```

## URL params

`react-router` 的 `useParams` 在 Page 顶部读取，转换后再传给 hook/query：

```tsx
function UserDetailPage() {
  const { id } = useParams<{ id: string }>();
  const numericId = Number(id);
  if (!Number.isFinite(numericId)) return <NotFound />;
  const { data, isPending } = useUserDetailQuery(numericId);
  // ...
}
```

## 禁止清单

- 禁止路由文件 import feature 内部模块（只能 lazy import Page 组件）
- 禁止在 Component 内调 `useNavigate` / `useLocation` 进行导航（导航属于 Page 编排）
- 禁止使用 `<Route loader>` / `<Route action>`（与 element 形式混用会乱）
- 禁止把同一路径在 `router.tsx` 与 `menuData.ts` 中写不同字符串
