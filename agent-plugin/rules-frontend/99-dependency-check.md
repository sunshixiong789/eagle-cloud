# 依赖校验工具

前端架构规则应尽量落到 CI。优先使用 `dependency-cruiser`；现有项目已用 ESLint boundaries 或自写脚本时沿用。

## 必校验

- 循环依赖为 0。
- `shared/api/http` 不 runtime import `features`、`providers`、`app`。
- API / Service 文件不 runtime import React，只允许 `import type`。
- 跨 feature 不能直接 import 对方内部模块。
- feature 内部不要用绝对路径引用自己。

## 平台差异

- Web：跨 feature 必须走对方 feature 顶层 barrel，例如 `@/features/order`；禁止深路径。
- React Native / Taro：禁用 barrel，跨 feature 如确需依赖必须显式深路径到公开约定文件，并优先评估上移 shared 或 Page 编排。

## 推荐工具

```bash
yarn add -D dependency-cruiser
yarn depcruise --validate .dependency-cruiser.cjs src
```

CI 至少在 PR 跑依赖检查、类型检查、lint 和单元测试。

## 最小规则清单

```js
module.exports = {
  forbidden: [
    { name: 'no-circular', severity: 'error', from: {}, to: { circular: true } },
    {
      name: 'shared-api-no-feature-runtime',
      severity: 'error',
      from: { path: '^src/shared/api/' },
      to: { path: '^src/features/' },
    },
    {
      name: 'api-no-react-runtime',
      severity: 'error',
      from: { path: '(^|/)(api|service|services)/.*\\.(ts|tsx)$' },
      to: { path: '^node_modules/react($|/)' },
    },
  ],
};
```

项目规则更复杂时，把具体正则放在项目自己的 `.dependency-cruiser.cjs`，本文件只作为校验方向。
