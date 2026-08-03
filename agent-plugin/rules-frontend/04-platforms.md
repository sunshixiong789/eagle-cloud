# 三端差异对照（Web / React Native / Taro）

业务结构三端统一，见 `01-architecture.md`。本文只记**差异**。先用 `00-overview.md` 的平台判断确定轨道，再读对应列。

## 速查总表

| 维度 | Web SPA | React Native / Expo | Taro 小程序 |
|---|---|---|---|
| 路由 shell | `src/app/router.tsx` | 根 `app/`（Expo Router） | `src/pages/` + `src/app.config.ts` |
| shell 形态 | `useRoutes` + `React.lazy` | 一行 `export { default } from ...` | `index.tsx` 只 re-export |
| Barrel | **可用**顶层 barrel | **禁用** | **禁用** |
| 跨 slice | 只 import 顶层 public API，禁深路径 | 显式深路径到公开约定文件 | 同 RN |
| 别名 | 单别名 `@/*` | 多别名 `@features/*` 等 | 多别名 `@features/*` 等 |
| 别名同步点 | Vite/Webpack + tsconfig | tsconfig + babel + jest | tsconfig + `config/index.ts` |
| 样式 | Tailwind + CSS-in-JS | NativeWind | Tailwind v4 + weapp-tailwindcss |
| 平台适配层 | analytics / SW / IndexedDB | SecureStore / permissions / push | `Taro.*` / `wx.*` / `my.*` |

---

## 目录差异

三端共用 `src/{app,pages,widgets,features,entities,shared,infrastructure}`。额外项：

**Web**
- `src/styles/`：Tailwind 入口、字体、reset —— **不放业务样式**

**React Native**
- 根 `app/`：Expo Router shell，**不迁到 `src/app/`**
- `src/app/` 仍是 bootstrap / providers

**Taro**
- `src/app.config.ts`：全局页面注册和窗口配置
- `src/pages/<route>/index.tsx`：框架页面入口，只 re-export 业务 Page
- 页面级 `index.config.ts` 放 `pages` 同级目录
- 业务 Page 层可放 `src/pages-fsd/` 或 `features/*/pages/` —— 因为 `src/pages/` 已被框架占用。项目二选一并保持一致
- `config/index.ts` 的 compiler 配置与实际构建链保持同步

---

## 路由差异

三端共同规则：**shell 只声明路由，Page/Screen 负责解析 params、导航和多 feature 编排，Component 不直接导航**。

**Web**
- 集中在 `src/app/router.tsx`，`useRoutes` + `React.lazy`
- Guard 用普通组件包裹 element（`AuthGuard` / `PermissionGuard`），**不与 React Router data API 的 loader/action 混用**
- 404 放最后
- 禁止：同一路径在 router、menu、权限配置写出不同字符串

**React Native**
- 根 `app/` 文件式路由，路由文件是一行薄壳：
  ```ts
  export { default } from '@features/x/screens/XScreen';
  ```
- `_layout.tsx` 放 Provider、Stack/Tabs、全局 deep link 处理
- 少数项目用集中式 React Navigation 也可，仍保持 Screen 编排
- 禁止：把根 `app/` 迁到 `src/app/`；跨 Screen 共享导航状态（用 URL params 或持久 store）

**Taro**
- 页面必须在 `app.config.ts` 注册，分包页面在 `subPackages` 注册
- **主包不要 import 分包内部代码**
- 禁止：漏注册；页面入口写业务逻辑；tabBar 页用 `navigateTo`；跨端路由参数格式不统一

---

## 样式差异

三端共同规则：**同一元素同一属性不要 className 与 style 双写**；主题色走 token / className / 平台主题系统，**不用 Tailwind 任意值色**（`bg-[#xxx]`）作业务主题色；全局样式文件只放字体和 reset。

**Web**
| 手段 | 用途 |
|---|---|
| Tailwind / UnoCSS | 布局、间距、尺寸、排版等无主题感知样式 |
| CSS-in-JS | 主题 token、颜色、阴影、圆角、状态变体 |
| CSS Modules | 仅遗留组件或第三方覆写，新代码不用 |

组件配套样式命名 `<Name>.style.ts`，与组件同目录。禁止 feature 内写全局 CSS、业务组件直接读 theme store 拼颜色。

**React Native（NativeWind）**
| 手段 | 用途 |
|---|---|
| `className` | 布局、间距、字号、主题色 |
| `theme.X` | 只传给 RN 原生 prop（StatusBar、Navigation、图表库颜色） |
| `style` | 仅动态数值、transform、absolute 坐标、复杂 RN 专有样式 |

`nwColorScheme.set()` 传原始 mode，**不传 resolved 后的 light/dark**。禁止 `style={{ backgroundColor: theme.surface }}` 写主题色。

**Taro**
- 布局、间距、字号优先 className
- H5 用 `dark:`；小程序端结合 `page-meta` 和平台暗色能力
- 单位跟随项目既有 px/rpx 转换策略，**不在同一组件混用无解释的 px/rpx**
- 禁止忘记同步 Taro compiler / PostCSS / weapp-tailwindcss 配置

---

## 别名差异

三端共同规则：**slice 内部用相对路径**，禁止绝对路径自引；跨 slice 只走 public API；**多处配置必须同步**。

**Web** —— 单别名
```json
{ "paths": { "@/*": ["src/*"] } }
```
跨 slice：`@/features/order`、`@/entities/product`。**禁止深路径**。

**React Native / Taro** —— 多别名
```text
@route-app/*  -> app/*            (仅 RN)
@app/*        -> src/app/*
@pages/*      -> src/pages/*
@widgets/*    -> src/widgets/*
@features/*   -> src/features/*
@entities/*   -> src/entities/*
@shared/*     -> src/shared/*
@infra/*      -> src/infrastructure/*
```
- RN 同步点：`tsconfig.json` + `babel.config.js` + `jest.config.js`
- Taro 同步点：`tsconfig.json` + `config/index.ts`（双向同步），启用 Jest 时测试解析也要同步
- **禁用 barrel**，不要 import `@features/order` 顶层 barrel

---

## 新增业务清单

**三端通用步骤**：

1. **先判断层级**：页面编排进 `pages`，用户动作进 `features`，稳定业务对象进 `entities`，大块组合进 `widgets`，无业务归属进 `shared`
2. 新 feature 建**最小目录**：`api`、`queries`，按需再补 `components/hooks/stores/lib`
3. DTO 放 `api`，ViewModel / UI 类型放 `types.ts` 或由 hook 返回值推断
4. 如需跨 slice 暴露能力，维护 public API named exports
5. 补映射、状态、权限、错误处理、测试

**平台专属步骤**：

| 平台 | 页面接入 | 验证方式 |
|---|---|---|
| Web | 新 page 放 `src/pages/<page>/`，在 `src/app/router.tsx` lazy 引入 | 浏览器验证 |
| RN | 新 screen 放 `src/pages/<page>/`，根 `app/` 路由文件一行 re-export；原生权限/存储/push/secure token 走 `infrastructure/` | 模拟器 / 真机验证关键路径 |
| Taro | 在 `app.config.ts` 注册（分包进 `subPackages`），`src/pages/<route>/index.tsx` re-export；多端差异代码放 `infrastructure/` 或条件编译文件 | 小程序开发者工具 + H5 双端验证 |

**禁止**复制其他 slice 后大面积改名 —— 先建最小骨架，再按需求补目录。
