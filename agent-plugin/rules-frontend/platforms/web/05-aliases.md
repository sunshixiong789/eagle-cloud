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
- 跨 slice 只 import 对方 public API：`@/features/order`、`@/entities/product`。
- `app`、`pages`、`widgets`、`features`、`entities`、`shared`、`infrastructure` 可用 `@/...` 绝对路径。

## 禁止清单

- slice 内部用 `@/<layer>/<self>/...` 绝对路径自引。
- Web 跨 slice 深路径。
- 多套 alias 配置不同步。
