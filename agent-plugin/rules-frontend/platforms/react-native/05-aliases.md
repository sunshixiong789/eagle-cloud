# React Native — 路径别名

## 推荐别名

- `@app/*` -> `app/*`
- `@features/*` -> `src/features/*`
- `@shared/*` -> `src/shared/*`
- `@providers/*` -> `src/providers/*`
- `@infra/*` -> `src/infrastructure/*`

`tsconfig.json`、`babel.config.js`、`jest.config.js` 必须同步。

## 使用

- feature 内部用相对路径。
- RN 禁用 barrel，不 import `@features/order` 顶层。
- 跨 feature 依赖先评估 shared / Screen 编排；确需依赖时显式到公开约定文件。

## 禁止清单

- alias 配置不同步。
- feature 内部绝对路径自引。
- Web 式 barrel 策略套到 RN。
