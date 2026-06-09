# 六角色与依赖矩阵（通用）

> 适用所有前端项目（Web / React Native / Taro / 小程序）。平台特有的路由实现细节见 `platforms/<x>/02-routing.md`。

## 核心 6 角色

> 「Page」是统一术语；在 React Native / Taro 项目里习惯叫「Screen」，等价。

| 角色 | 职责 | 可依赖 | **不可依赖** |
|---|---|---|---|
| **Route** | 路由声明 + 嵌套 + Guard + URL params 解析。**路由文件 1 行薄壳**，仅 lazy / re-export 业务 Page | Page（仅通过 `React.lazy(() => import(...))` 或 `export { default } from '...'`） | Component、Hook、Query、Service、Store |
| **Page (Screen)** | 编排 Hook / Query + 拼装 Component；处理 loading / error / empty | Component、Hook、Query、Store selector | 直接调 API/Service；跨 feature 深 import 内部 |
| **Component** | 受 props 驱动的展示 + 受控交互 | 其他 Component、`@shared/constants/`、design token | Hook（含 `useQuery` / `useMutation`）、API/Service、Store |
| **Hook / Query** | 数据编排（React Query / Zustand 包装）+ 业务派生计算 + DTO→ViewModel 映射 | API/Service、Store、其他 Hook | 渲染 JSX |
| **API / Service** | HTTP / WebSocket / IO 封装 + DTO 类型定义 | `@shared/api/http`、`@shared/constants/`、纯函数 | React hook / JSX、其他 feature 内部 |
| **Provider** | App 启动 wiring（QueryClient 实例、ThemeProvider、AuthProvider、`configureHttp` / `setTokenGetter` 注入） | 任意层（composition root） | — |

## 可选第 7 角色：Infrastructure（Hexagonal Adapter）

**何时启用**：当需要把"外部 IO / 平台原生能力"从业务隔离时。

| 平台 | 必要性 | 典型内容 |
|---|---|---|
| Web | 🟡 按需 | analytics 适配、Service Worker、IndexedDB wrapper、WebRTC |
| React Native | ✅ 推荐 | SecureStore / Keychain、原生桥、push notifications、permissions |
| Taro / 小程序 | ✅ 推荐 | `wx.*` / `my.*` / `tt.*` 跨端兼容层、`Taro.login` / 支付 / 文件 |

放在 `src/infrastructure/` 下，按用途分子目录（storage / native / analytics / permissions / notifications）。

**Infrastructure 层约束**：

- 只允许依赖平台运行时 API（`Platform`、`NativeModules`、`Appearance`、`window`、`document`、`Taro`、`wx`）
- **禁止**依赖 React、业务 feature
- 对外暴露纯函数或类实例，供 `@shared/api/http.ts` 或 feature 通过 Port 注入

---

## 依赖方向（严禁反向）

```
Route ─→ Page ─┬─→ Component
               ├─→ Hook ─┬─→ Service/API ──→ Infrastructure (可选)
               └─→ Query └─→ Store ─────→ ↑
                                          │
Provider ─────────────────────────────────┘  (启动时注入 token getter / http config)
```

## 跨层快速校验

- **Component 顶部**不应出现带 `useQuery` / `useMutation` 的 hook import → 上移到 Page 或包成 `hooks/use-*` / `queries/use-*`
- **Service / API 文件**顶部不应出现 `import { useXxx } from 'react'` 或带 JSX 的运行时组件
  - 例外：`import type` 总是允许；平台运行时 API（`Platform`、`window`、`Taro`、`wx`）允许
- **Store** 不应导入业务 Service（infrastructure-level service 如 `secure-storage` / `local-storage` 允许；type-only import 总是允许）
- **`@shared/api/http.ts`** 不直接 import 任何 store / router / feature 内部模块。需要 token / tenant 等能力时，由 Provider 在启动期注入（`configureHttp({ getToken })` / `setTokenGetter()`）

## 依赖反转：HTTP 与 Auth

```ts
// ❌ 错误：http.ts 顶部 import store 反向依赖
// shared/api/http.ts
import { useAuthStore } from '@features/auth/stores/auth.store';
const token = useAuthStore.getState().token;   // 反向依赖

// ✅ 正确：http.ts 暴露注入接口，Provider 在启动期注入
// shared/api/http.ts
interface HttpAuthBridge {
  getToken: () => string | null;
  getRefreshToken?: () => string | null;
  onUnauthorized?: () => void;
}
let bridge: HttpAuthBridge = { getToken: () => null };
export function configureHttp(b: HttpAuthBridge) { bridge = b; }

// providers/AppProvider.tsx
configureHttp({
  getToken: () => useAuthStore.getState().token,
  onUnauthorized: () => { logout(); router.replace('/login'); },
});
```

**这一条是分层是否"立得住"的试金石**——只要 `shared/` 里没有反向 import `features/`，整个架构的依赖图就是有向无环的。
