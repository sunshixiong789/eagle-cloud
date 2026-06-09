# 状态三分（通用）

## 决策树

```
有数据吗？
├─ 来自服务端（API / WebSocket）→ React Query
└─ 来自客户端
   ├─ 跨页面 / 跨重启需要保留 → Zustand + persist
   └─ 仅当前组件需要         → useState
```

**反例**：
- 表单输入用 Zustand（应当 `useState`，组件卸载即释放）
- 列表筛选只放本地 `useState`，导致返回再进丢状态（跨页面 → Zustand 或 URL params）
- 服务端数据进 Zustand（重复缓存，丢 React Query 的 invalidate 能力）

## React Query 配置基线

在 `@providers/QueryProvider.tsx` 一次性设好，特殊场景才覆盖：

| 配置 | Web 推荐值 | RN / Taro 推荐值 |
|---|---|---|
| `staleTime` | 5 min | 60 s（移动端网络更不稳定，刷新更勤） |
| `gcTime` | 15 min | 5 min（节省内存） |
| `refetchOnWindowFocus` | `false` | RN/Taro 无 window 概念，自然 false |
| `retry`（query） | 1 | 1 |
| `retry`（mutation） | `false` | `false` |
| query key | 元组 `['users', filters]` | 同 |

## React Query key 公约

- **元组形式**：`['users', filters]`、`['order', orderId]`。**禁止**字符串 `'users:list:filter1'`。
- **集中管理**（推荐 Web、可选 RN/Taro）：feature 内 `queries/<f>.keys.ts` 集中导出 keyFactory：

```ts
// queries/user.keys.ts
export const userKeys = {
  all: ['users'] as const,
  list: (filter: UserFilter) => [...userKeys.all, 'list', filter] as const,
  detail: (id: string) => [...userKeys.all, 'detail', id] as const,
};

// queries/user.queries.ts
import { userKeys } from './user.keys';
useQuery({ queryKey: userKeys.detail(id), queryFn: () => fetchUser(id) });
```

理由：重命名 key 时单点改；invalidate 时不用全文 grep。

---

## 跨 feature invalidation 模式

A 的 mutation 完成后让 B 的 query 失效：**invalidate 操作在 Page 编排，不在 hook 内**。

❌ **错误**：A 的 hook 内 `queryClient.invalidateQueries({ queryKey: ['<feature-B>', ...] })` —— hook 知道了 B 的 query key，违反 bounded context。

✅ **正确**：

```tsx
// XPage.tsx
import { useQueryClient } from '@tanstack/react-query';
import { useFeatureAMutation } from '../queries/featureA.queries';
import { featureBKeys } from '@features/featureB/queries/featureB.keys';  // ← 仅类型与 key 跨界

export default function XPage() {
  const queryClient = useQueryClient();
  const mutate = useFeatureAMutation();

  const onSubmit = async (form) => {
    await mutate.mutateAsync(form);
    queryClient.invalidateQueries({ queryKey: featureBKeys.list({}) });
    queryClient.invalidateQueries({ queryKey: featureBKeys.summary() });
  };

  return <Form onSubmit={onSubmit} />;
}
```

Page 是编排者，可以同时知道 A 与 B 的 query key。

详见 `05-bounded-context.md`。

---

## Zustand 使用规范

### Selector 模式（强制）

```ts
// ✅ 正确
const userId = useUserStore(s => s.userId);

// ❌ 错误：全量订阅，任何字段变化都触发 re-render
const store = useUserStore();
```

### Persist hydration 标记

跨页 + 持久化 store 必须有 `_hasHydrated` 标记，UI 在未 hydrated 前展示 fallback（否则首帧拿默认值闪烁）：

```ts
const useUserStore = create<UserState>()(
  persist(
    (set) => ({
      user: null,
      _hasHydrated: false,
      setHydrated: () => set({ _hasHydrated: true }),
      setUser: (user) => set({ user }),
    }),
    {
      name: 'user-storage',
      storage: createJSONStorage(() => storage),
      onRehydrateStorage: () => (state) => state?.setHydrated(),
    }
  )
);

// 使用方
const hasHydrated = useUserStore(s => s._hasHydrated);
if (!hasHydrated) return <SplashScreen />;
```

### 表单 state 不进 Zustand

临时输入、modal 草稿、列表筛选 → 用 `useState` 或表单库（react-hook-form / antd Form）。
理由：表单生命周期跟组件绑定；进 store 会污染全局快照。

### Store action 不在 store 内 fetch

```ts
// ❌ 错误：store 内直接调 API
const useStore = create((set) => ({
  user: null,
  loadUser: async () => {
    const data = await fetchUser();   // 越权
    set({ user: data });
  },
}));

// ✅ 正确：mutation 触发 store action
const mutation = useMutation({
  mutationFn: fetchUser,
  onSuccess: (data) => useUserStore.getState().setUser(data),
});
```

---

## 不用 React Context 管业务 state

- 跨页持久 → Zustand
- 组件树上下文（ThemeProvider / AuthProvider 这种 framework-level）→ Context only when really needed

**理由**：Context 全树重渲染、没有 selector；Zustand 默认就比 Context 更高效。
