# Taro — 路径别名

## 推荐别名

- `@app/*` -> `src/app/*`
- `@pages/*` -> `src/pages-fsd/*` 或项目约定的业务 Page 层
- `@widgets/*` -> `src/widgets/*`
- `@features/*` -> `src/features/*`
- `@entities/*` -> `src/entities/*`
- `@shared/*` -> `src/shared/*`
- `@infra/*` -> `src/infrastructure/*`

## 同步要求

`tsconfig.json` 与 `config/index.ts` 必须双向同步；启用 Jest 时测试解析也要同步。

## 使用

- slice 内部用相对路径。
- Taro 禁用 barrel，不 import `@features/order` 顶层 barrel。
- 跨 slice 依赖先评估 shared / entities / Page 编排；确需依赖时显式到公开约定文件。

## 禁止清单

- alias 只改 TS 不改 Taro webpack。
- slice 内部绝对路径自引。
- Web 式 barrel 策略套到 Taro。
