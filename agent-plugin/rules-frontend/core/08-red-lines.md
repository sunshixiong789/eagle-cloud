# 红线规则（通用）

样式系统的具体实现见 `platforms/<x>/03-styling.md`。本文只列**所有平台共通**的红线。

## 1. TypeScript

- **`tsconfig` 必须 `strict: true`**；新增代码**不允许出现 `any`**。需要"任意"语义用 `unknown` + 类型守卫。
  *理由*：`any` 关闭整条调用链的类型检查，是潜在 bug 入口。

- **类型专用 import 必须用 `import type`**：
  ```ts
  import type { Foo } from '...';
  import { foo, type Foo } from '...';      // 混合时
  ```
  *理由*：被 TS 编译期完全擦除，零运行时副作用，避免循环依赖与 tree-shaking 问题。

- **不为类型而类型**：能从函数体 / 默认值推断的，不要显式标注（返回类型、内部变量）。**React 组件 props 必须显式 interface**。

- **Props interface 默认不 `export`**。只有真正外部消费才 export，避免单向变成 public API。

- **不写 JSDoc 大段块注释**；类型即文档。需要说明"为什么"时用 1 行 `//` 注释。

---

## 2. 状态管理

- **三类状态分别用三种工具**（见 `04-state.md`）。

- **Zustand 必须用 selector 模式**：`useStore(s => s.x)`，不要 `const store = useStore()` 全量订阅。
  *理由*：全量订阅 = 任何字段变化都 re-render，性能黑洞。

- **Zustand persist store 必须有 `_hasHydrated` 标记**，UI 在未 hydrated 前展示 fallback。
  *理由*：异步存储（AsyncStorage / localStorage / Taro.storage）首帧用默认值会闪烁。

- **表单 state 不进 Zustand**。临时输入、modal 草稿、列表筛选这类用 `useState` 或表单库。
  *理由*：表单生命周期跟组件绑定；进 store 会污染全局快照。

- **React Query key 用元组**：`['products', filters]`、`['order', id]`。或集中在 `queries/<f>.keys.ts`。
  *理由*：序列化稳定、便于 partial invalidate。

- **不要用 React Context 管业务 state**：跨页持久用 Zustand，组件树上下文传值用 props / Provider only when really needed（如 ThemeProvider 这种 framework-level）。
  *理由*：Context 全树重渲染，且没有 selector；Zustand 默认就比 Context 更高效。

---

## 3. HTTP / 认证

- **HTTP 客户端只有一个实例**，放在 `@shared/api/http.ts`。**禁止**另起、禁止业务直接 `fetch` / `axios.create`。

- **Token 存取走专门的 lib**（如 `@features/auth/lib/token.ts` 或 `@infra/storage/secure-storage.ts`），不要散落在各处直接读 `localStorage` / `AsyncStorage` / `Taro.getStorageSync`。

- **`@shared/api/` 禁止反向 import `@features/`**。需要 token 等 feature 提供的能力时，让 feature 在启动期通过 `configureHttp({...})` / `setTokenGetter()` 注入到 http 客户端（详见 `01-architecture.md` 依赖反转）。

- **401 / Token 刷新由 http 客户端拦截器统一处理**。业务层无感知（详见 `06-cross-cutting.md`）。

---

## 4. Bug 修复流程

- **先找根因，禁止盲修症状**。能用一句话描述"哪个变量在哪个 effect 被错误赋值"才能开始动代码。
  *理由*：症状修复留 root cause 在原地，迟早再爆。

- **一次改一个变量**。多个可疑点不要一起改；改一处验一处。
  *理由*：并行改动无法定位真正 work 的那个，回归时找不回。

- **三次失败必停**。同一 bug 第 3 次尝试失败后，**禁止**第 4 次盲修，停下来质疑：是否假设错、架构错、复现条件理解错。需要时升级讨论。
  *理由*：3 次失败 ≈ 心智模型与代码现实脱节，再试只是积累乱七八糟的副作用。

- **Bug 修复 commit 只动相关代码**。不要顺手重构、不要顺手改格式、不要顺手 rename 无关变量。
  *理由*：bug fix 要可独立 revert，混入无关改动会破坏 revert 的精度。

- **commit message 必须写清 root cause**，不要只说"修复 xx 问题"。模板：

  ```
  fix(<scope>): <症状的一句话描述>

  Root cause: <哪个变量 / 哪个调用 / 哪个 effect 在什么条件下做错了什么>
  ```

  具体示例：

  ```
  fix(auth): refreshAccessToken 在非安全上下文 crypto 不可用导致登录回退失败

  Root cause: 浏览器非 HTTPS 环境下 window.crypto.subtle 为 undefined，
  generateCodeChallenge 抛 TypeError；上层 catch 直接 setCurrentUser(null)
  让用户被踢回登录页，但实际只是 PKCE 计算失败。
  ```

- **找 regression 用 `git log -p <file>`** 加二分回溯，先定位**引入提交**再决定怎么改，不要在最新代码上凭直觉打补丁。

---

## 5. 样式（通用约束）

平台具体 Stack 见 `platforms/<x>/03-styling.md`。**所有平台共通**：

- **不双写**：同一元素的同一样式不要同时用 className + `style={{ }}`。
- **允许 `style={{ }}`** 的白名单：
  - RN 阴影 props（`shadowColor / shadowOffset / shadowOpacity / shadowRadius / elevation`，无对应 utility）
  - 动画 worklet（reanimated `useAnimatedStyle()` 返回值）
  - 来自 props / state / `Dimensions.get()` / hook 返回的动态值
  - CSS border-trick 等几何 hack

- **`src/styles/global.*`** 只放 `@font-face` 与基础 reset，**禁止**写业务样式。

---

## 6. 包管理 / 提交

- **包管理器只用一种**（yarn / pnpm / npm）。lock 文件提交到 git。**禁止**多个 lock 文件并存。

- **Commit message 用 Conventional Commits 前缀**：`feat(<scope>): ...` / `fix(<scope>): ...` / `refactor(<scope>): ...` / `chore(<scope>): ...` / `docs(<scope>): ...` / `test(<scope>): ...` / `build(<scope>): ...`。

- **Bug fix commit 必写 root cause**（详见 §4）。

- **`<scope>` 用 feature 词根或 `shared` / `infra` / `providers` / `app`**。
