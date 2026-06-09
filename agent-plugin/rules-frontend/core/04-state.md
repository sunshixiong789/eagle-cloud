# 状态边界

## 决策

- 服务端状态：React Query。
- 当前组件临时状态：`useState` / `useReducer`。
- 跨页、跨会话、持久化 UI 状态：Zustand + persist。

禁止把服务端数据复制进 Zustand；禁止把表单状态放进全局 store。

## React Query

- query key 使用元组或 feature 内 key factory：`['orders', filters]`，不要字符串拼接 key。
- mutation 成功后由 Page/Hook 编排 invalidate；跨 feature invalidation 优先通过公开 key factory 或 Page 编排。
- Web 可按交互设置 `staleTime`；RN/Taro 注意前后台恢复和网络状态。

## Zustand

- 必须使用 selector：`useStore(s => s.value)`，不要全量订阅。
- persist store 必须有 `_hasHydrated` 或等价 hydration 标记，首帧避免默认值闪烁。
- store action 只改本地状态，不在 store 内 fetch；异步数据由 React Query 管。

## React Context

不使用 Context 管业务状态。Context 只用于 ThemeProvider、QueryProvider、平台适配器等 framework-level 注入。
