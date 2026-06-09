# Web SPA — 路径别名

## 推荐

Web 使用单别名：

```json
{
  "paths": {
    "@/*": ["src/*"]
  }
}
```

Vite / Webpack 与 TypeScript 保持一致。

## 使用

- feature 内部使用相对路径。
- 跨 feature 只 import 对方顶层 barrel：`@/features/order`。
- shared、providers、app 可用 `@/shared/...` 等绝对路径。

## 禁止清单

- feature 内部用 `@/features/<self>/...`。
- Web 跨 feature 深路径。
- 多套 alias 配置不同步。
