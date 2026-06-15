# 反例速查（通用）

依次扫一遍，命中即重构：

1. **Component 内出现 `useQuery` / `useMutation`** → 上提到 Page 或包成 `hooks/use-*` / `queries/use-*`
2. **Service / API 文件出现 `import { useXxx } from 'react'` 或 JSX**
   - 例外：`import type` 总是允许；平台运行时 API（`Platform`、`NativeModules`、`Taro`、`wx`、`window`）允许
3. **JSX 里出现 `data.created_at` / `data.commission_rate_start` 这类 DTO 原名** → Hook/Query 缺映射（详见 `03-types.md`）
4. **一个 hook / query 文件返回 10+ 字段** → 拆 `useXxxQuery` + `useXxxMutation` + `useXxxDerived`
5. **同一 feature 的逻辑分散在多目录但词根不一致** → 重命名对齐
6. **Store action 内有 fetch** → 移到 hook / mutation 的 `onSuccess` 触发 store action
7. **`@shared/api/http.ts` 顶部出现 `import { useXxxStore } from '@shared/stores/...'` 或 `import ... from '@features/...'`** → 走依赖反转（`configureHttp` / `setTokenGetter`）
8. **路由文件超过 1 行**（`app/<route>.tsx` / `router.tsx` 中的 lazy import 元素） → 应当是 `export { default } from '@features/.../screens/...'` 或 `React.lazy(...)` 一行薄壳
9. **Hook 内出现 `queryClient.invalidateQueries(['<其他 feature>', ...])`** → 移到 Page
10. **feature 内部用 `@features/<self>/...` 绝对路径** → 改相对路径（边界检查会拦）
11. **跨 feature 用深路径**（Web）或 **用 barrel**（RN/Taro） → 按 `05-bounded-context.md` 平台策略改
12. **用 `import { Foo }` 引入只作为类型的导出** → 改为 `import type { Foo }`，避免运行时副作用与循环依赖
13. **在 feature 里新建 `context/` / `guard/` / `utils/` 这种非约定子目录** → 用 `components/` / `lib/` 容纳
14. **同一元素同一属性同时写 className + `style={{ }}`** → 选一种（详见 `08-red-lines.md` §样式）
15. **Zustand 全量订阅** `const store = useXxxStore()` → 改 selector `useXxxStore(s => s.x)`
16. **表单 state 进了 Zustand** → 改 `useState` 或表单库
