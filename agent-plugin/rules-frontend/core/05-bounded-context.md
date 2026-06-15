# Slice 边界与 Public API

每个 `pages / widgets / features / entities` 下的 slice 默认只暴露 public API，内部目录不可被外部深 import。

## 跨 slice 协作

- 可复用且无业务归属：上移 `shared/`。
- 页面级组合：由 Page 同时编排多个 feature/entity。
- 稳定业务对象：下沉到 `entities/<entity>`。
- 真正需要跨 slice 复用：通过该 slice public API 暴露，并保持 named exports。

## Public API 策略

- Web：可用 slice 顶层 barrel，跨 slice 只 import 顶层 public API。
- React Native / Taro：默认禁用 barrel；public API 可用明确文件路径表达，避免 Metro/Taro 解析和 tree-shaking 问题。
- 禁止 `export *`；只允许 named exports 明确公开能力。

## 禁止共享

- feature 内部 store、service、私有 hook、私有 component。
- 为复用一个函数深 import 其他 slice 内部文件。
- shared 反向依赖 pages / widgets / features / entities。
