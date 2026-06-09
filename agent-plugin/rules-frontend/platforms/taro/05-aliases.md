# Taro — 路径别名

## 推荐别名

- `@features/*` -> `src/features/*`
- `@shared/*` -> `src/shared/*`
- `@providers/*` -> `src/providers/*`
- `@infra/*` -> `src/infrastructure/*`

## 同步要求

`tsconfig.json` 与 `config/index.ts` 必须双向同步；启用 Jest 时测试解析也要同步。

## 使用

- feature 内部用相对路径。
- Taro 禁用 barrel，不 import `@features/order` 顶层。
- 跨 feature 依赖先评估 shared / Screen 编排；确需依赖时显式到公开约定文件。

## 禁止清单

- alias 只改 TS 不改 Taro webpack。
- feature 内部绝对路径自引。
- Web 式 barrel 策略套到 Taro。
