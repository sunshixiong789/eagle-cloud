# Bounded Context 与 Barrel 策略

每个 feature 的 `api / queries / stores / lib / hooks / components` 默认是内部实现，其他 feature 不直接 import。

## 跨 feature 协作

- 可复用且无业务归属：上移 `shared/`。
- 页面级组合：由 Page/Screen 同时调用多个 feature 的公开 hook/component。
- 真正业务依赖：抽成明确的公开 API，并在 feature 边界文档化。

禁止为复用一个函数就深 import 对方内部文件。

## Barrel 策略

- Web：允许 feature 顶层 barrel，跨 feature 只能 import 顶层 `@/features/<name>`。
- React Native / Taro：禁用 barrel，避免 Metro/Taro 解析和 tree-shaking 问题。
- 即使启用 barrel，也禁止 `export *`，只能 named export 明确公开 API。

## 唯一可共享内容

- 公开 component / hook / query key factory / type。
- 无业务归属的 pure util、design token、基础 UI。
- 不共享 feature 内部 store、service、私有 hook、私有 component。
