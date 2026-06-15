# React Native — 路径别名

## 推荐别名

- `@route-app/*` -> `app/*`
- `@app/*` -> `src/app/*`
- `@pages/*` -> `src/pages/*`
- `@widgets/*` -> `src/widgets/*`
- `@features/*` -> `src/features/*`
- `@entities/*` -> `src/entities/*`
- `@shared/*` -> `src/shared/*`
- `@infra/*` -> `src/infrastructure/*`

`tsconfig.json`、`babel.config.js`、`jest.config.js` 必须同步。

## 使用

- slice 内部用相对路径。
- RN 禁用 barrel，不 import `@features/order` 顶层 barrel。
- 跨 slice 依赖先评估 shared / entities / Page 编排；确需依赖时显式到公开约定文件。

## 禁止清单

- alias 配置不同步。
- slice 内部绝对路径自引。
- Web 式 barrel 策略套到 RN。
