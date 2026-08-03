# 前端工程规范（入口）

适用 Web SPA / React Native(Expo) / Taro 小程序三端。业务结构统一，差异只在平台 shell、路由、样式、原生 API、别名。

## 文件索引

| 文件 | 内容 |
|---|---|
| `01-architecture.md` | FSD-lite 分层、角色矩阵、依赖方向、slice 边界与 public API |
| `02-conventions.md` | 文件/函数命名、slice 词根、import 规则、DTO/ViewModel/Props 类型三层 |
| `03-state-data.md` | React Query / Zustand / local state 边界、错误与 401、主题、副作用、i18n |
| `04-platforms.md` | **三端差异对照**：目录、路由、样式、别名、新增业务清单 |
| `05-quality.md` | 红线、反例速查、测试、性能预算、依赖校验、扩展信号 |

只读当前任务相关的文件，不要一次性展开全部。

## 平台判断

| 特征 | 平台轨 |
|---|---|
| 有 `vite.config.ts` / 普通 `webpack.config.js`，且非 Taro | **Web** |
| 有 Expo Router 根 `app/`，或 RN 工程 | **React Native** |
| 有 `config/index.ts`、`app.config.ts`、`@tarojs/*` 依赖 | **Taro** |

## 架构定位

参考 Feature-Sliced Design 的分层与 public API 思想，但**不完整照搬** —— 采用业务优先的 FSD-lite：

```text
app / pages / widgets / features / entities / shared / infrastructure
```

**不要为了符合层名而拆目录。** `entities`、`widgets` 按业务复杂度引入；轻业务只用 `app / pages / features / shared / infrastructure` 就够。

## TL;DR

1. 平台路由文件是**薄壳**，业务在 `src/pages` 或 feature 页面入口。
2. Component 只渲染，不请求 API、不导航、不读写 token。
3. API/Service 不 import React runtime，也不反向依赖 feature。
4. 跨 slice 不深 import 内部模块，只走 public API。
5. 服务端状态进 React Query；跨页持久状态进 Zustand；表单/局部状态留组件。
6. DTO 镜像后端契约，ViewModel 面向 UI，**DTO 字段名不得出现在 JSX 里**。
7. HTTP 客户端单例，401 与 token 注入统一处理。
8. 主题模式保存 `system / light / dark` 原始值。
9. 同一元素同一属性不要 className 与 style 双写。
10. 新功能先判断该进哪一层，再查 `04-platforms.md` 对应平台的新增清单。
