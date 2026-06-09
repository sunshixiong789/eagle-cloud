# Taro — 新增 feature 操作清单

> 写一个全新 feature `<X>`（比如 `cart`）的步骤。

**步骤 1：定词根** —— 按 `core/02-naming.md` 选词根，在 `CLAUDE.md` 业务模块清单追加一行。

**步骤 2：建目录**

```bash
mkdir -p src/features/<X>/{api,screens}
mkdir -p src/features/<X>/{queries,stores,components}   # 按需
mkdir -p src/pages/<X>                                  # 框架入口
```

> **不建** `src/features/<X>/index.ts`（barrel 禁用）。

**步骤 3：写 API（包 Taro.request）**

```ts
// src/features/<X>/api/<X>.api.ts
import { api } from '@shared/api/http';

export interface XResponse {
  id: number;
  created_at: string;
}

export async function fetchX(id: number): Promise<XResponse> {
  return api.get<XResponse>(`/x/${id}`);
}
```

**步骤 4：写 Query**

```ts
// src/features/<X>/queries/<X>.keys.ts
export const xKeys = {
  all: ['x'] as const,
  detail: (id: number) => [...xKeys.all, 'detail', id] as const,
};

// src/features/<X>/queries/<X>.queries.ts
import { useQuery } from '@tanstack/react-query';
import { fetchX } from '../api/<X>.api';
import { xKeys } from './<X>.keys';

export interface XViewModel {
  id: number;
  createdAt: string;
}

export function useXQuery(id: number) {
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

**步骤 6：写 Screen（业务实际入口）**

```tsx
// src/features/<X>/screens/<X>Screen.tsx
import { View, Text } from '@tarojs/components';
import { useXQuery } from '../queries/<X>.queries';

export default function XScreen() {
  const { data, isPending, error } = useXQuery(123);

  if (isPending) return <View><Text>加载中...</Text></View>;
  if (error) return <View><Text>加载失败</Text></View>;
  if (!data) return <View><Text>暂无数据</Text></View>;

  return (
    <View className="flex flex-col p-4">
      <Text className="text-base text-gray-800">{data.createdAt}</Text>
    </View>
  );
}
```

**步骤 7：写框架入口（1 行薄壳）**

```tsx
// src/pages/<X>/index.tsx —— 1 行 re-export
export { default } from '@features/<X>/screens/<X>Screen';
```

```ts
// src/pages/<X>/index.config.ts —— 页面级配置
export default definePageConfig({
  navigationBarTitleText: '<X> 页面',
});
```

**步骤 8：注册到 `app.config.ts`**

```ts
// src/app.config.ts
export default defineAppConfig({
  pages: [
    'pages/index/index',
    'pages/product/index',
    'pages/<X>/index',           // ← 加这一行
  ],
});
```

如果是 tabBar 页面，同时在 `tabBar.list` 中加：

```ts
tabBar: {
  list: [
    // ...
    { pagePath: 'pages/<X>/index', text: '<X>', iconPath: '...', selectedIconPath: '...' },
  ],
},
```

**步骤 9：多端检查**

```bash
yarn dev:h5              # H5 端验证
yarn dev:weapp           # 微信小程序端验证
yarn lint                # ESLint
yarn lint:style          # Stylelint
yarn taro doctor         # Taro 环境检查
```

至少在**目标主端**（如微信小程序）开发者工具里跑通；H5 端可用浏览器验证大致逻辑。

**步骤 10：提交**

```
feat(<X>): 新增 <X> 模块骨架

- screens/<X>Screen.tsx 编排 useXQuery
- api/<X>.api.ts 暴露 fetchX
- queries/<X>.queries.ts 包 useQuery + DTO → ViewModel
- pages/<X>/index.tsx 一行 re-export 壳 + index.config.ts 标题
- app.config.ts 注册 pages/<X>/index
```

---

## 常见漏项

1. **漏注册 `app.config.ts` 的 `pages`** → 编译过但运行时 `navigateTo` 找不到页面
2. **`src/pages/<X>/index.tsx` 写了业务** → Screen 在 `@features/<X>/screens/` 才对
3. **漏建 `index.config.ts`** → 页面顶部 navigation bar 会用 app 默认
4. **tabBar 页面用了 `Taro.navigateTo`** → 必须用 `Taro.switchTab`
5. **新增 `TARO_APP_XXX` env 变量未同步 `types/global.d.ts`** → 取值类型不可知，类型检查松懈漏 bug
