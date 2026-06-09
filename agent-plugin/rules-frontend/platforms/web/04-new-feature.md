# Web SPA — 新增 feature 操作清单

> 写一个全新 feature `<X>` 的步骤。AI 严格按顺序，每步出 commit-able 单元。

**步骤 1：定词根** —— 按 `core/02-naming.md` 选定单数小写连字符词根，在项目入口文档（`.agents/PROJECT.md` 或 `CLAUDE.md`）登记。

**步骤 2：建目录**

```bash
mkdir -p src/features/<X>/{api,pages,components}
# 按需建：queries/ hooks/ stores/ schemas/ lib/
touch src/features/<X>/index.ts src/features/<X>/types.ts
```

**步骤 3：写 API（贴后端字段）**

```ts
// src/features/<X>/api/<X>.api.ts
import { http } from '@/shared/api/http';

export interface XResponse {
  id: number;
  created_at: string;        // 允许 snake_case
}

export async function fetchX(id: string): Promise<XResponse> {
  return http.get<XResponse>(`/x/${id}`).then(r => r.data);
}
```

**步骤 4：写 Query（数据编排 + ViewModel 映射）**

```ts
// src/features/<X>/queries/<X>.keys.ts
export const xKeys = {
  all: ['x'] as const,
  detail: (id: string) => [...xKeys.all, 'detail', id] as const,
};

// src/features/<X>/queries/<X>.queries.ts
import { useQuery } from '@tanstack/react-query';
import { fetchX } from '../api/<X>.api';
import { xKeys } from './<X>.keys';

export interface XViewModel {
  id: number;
  createdAt: string;
}

export function useXQuery(id: string) {
  return useQuery({
    queryKey: xKeys.detail(id),
    queryFn: () => fetchX(id),
    select: (dto): XViewModel => ({ id: dto.id, createdAt: dto.created_at }),
  });
}
```

**步骤 5：写 Store（如有跨页 UI state）**

```ts
// src/features/<X>/stores/<X>.store.ts
import { create } from 'zustand';

interface XState {
  filter: string;
  setFilter: (filter: string) => void;
}

export const useXStore = create<XState>((set) => ({
  filter: '',
  setFilter: (filter) => set({ filter }),
}));
```

**步骤 6：写 Page**

```tsx
// src/features/<X>/pages/<X>Page.tsx
import { Spin, Alert, Empty } from 'antd';
import { useXQuery } from '../queries/<X>.queries';

export default function XPage() {
  const { data, isPending, error } = useXQuery('123');

  if (isPending) return <Spin />;
  if (error) return <Alert type="error" message={String(error)} />;
  if (!data) return <Empty />;

  return <div>{data.createdAt}</div>;
}
```

**步骤 7：在 `index.ts` 公开 API**

```ts
// src/features/<X>/index.ts
export { useXQuery } from './queries/<X>.queries';
export type { XViewModel } from './queries/<X>.queries';
export { xKeys } from './queries/<X>.keys';
// pages 不必从 barrel 导出 —— router.tsx 直接 lazy import 路径
```

**步骤 8：注册路由**

```tsx
// src/app/router.tsx
const X = lazy(() => import('@/features/<X>/pages/<X>Page'));
// ...
{ path: 'x', element: <X /> },
```

**步骤 9：加菜单**

```ts
// src/app/layouts/menuData.ts
{ path: '/x', name: 'X 模块', icon: '...' }
```

**步骤 10：质量检查 + 提交**

```bash
yarn lint              # 格式器 + boundaries + tsc
yarn test              # 如有测试
```

```
feat(<X>): 新增 <X> 模块骨架

- pages/<X>Page.tsx 编排 useXQuery
- api/<X>.api.ts 暴露 fetchX
- queries/<X>.queries.ts 包 useQuery + DTO → ViewModel 映射
- router.tsx 注册路由 + menuData 加菜单
```
