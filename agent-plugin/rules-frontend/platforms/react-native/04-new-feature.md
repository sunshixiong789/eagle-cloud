# React Native (Expo Router) — 新增 feature 操作清单

> 写一个全新 feature `<X>`（比如 `wallet`）的步骤。AI 严格按顺序，每步出 commit-able 单元。

**步骤 1：定词根** —— 按 `core/02-naming.md` 选词根，在 `.agents/PROJECT.md` 登记。

**步骤 2：建目录**

```bash
mkdir -p src/features/<X>/{api,hooks,screens}
# 按需建：stores / components / queries / schemas
```

> **不建** `src/features/<X>/index.ts`（RN barrel 禁用）。

**步骤 3：写 API 层**

```ts
// src/features/<X>/api/<X>.api.ts
import { api } from '@shared/api/http';

export interface XResponse {
  id: number;
  created_at: string;        // 允许 snake_case
}

export async function fetchX(id: string): Promise<XResponse> {
  return api.get<XResponse>(`/x/${id}`);
}
```

**步骤 4：写 Hook（数据编排 + ViewModel 映射）**

```ts
// src/features/<X>/hooks/use-<X>.ts
import { useQuery } from '@tanstack/react-query';
import { fetchX, type XResponse } from '@features/<X>/api/<X>.api';

export interface XViewModel {
  id: number;
  createdAt: string;
}

function toViewModel(dto: XResponse): XViewModel {
  return { id: dto.id, createdAt: dto.created_at };
}

export function useXQuery(id: string) {
  return useQuery({
    queryKey: ['x', id] as const,
    queryFn: () => fetchX(id),
    select: toViewModel,
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

**步骤 6：写 Screen**

```tsx
// src/features/<X>/screens/<X>Screen.tsx
import { View, Text, ActivityIndicator } from 'react-native';
import { useXQuery } from '@features/<X>/hooks/use-<X>';

export default function XScreen() {
  const { data, isPending, error } = useXQuery('123');

  if (isPending) return <ActivityIndicator />;
  if (error) return <Text>加载失败</Text>;
  if (!data) return <Text>暂无数据</Text>;

  return (
    <View className="bg-surface dark:bg-surface-dark">
      <Text className="text-text dark:text-text-dark">{data.createdAt}</Text>
    </View>
  );
}
```

**步骤 7：写路由薄壳**

```tsx
// app/<x>.tsx 或 app/(tabs)/<x>.tsx
export { default } from '@features/<X>/screens/<X>Screen';
```

**步骤 8：注册到 `_layout.tsx`（非 tab 路由）**

```tsx
// app/_layout.tsx
<Stack.Screen name="<x>" options={MODAL_OPTIONS} />
```

**步骤 9：质量检查**

```bash
yarn lint
yarn test
yarn arch              # dependency-cruiser
```

**步骤 10：提交**

```
feat(<X>): 新增 <X> 模块 + 屏幕骨架

- screens/<X>Screen.tsx 编排 useXQuery
- api/<X>.api.ts 暴露 fetchX
- hooks/use-<X>.ts 包 useQuery + DTO → ViewModel 映射
- app/<x>.tsx 一行 re-export 壳
```
