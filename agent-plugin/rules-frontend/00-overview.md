# AI Agent 前端工程规范（总入口）

> 面向 AI 编程助手（Claude Code / Codex / Cursor / Gemini / Copilot）的**通用前端规范**。
>
> 拆分为两层：
> - **`core/`** —— 跨平台**通用**架构与纪律（FSD、命名、类型分层、状态边界、横切、反例、红线、扩展）。**所有前端项目都要读**。
> - **`platforms/`** —— **平台特有**的目录、路由、样式、新增 feature 步骤、路径别名。**按项目栈读对应一套**。
>
> 项目层入口文档（`CLAUDE.md` / `AGENTS.md` / `.agents/PROJECT.md`）只放摘要 + 命令速查 + 当前 feature 词根表、token 列表、品牌色。详细规范一律以本套规则为准。

**架构风格**：Feature-Sliced Design（主轴）+ 角色分层（feature 内 + 全局两层）+ Shared Kernel（`src/shared/`）+ Hexagonal touch（`src/infrastructure/` 隔离外部 IO，按平台必要性启用）。
**不是** Clean Architecture——没有显式 Entities / Use Cases 层，未做完整 Dependency Inversion；中小规模下属于过度设计。

---

## 三条平台轨

| 平台轨 | 适用 | 关键栈 |
|---|---|---|
| **Web** (`platforms/web/`) | 浏览器 SPA / 后台管理系统 | Vite 或 Webpack 5 / React / React Router (`useRoutes` + `React.lazy`) / Axios 或 fetch wrapper / Tailwind + CSS-in-JS（antd-style / Emotion）+ CSS Modules / Zustand + React Query |
| **React Native** (`platforms/react-native/`) | 原生 iOS / Android 应用 | Expo Router 文件式路由 或 React Navigation / NativeWind / fetch wrapper + AbortController + HttpAuthBridge / Zustand persist + React Query |
| **Taro / 小程序** (`platforms/taro/`) | 微信 / 支付宝 / 字节 / 百度 / QQ / 京东 / 鸿蒙混合 + H5 + RN 同源 | Taro 4 + React + Webpack 5 / 框架文件式路由（`app.config.ts` / `pages/<x>/index.config.ts`） / Tailwind v4 + weapp-tailwindcss / Taro.request 封装 / Zustand + React Query |

差异**只在**目录/路由/样式/原生 API 处。核心结构（feature / shared / providers）、依赖方向、状态边界、类型分层、横切模式**完全一致**——这些写在 `core/`。

---

## 章节索引

### core/ — 通用核心（所有项目必读）

| 文件                                         | 适用场景                                    |
|--------------------------------------------|-----------------------------------------|
| `core/01-architecture.md`                  | 六角色与依赖矩阵 + 可选 Infrastructure 层          |
| `core/02-naming.md`                        | 文件/函数/类命名 + Feature 词根公约 + Import 顺序   |
| `core/03-types.md`                         | 类型三层：DTO / View Model / Component Props |
| `core/04-state.md`                         | 状态三分决策树 + 跨 feature invalidation        |
| `core/05-bounded-context.md`               | 跨 feature 数据流动两条合法路径 + Barrel 策略       |
| `core/06-cross-cutting.md`                 | 错误 / 加载 / 空态 / 401 / 主题 / 副作用           |
| `core/07-anti-patterns.md`                 | 反例速查 12 条                               |
| `core/08-red-lines.md`                     | 红线规则（TS / 状态管理 / HTTP-认证 / Bug 修复）     |
| `core/09-scaling.md`                       | 扩展信号（Scaling Signals）                   |

### platforms/ — 按项目栈选一套读

| 文件                                              | 适用场景                                        |
|-------------------------------------------------|---------------------------------------------|
| `platforms/web/01-directory.md`                 | Web SPA 目录布局                                |
| `platforms/web/02-routing.md`                   | 集中路由 + lazy + Guard                         |
| `platforms/web/03-styling.md`                   | Tailwind atom + CSS-in-JS theme + CSS Modules |
| `platforms/web/04-new-feature.md`               | Web 项目新增 feature 10 步                       |
| `platforms/web/05-aliases.md`                   | Web 路径别名最少化方案                                |
| `platforms/react-native/01-directory.md`        | RN/Expo 目录布局（根 `app/` + `src/`）              |
| `platforms/react-native/02-routing.md`          | Expo Router 文件式 / React Navigation 集中       |
| `platforms/react-native/03-styling.md`          | NativeWind className + `theme.X` for native props |
| `platforms/react-native/04-new-feature.md`      | RN 项目新增 feature 10 步                        |
| `platforms/react-native/05-aliases.md`          | RN 完整别名集 (`@features` `@shared` `@infra` ...) |
| `platforms/taro/01-directory.md`                | Taro 多端目录布局（`src/pages/` 框架入口 + `src/features/` 业务） |
| `platforms/taro/02-routing.md`                  | Taro `app.config.ts` 路由注册 + 多端构建            |
| `platforms/taro/03-styling.md`                  | Tailwind v4 + weapp-tailwindcss class 转义     |
| `platforms/taro/04-new-feature.md`              | Taro 项目新增 feature 10 步（含 `pages/` 与 `screens/` 衔接） |
| `platforms/taro/05-aliases.md`                  | Taro 路径别名 + `tsconfig.json` 与 `config/index.ts` 双向同步 |

### 通用工具链

| 文件                            | 适用场景                                       |
|-------------------------------|--------------------------------------------|
| `99-dependency-check.md`      | dependency-cruiser / feature-boundaries / tsc CI 集成 |

---

## 🚀 30 秒 TL;DR

> AI 写第一行代码前的 11 条速记。详细规则按章节展开。

1. **角色 6 层**：Route → Page → (Component, Hook/Query) → Service/API；Provider 是 composition root。**严禁反向依赖**。详见 `core/01-architecture.md`。
2. **Feature 内 grep 即可看全部**：同一 feature 的所有文件共享词根（单数小写连字符）。词根表见各项目 `.agents/PROJECT.md`。
3. **不要跨 feature 直接 import 内部**：要么上移到 `@shared/`，要么 Page 编排两个 feature 的 hook。`core/05-bounded-context.md` 给出 Web / RN / Taro 三种 barrel 策略。
4. **路由文件保持薄壳**：业务永远写在 feature 内的 Page/Screen 组件，路由层只做声明 + lazy + Guard。
5. **样式不要双写**：同一元素同一属性禁止同时 className + `style={{ }}`。
6. **状态三分**：服务端 → React Query；当前组件 → `useState`；跨页持久 → Zustand + persist。**表单 state 不进 Zustand**。
7. **HTTP 单例 + 依赖反转**：`@shared/api/http.ts` 通过 `setTokenGetter()` / `configureHttp({...})` / `HttpAuthBridge` 接收依赖，不直接 import 任何 store / router / feature。
8. **类型严格**：`strict: true`，禁 `any`，类型 import 必须 `import type`。
9. **包管理锁文件**：yarn / pnpm / npm 选一种不混用；commit message 用 Conventional 前缀；**bug fix 必写 root cause**。
10. **机器校验**：lint 串接格式器 + feature 边界检查 + `tsc --noEmit`，CI 必跑。详见 `99-dependency-check.md`。
11. **不确定 → 读对应章节**，不要凭直觉。

---

## 平台轨选择速查

- 项目根有 `vite.config.ts` / `webpack.config.js` 但**不是** Taro → 走 `platforms/web/`
- 项目依赖含 `expo` 或 `@react-navigation` → 走 `platforms/react-native/`
- 项目依赖含 `@tarojs/taro` → 走 `platforms/taro/`
- 项目同时是多种？逐一对照各 `platforms/*/01-directory.md`，挑最接近的一套。

**项目层配套文件**（项目侧维护）：
- `.agents/PROJECT.md` / `CLAUDE.md`：feature 词根清单、theme tokens、品牌色、store 清单、当前业务关注点
- `README.md`：命令、env 变量、构建产物说明
