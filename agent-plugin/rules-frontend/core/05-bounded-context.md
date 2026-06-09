# Bounded Context（通用）+ Barrel 平台策略

**每个 feature 的内部模块（api / queries / stores / lib / hooks / components）禁止被其他 feature 直接 import**。

跨 feature 数据流动**只有两条合法路径**：

## 路径一：上移到 Shared Kernel

把需要被多 feature 共享的能力放到 `@/shared/`：

| 共享内容 | 上移目的地 |
|---|---|
| 通用 HTTP / token / 错误处理 | `@shared/api/` |
| 通用 hook（color-scheme、theme、debounce） | `@shared/hooks/` |
| 通用 store（user、theme、modal 队列） | `@shared/stores/` |
| 通用展示组件 | `@shared/ui/` 或 `@shared/components/` |
| 平台原生适配 | `@infra/`（如有） |

## 路径二：Page 编排

A 的 Page 同时调 A 的 hook + B 的 hook（UI 层负责编排，hook 之间不互相依赖）。跨 feature query invalidation 也走 Page。

```tsx
// 不推荐：A 的 hook 内 import B 的 query key
function useAComplexFlow() {
  const queryClient = useQueryClient();
  // ...
  queryClient.invalidateQueries({ queryKey: ['B', ...] });  // hook 知道 B 的实现 ❌
}

// 推荐：Page 把两个 feature 缝起来
function APage() {
  const { mutate } = useAMutation();
  const queryClient = useQueryClient();

  const onSubmit = async (form) => {
    await mutate(form);
    queryClient.invalidateQueries({ queryKey: bKeys.list() });  // Page 知道两边 ✅
  };
}
```

---

## 反例

```ts
// ❌ A 的 hook 顶部 import B 的内部 API
// @features/A/hooks/use-a.ts
import { fetchB } from '@features/B/api/b.api';

// ❌ A 的 store 内调用 B 的 store
// @features/A/stores/a.store.ts
import { useBStore } from '@features/B/stores/b.store';
```

**修复**：
- 如果是数据查询 → 让 A 的 Page 同时调 A、B 的 query
- 如果是共享逻辑 → 上移到 `@shared/`
- 如果两个 feature 真的强耦合 → 它们本来就是**同一个 bounded context**，应当合并成一个 feature

---

## 特例：合并 vs 拆分

当两个 feature 强耦合到无法用上述两条化解时，说明它们本来就是同一个 context，**应当合并**。例如发现两个"feature"共享同一组 hook、store、组件——那就是一个 context，`.agents/PROJECT.md` 里只该有一个词根。

---

## Barrel 平台策略

`@features/<f>/index.ts` 是否作为 barrel 暴露 feature 的对外 API？三个平台答案不同——这是构建器决定的。

### Web → Barrel **启用**

```ts
// @features/order/index.ts
export { useOrderListQuery, useOrderDetailQuery } from './queries/order.queries';
export type { OrderViewModel } from './queries/order.queries';
// pages/ 不必从 barrel 导出 —— router.tsx 直接 lazy import
```

跨 feature 引用：

```ts
// ✅ 走 barrel
import { useOrderListQuery } from '@/features/order';

// ❌ 禁止深路径
import { useOrderListQuery } from '@/features/order/queries/order.queries';
```

**理由**：Vite / Webpack 5 对 barrel 的 tree-shaking 成熟。Barrel 形成清晰的「公开 API 面」，重构内部不影响消费方。

### React Native → Barrel **禁用**

```ts
// @features/order/index.ts  ❌ 不建此文件
```

跨 feature 引用：

```ts
// ✅ 直接路径
import { useOrderListQuery } from '@features/order/queries/order.queries';
```

**理由**：Metro bundler 对 barrel 的 tree-shaking 不友好；常引起循环依赖与首屏体积膨胀。

### Taro → Barrel **禁用**（与 RN 一致）

虽然 Taro 4 用 Webpack 5，但因为：

1. 编译目标同时含小程序（构建器对 tree-shaking 支持差）
2. weapp 包大小限制严格（主包 2MB / 分包 20MB）
3. Taro 自身约定与 RN 风格一致（`screens/` 而非 `pages/`）

跨 feature 同样走直接路径：

```ts
// ✅ 直接路径
import { useProductListQuery } from '@features/product/queries/product.queries';
```

### 哪怕启用 barrel，也禁止 `export *`

```ts
// ❌ 禁止：把所有内部都 re-export
export * from './api/order.api';

// ✅ 必须：明确 named exports
export { useOrderListQuery, useOrderCreateMutation } from './queries/order.queries';
export type { OrderViewModel } from './queries/order.queries';
```

理由：`export *` 把内部所有命名都暴露成公开 API；任何内部命名冲突或 rename 都会破坏消费方。

---

## 唯一允许跨 feature 共享的内容

无论平台，**`queries/<f>.keys.ts` 中的 key factory 可以被其他 feature 引用**（用于在 Page 编排时 invalidate）：

```ts
// @features/B/queries/b.keys.ts
export const bKeys = { all: ['B'] as const, list: () => [...bKeys.all, 'list'] as const };

// APage.tsx 编排时引用
import { bKeys } from '@features/B/queries/b.keys';
queryClient.invalidateQueries({ queryKey: bKeys.list() });
```

key 是不可变的字符串元组，属于"轻量契约"，不算"内部实现"。
