# AI Agent 前端工程规范（入口）

本目录是前端规则索引。只按项目栈读取对应文件，不一次性展开全部规则。

## 平台轨

- Web SPA：`platforms/web/`
- React Native / Expo：`platforms/react-native/`
- Taro / 小程序：`platforms/taro/`

差异只在平台 shell、路由、样式、原生 API、别名配置。业务结构参考 Feature-Sliced Design 的分层思想，但采用业务优先的
FSD-lite：`app / pages / widgets / features / entities / shared / infrastructure`。

这不是完整照搬 FSD：不要为了符合层名而拆目录。`entities`、`widgets` 按业务复杂度引入；轻业务可以只用
`app / pages / features / shared / infrastructure`。

## 必读核心

- `core/01-architecture.md`：FSD-lite 分层、依赖方向、平台 shell。
- `core/02-naming.md`：命名、feature 词根、import 规则。
- `core/03-types.md`：DTO / ViewModel / Component Props 三层。
- `core/04-state.md`：React Query / Zustand / local state 边界。
- `core/05-bounded-context.md`：slice 边界、public API、barrel 策略。
- `core/06-cross-cutting.md`：错误、401、主题、副作用、全局 listener。
- `core/08-red-lines.md`：TypeScript、状态、HTTP、样式、包管理红线。

按需读取：`core/10-i18n.md`、`core/11-testing.md`、`core/12-performance.md`、`99-dependency-check.md`。

## TL;DR

1. 平台路由文件是薄壳，业务在 `src/pages` 或 feature 页面入口。
2. Component 只渲染，不直接请求 API、不导航、不读写 token。
3. API/Service 不 import React，也不反向依赖 feature。
4. 跨 slice 不直接 import 内部模块；只能走 public API。
5. 服务端状态进 React Query；跨页持久状态进 Zustand；表单/局部状态留在组件。
6. DTO 镜像后端契约，ViewModel 面向 UI。
7. HTTP 客户端单例，401 和 token 注入统一处理。
8. 主题模式保存 `system / light / dark` 原始值。
9. 样式不要 className 与 style 双写。
10. 新功能先判断放 `pages / widgets / features / entities / shared` 哪一层，再读对应平台 `04-new-feature.md`。

## 平台判断

- 有 `vite.config.ts` / 普通 `webpack.config.js`，不是 Taro：读 `platforms/web/`。
- 有 Expo Router 根 `app/` 或 RN 工程：读 `platforms/react-native/`。
- 有 `config/index.ts`、`app.config.ts`、Taro 依赖：读 `platforms/taro/`。
