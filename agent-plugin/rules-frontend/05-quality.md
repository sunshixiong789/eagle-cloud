# 红线、反例、测试、性能与依赖校验

# 一、红线

## TypeScript

- `tsconfig` 必须 `strict: true`；新代码不用 `any`，需要任意值时用 `unknown` + 类型守卫
- 类型专用导入用 `import type`
- 组件 props 显式定义 interface；内部变量和返回值能推断则不重复标注
- 不写大段 JSDoc；必要背景用短注释说明**为什么**

## 状态

- 服务端状态进 React Query，不复制到 Zustand
- React Query key 用元组或 key factory
- Zustand 用 selector，不全量订阅
- persist store 有 hydration 标记
- 表单状态不进全局 store
- 不用 React Context 管业务状态

## HTTP / Auth

- HTTP 客户端**单例**，放 `shared/api/http`
- token 存取走专门 lib 或 infrastructure adapter，**不散落**读写 localStorage / AsyncStorage / Taro storage
- `shared/api` 不反向 import feature；需要 token 等能力时启动期注入
- 业务代码不直接新建 fetch/axios 实例

## 样式

- 同一元素同一属性不同时用 className 和 style
- 主题色走 token / className / 平台主题系统，不硬编码散落
- `src/styles/global.*` 只放字体和 reset

## 包管理

- 一个项目只用一种包管理器，提交对应 lock 文件
- **禁止多个 lock 文件并存**
- 新依赖先确认体积、维护状态、平台兼容性和替代方案

## Bug 修复

- 先复现和定位 root cause，再改代码
- 一次改一个可疑点，改后验证
- 同一 bug 三次失败后暂停，重新审视假设或升级讨论
- bug fix **不夹带**无关重构、格式化、rename

---

# 二、反例速查

依次扫一遍，命中即重构：

1. **Component 内出现 `useQuery` / `useMutation`** → 上提到 Page，或包成 `hooks/use-*` / `queries/use-*`
2. **API / Service 文件出现 `import { useXxx } from 'react'` 或 JSX** → 例外：`import type` 总是允许；平台运行时 API（`Platform`、`NativeModules`、`Taro`、`wx`、`window`）允许
3. **JSX 里出现 `data.created_at` 这类 DTO 原名** → Hook/Query 缺映射（见 `02-conventions.md`）
4. **一个 hook / query 文件返回 10+ 字段** → 拆 `useXxxQuery` + `useXxxMutation` + `useXxxDerived`
5. **同一 feature 逻辑分散在多目录但词根不一致** → 重命名对齐
6. **Store action 内有 fetch** → 移到 hook/mutation 的 `onSuccess` 触发 store action
7. **`shared/api/http.ts` 顶部 import store 或 feature** → 走依赖反转（`configureHttp` / `setTokenGetter`）
8. **路由文件超过 1 行** → 应是 `export { default } from ...` 或 `React.lazy(...)` 一行薄壳
9. **Hook 内 `queryClient.invalidateQueries(['<其他 feature>', ...])`** → 移到 Page
10. **feature 内部用绝对路径自引** → 改相对路径
11. **跨 slice 用深路径（Web）或用 barrel（RN/Taro）** → 按 `01-architecture.md` 平台策略改
12. **`import { Foo }` 引入只作类型的导出** → 改 `import type`，避免运行时副作用与循环依赖
13. **feature 内新建 `context/` / `guard/` / `utils/`** → 用 `components/` / `lib/` 容纳
14. **同一元素同一属性同时写 className + `style={{ }}`** → 选一种
15. **Zustand 全量订阅 `const store = useXxxStore()`** → 改 selector
16. **表单 state 进了 Zustand** → 改 `useState` 或表单库

---

# 三、测试

## 必测

- DTO → ViewModel 映射
- query hook / mutation 的成功、失败、invalidate
- Zustand store action、persist hydration
- 权限、401、错误展示、表单校验
- 关键 Page/Screen 编排和回归 bug

## 组织

- 测试与源码同目录：`*.test.ts` / `*.test.tsx`
- 测试工具、mock server、render helper 放 `src/test/`

| 平台 | Runner |
|---|---|
| Web | Vitest + Testing Library |
| RN | Jest + React Native Testing Library |
| Taro | 优先纯函数 / hook 单测；页面行为用平台测试能力或手工验证补充 |

## Mock 原则

- mock **网络边界**，不 mock 被测 hook/store/component 本身
- API mock 返回后端 **DTO 形状**，不返回 ViewModel
- 时间、存储、路由、平台 API 用可控 mock

## 禁止清单

- 只测快照不测行为
- 为通过测试修改生产契约
- E2E 替代单元测试覆盖业务分支
- `sleep` 等待异步 —— 用 `waitFor` / fake timers

---

# 四、性能预算

## 通用红线

- **循环依赖为 0**
- 首屏只加载必要 feature，路由级懒加载
- 列表用分页、虚拟列表或增量加载
- 图片压缩、懒加载、按尺寸请求
- 生产代码不保留大量 `console.log`

## 平台关注点

| 平台 | 关注 |
|---|---|
| Web | bundle size、FCP/LCP、路由 chunk、polyfill 精准性 |
| RN | 首屏 JS 执行、列表虚拟化、图片缓存、Hermes profiling |
| Taro | 主包体积、分包、`setData` 频率、跨端样式转换体积 |

## 必查场景

新增大依赖（图表、富文本、地图、视频、支付 SDK）；单页面/单文件持续膨胀；页面出现长列表、轮询、WebSocket、复杂表单；RN/Taro 引入 Web 生态大包。

## 禁止清单

- Web 全量引入 lodash / moment / polyfill
- RN 列表 1000+ 项不用虚拟化
- Taro 主包直接引用分包代码
- 为性能问题盲目 `memo/useMemo/useCallback` 而不先定位热点

---

# 五、依赖校验（落到 CI）

优先用 `dependency-cruiser`；已用 ESLint boundaries 或自写脚本的项目沿用。

## 必校验

- 循环依赖为 0
- `shared/api/http` 不 runtime import `features` / `providers` / `app`
- API / Service 文件不 runtime import React（只允许 `import type`）
- 跨 feature 不直接 import 对方内部模块
- feature 内部不用绝对路径自引

```bash
yarn add -D dependency-cruiser
yarn depcruise --validate .dependency-cruiser.cjs src
```

```js
module.exports = {
  forbidden: [
    { name: 'no-circular', severity: 'error', from: {}, to: { circular: true } },
    {
      name: 'shared-api-no-feature-runtime',
      severity: 'error',
      from: { path: '^src/shared/api/' },
      to: { path: '^src/features/' },
    },
    {
      name: 'api-no-react-runtime',
      severity: 'error',
      from: { path: '(^|/)(api|service|services)/.*\\.(ts|tsx)$' },
      to: { path: '^node_modules/react($|/)' },
    },
  ],
};
```

CI 至少在 PR 跑：依赖检查、类型检查、lint、单元测试。

---

# 六、扩展信号

当前 FSD-lite 够用。出现下列信号**再**考虑精细化，每次升级前先核对本表，避免过度设计：

| 信号 | 应对 |
|---|---|
| 单 feature 文件数 > 30 | feature 内引入 `widgets/` 或 `entities/` 子分层 |
| 多 feature 都需要某领域规则 | 抽 `shared/domain/<rule>.ts` 纯函数库 |
| 出现真正的领域聚合行为（状态机、审批流） | 引入 `features/<f>/use-cases/*.ts` |
| 基础设施需要切实现 | 扩展 `src/infrastructure/`，给依赖反转更多 Port |
| 团队 > 3 人或 feature 数 > 15 | 评估 monorepo（pnpm workspace / nx / turborepo） |
| 同源代码跨多端 | 评估 Taro / Uni-App，或把纯业务函数抽 npm 包 |
| Zustand store > 10 个、跨 store 联动复杂 | 评估 Redux Toolkit / Jotai / XState |
| 首屏 > 3s | feature 级 code-split、路由级 lazy、依赖体积审计 |

## 反向信号：过度抽象（出现应回退）

- feature 内 80% 文件都依赖 `shared/utils/` → 业务逻辑是不是误抽到 shared 了
- 一个 hook 包了 5 层其他 hook，最底层只调 1 个 API → 拍扁
- DTO / ViewModel / Props 三层映射代码量超过实际渲染代码 → 简化为两层
- 路由表 30+ lazy import 但无 chunk 分组 → 按业务域分组打包
- 引入 use-case 层但只有 1 个 use-case → 撤回

## 监控点

- `src/features/<x>/` 文件数 → 单 feature 触顶 30 报警
- bundle size → Web 主包 > 500KB / 小程序主包 > 1.5MB 报警
- 单文件 > 500 行考虑拆分
- 循环依赖数必须为 0

具体阈值由项目自定。
