# AI Agent 工程规范

> 面向 AI 编程助手（Claude Code / Codex / Cursor / Gemini / Copilot）的 **通用** 规范源。
> 适用范围：React Native + Expo Router + Feature-Sliced Design 的项目。
> 项目特定内容（feature 词根、token 列表、store 清单、品牌色）请见 `.agents/PROJECT.md`。

**架构风格**：Feature-Sliced Design（主轴）+ 角色分层（feature 内 + 全局两层）+ Shared Kernel（`src/shared/`）+ Hexagonal touch（`src/infrastructure/` 抽离外部 IO）。
**不是** Clean Architecture——没有显式 Entities / Use Cases 层，未做完整 Dependency Inversion，中小规模下属于过度设计。

---

## 🚀 30 秒 TL;DR

> AI 写第一行代码前的 10 条速记。详细规则按章节展开。

1. **架构 7 角色**：Route → Screen → (Component, Hook) → Service → Infrastructure；Provider 是 composition root。**严禁反向依赖**。
2. **Feature 内 grep 即可看全部**：同一 feature 的所有文件共享词根。词根列表见 `.agents/PROJECT.md`。
3. **不要跨 feature import**：要么上移到 `@shared/`，要么 Screen 编排两个 feature 的 hook。
4. **路径别名**：`@shared/*` `@features/*` `@infra/*` `@providers/*` `@app/*`。引用其他位置才用 `@/*`。
5. **路由文件保持 1 行薄壳**：`app/<route>.tsx` 永远是 `export { default } from '@features/.../screens/...'`，业务写在 feature 里。
6. **样式 className 化**：主题色用 className + `dark:` 配对；`theme.X` 只作为 RN 原生 prop 颜色；`nwColorScheme.set()` **必须**传原始 `mode`（`'system' | 'light' | 'dark'`），不传 resolved 值。
7. **状态三分**：服务端 → React Query；当前组件 → `useState`；跨页持久 → Zustand + persist。**表单 state 不进 Zustand**。
8. **`@shared/api/http.ts` 通过 `HttpAuthBridge` 接收依赖**，不直接 `import` 任何 store / router。
9. **包管理用 Yarn**（不 npm）；commit message 中文 + 类型前缀；**bug fix 必写 root cause**。
10. **架构规则有机器校验**：`yarn arch`（dependency-cruiser），违规拦下。详见 §13。
11. **不确定 → 读对应章节**，不要凭直觉。

---

## 1. 七角色与依赖矩阵

| 角色 | 职责 | 可依赖 | **不可依赖** |
|---|---|---|---|
| **Route** | Stack / Modal 配置、URL params 解析、挂载 AppProvider | Screen（仅通过 re-export 引用 default） | Component、Hook、Service、Store |
| **Screen** | 编排 Hook + 拼装 Component；处理错误 / 加载 / 空态 | Component、Hook、Store selector | 直接调 Service / 直接 fetch |
| **Component** | 纯展示 + 受控交互（受 props 驱动） | 其他 Component、`@shared/constants/`、`theme.X`（仅 prop 颜色） | Hook（含 `useQuery` / `useMutation`）、Service、Store |
| **Hook** | 数据编排（React Query / Zustand 包装）+ 业务派生计算 | Service、Store、其他 Hook | 直接渲染 JSX |
| **Service** | API / WebSocket / IO 封装；DTO 类型定义 | `@shared/api/http`、`@shared/constants/`、纯函数 | React 的 hook / JSX；其他 feature 的代码 |
| **Infrastructure** | 系统级适配（SecureStore、原生桥、analytics / push 等） | RN 运行时 API（`Platform` / `NativeModules` / `Appearance`） | React、业务 |
| **Provider** | App 启动 wiring（`configureHttp`、QueryClient 实例、ThemeProvider） | 任意层（composition root） | — |

各角色的具体位置见 §2 目录布局。

**依赖方向（严禁反向）**：

```
Route ─→ Screen ─┬─→ Component
                 └─→ Hook ─┬─→ Service ──→ Infrastructure
                           └─→ Store
                                          ↑
Provider ──────────────────────────────────┘  (启动时 configureHttp 注入)
```

**跨层快速校验**：

- Component 顶部不应出现带 `useQuery` / `useMutation` 的 hook import
- Service 顶部不应出现 `import { useXxx } from 'react'` 或带 JSX 的 `react-native` 组件（注：`import type` 总是允许；RN 运行时 API 如 `Platform` / `NativeModules` 也允许）
- Store 不应导入业务 Service（infrastructure-level service 允许，如 `secure-storage`；type-only import 总是允许）
- `@shared/api/http.ts` 通过 `configureHttp({ ... })` 接收依赖，不直接 import 任何 store / router

---

## 2. 完整目录布局与预留模块

整张地图——✅ 已就位、🟡 预留命名（满足触发条件再启用）、❌ 不采用（说明理由）：

```
<project-root>/
├── app/                              # ✅ Expo Router 路由根（不迁 src/app/）
│   ├── _layout.tsx                   # ✅ 挂 AppProvider + Stack 声明
│   ├── (tabs)/                       # ✅ 底部 tab
│   │   ├── _layout.tsx
│   │   └── <tab>.tsx                 # ✅ 1 行 re-export 壳，指向 @features/.../screens/...
│   ├── <route>/[id].tsx              # ✅ 1 行 re-export 壳
│   └── ...                           # ✅ 其余 modal 路由都是薄壳
│
├── src/
│   ├── features/                     # ✅ 业务 bounded context（词根列表见 PROJECT.md）
│   │   └── <feature>/
│   │       ├── api/                  # ✅ API 调用 + DTO 类型 + 测试
│   │       ├── hooks/                # ✅ React Query hooks + 派生计算
│   │       ├── queries/              # 🟡 预留：当 query/mutation > 5 个时，
│   │       │                         #     拆 queries/<f>.keys.ts + <f>.queries.ts
│   │       ├── stores/               # ✅ Zustand store（feature 私有 state）
│   │       ├── schemas/              # 🟡 预留：未引入 zod，TS interface 够用；
│   │       │                         #     有 runtime 输入校验需求时启用
│   │       ├── components/           # ✅ feature 内部复用展示组件
│   │       ├── screens/              # ✅ 屏幕组件，被 app/ 薄壳 import
│   │       ├── types.ts              # 🟡 预留：跨文件共享类型 ≥ 3 个时创建
│   │       └── index.ts              # ❌ 不采用：barrel 导致循环依赖与 tree-shaking 问题
│   │
│   ├── shared/                       # ✅ Shared Kernel：跨 feature 通用
│   │   ├── api/
│   │   │   ├── http.ts               # ✅ fetch 封装 + AbortController + HttpAuthBridge
│   │   │   ├── oauth.ts              # ✅ OAuth client_id 取值器 + OAuth2Token 类型
│   │   │   ├── error.ts              # 🟡 预留：错误类型 ≥ 2 个时拆出
│   │   │   ├── interceptors.ts       # 🟡 预留：接入第三方拦截器（log / metric）时拆
│   │   │   └── types.ts              # 🟡 预留：公共类型 ≥ 3 个时拆
│   │   │
│   │   ├── query/                    # ❌ 不采用：QueryClient + Provider 直接在
│   │   │                             #     @providers/QueryProvider.tsx，单点 composition
│   │   │
│   │   ├── stores/                   # ✅ 跨 feature 共享 store（具体清单见 PROJECT.md）
│   │   │   ├── persist-storage.ts    # ✅ AsyncStorage 跨平台 wrapper（非 store，是工具）
│   │   │   ├── app.store.ts          # 🟡 预留：全局 app 启动 flag / 全局 modal
│   │   │   └── modal.store.ts        # 🟡 预留：跨 feature 的全局 modal 队列
│   │   │
│   │   ├── ui/                       # ✅ 跨 feature 复用纯展示组件（kebab-case 文件名）
│   │   ├── hooks/                    # ✅ 通用 hook（color-scheme、theme 等）
│   │   ├── constants/                # ✅ theme tokens、env、storage-keys
│   │   ├── utils/                    # 🟡 预留：format / date / money 等纯函数
│   │   ├── schemas/                  # 🟡 预留：跨 feature 通用校验 schema
│   │   └── types/                    # 🟡 预留：跨 feature 通用类型
│   │
│   ├── infrastructure/               # ✅ Hexagonal Adapters（外部 IO 隔离）
│   │   ├── storage/                  # ✅ SecureStore 等持久化封装
│   │   ├── native/                   # ✅ 原生桥（如平台 SDK）
│   │   ├── analytics/                # 🟡 预留：Mixpanel / Amplitude / 自研事件
│   │   ├── permissions/              # 🟡 预留：相机 / 位置 / 通知授权封装
│   │   └── notifications/            # 🟡 预留：Push / 本地通知封装
│   │
│   ├── providers/                    # ✅ Composition Root
│   │   ├── AppProvider.tsx           # ✅ 顶层聚合 + configureHttp wiring
│   │   ├── QueryProvider.tsx         # ✅ QueryClient + QueryClientProvider
│   │   └── ThemeProvider.tsx         # ✅ Navigation theme + nwColorScheme.set(mode)
│   │
│   ├── app/                          # ❌ 不采用：Expo Router 用根 app/；切到 src/app/ 需
│   │                                 #     EXPO_ROUTER_APP_ROOT 配置，零收益高风险
│   │
│   ├── assets/                       # ❌ 不采用：保留根 /assets/（Expo asset 加载惯例）
│   │
│   └── config/                       # ❌ 不采用：env 走 EXPO_PUBLIC_*、app config 用 app.json、
│                                     #     theme tokens 在 @shared/constants/ + tailwind.config.js
│
├── assets/                           # ✅ 图片 / 字体 / 图标
├── docs/                             # ✅ specs / plans 历史
├── __mocks__/                        # ✅ Jest mocks
├── app.json                          # ✅ Expo config
├── tailwind.config.js                # ✅ Tailwind tokens
├── tsconfig.json                     # ✅ 路径别名定义在这里
├── jest.config.js                    # ✅ logic + rn 双 project
└── ...
```

**Feature 内部布局原则**：

```
@features/<name>/
├── api/         # 必有：API 调用 + DTO + .test.ts
├── hooks/       # 通常有：use-* React Query / 派生
├── stores/      # 按需：feature 私有 state（不是所有 feature 都需要）
├── components/  # 按需：feature 内部复用
├── screens/     # 通常有：屏幕（被 app/ 薄壳 re-export）
└── types.ts     # 按需：跨文件共享类型
```

子目录**按需建**，不强制全有。

---

## 3. 命名约定

### 文件名

| 类别 | 风格 | 示例 |
|---|---|---|
| 屏幕组件 | PascalCase + `Screen` 后缀 | `LoginScreen.tsx`、`ProductDetailScreen.tsx` |
| Feature 私有组件（`@features/<f>/components/`） | PascalCase | `ProductCard.tsx`、`OrderListItem.tsx` |
| Shared UI（`@shared/ui/`） | kebab-case | `app-image.tsx`、`themed-text.tsx`、`screen-header.tsx` |
| Hook | kebab-case + `use-` 前缀 | `use-catalog.ts`、`use-color-scheme.ts` |
| Store | kebab-case + `.store.ts` 后缀 | `user.store.ts`、`<feature>-ui.store.ts` |
| API | kebab-case + `.api.ts` 后缀 | `catalog.api.ts`、`auth.api.ts` |
| 类型独立文件 | kebab-case 或 `types.ts` | `types.ts`、`product.types.ts` |
| 平台特定 | 后缀 `.ios.tsx` / `.android.tsx` / `.web.ts` / `.native.ts` | `icon-symbol.ios.tsx` |
| 测试 | 同源 + `.test.{ts,tsx}` | `auth.api.test.ts`、`login-screen.test.tsx` |

> **为什么 shared/ui 用 kebab-case 而 features 内用 PascalCase？** 历史遗留 + 与生态约定混用的结果。kebab-case 在 RN 早期教程更常见，PascalCase 在 React 生态主流。两边都已稳定，不强求统一。新建 shared/ui 文件用 kebab-case，新建 feature 组件用 PascalCase。

### 函数 / 类 / 导出

- **Hook**：`useXxx` camelCase，文件 named export
- **Zustand store**：`useXxxStore` named export
- **Component**：PascalCase 函数，screens default export，feature components 通常 named export
- **API 函数**：camelCase 动词起头，如 `fetchProducts`、`listBanners`、`getProductDetail`
- **常量**：UPPER_SNAKE_CASE，如 `BASE_URL`、`DEFAULT_TIMEOUT_MS`
- **类型 / 接口**：PascalCase，如 `Product`、`OAuth2Token`、`RequestOptions`

---

## 4. Feature 命名公约

**同一 feature 的所有文件共享词根**，使 `grep <feature>` 可看到全部相关代码。文件路径前缀统一 `@features/<词根>/`。

**词根规范**：
- 单数小写连字符，如 `catalog`、`services-market`、`order`
- 名词或名词短语，不用动词（不用 `checkout-flow`，用 `checkout`）
- 反映 bounded context 而非 UI 位置（不用 `home-tab`、`profile-page`）

**当前项目的词根清单**：见 `.agents/PROJECT.md` §1。

---

## 5. 新增 feature 操作清单

> 写一个全新 feature `<X>`（比如 `wallet`）的步骤。AI 严格按顺序，每步出 commit-able 单元。

**步骤 1：定词根**
- 沿用 `.agents/PROJECT.md` 现有词根，或为新业务域新增一行（同时更新 PROJECT.md）
- 词根用单数小写连字符

**步骤 2：建目录**

```bash
mkdir -p src/features/<X>/{api,hooks,screens}
# 按需建：stores / components
```

**步骤 3：写 API 层（如有后端调用）**

```ts
// src/features/<X>/api/<X>.api.ts
import { api } from '@shared/api/http';

// DTO 贴后端字段名
export interface XResponse {
  id: number;
  created_at: string;        // 允许 snake_case
}

export async function fetchX(id: string): Promise<XResponse> {
  return api.get<XResponse>(`/x/${id}`);
}
```

**步骤 4：写 Hook（数据编排 + 视图模型派生）**

```ts
// src/features/<X>/hooks/use-<X>.ts
import { useQuery } from '@tanstack/react-query';
import { fetchX, type XResponse } from '@features/<X>/api/<X>.api';

export interface XViewModel {
  id: number;
  createdAt: string;         // camelCase，hook 内做映射
}

function toViewModel(dto: XResponse): XViewModel {
  return { id: dto.id, createdAt: dto.created_at };
}

export function useXQuery(id: string) {
  return useQuery({
    queryKey: ['x', id] as const,
    queryFn: () => fetchX(id),
    select: toViewModel,
  });
}
```

**步骤 5：写 Store（如有跨页 UI state）**

```ts
// src/features/<X>/stores/<X>.store.ts
import { create } from 'zustand';

interface XState {
  filter: string;
  setFilter: (filter: string) => void;
}

export const useXStore = create<XState>((set) => ({
  filter: '',
  setFilter: (filter) => set({ filter }),
}));
```

**步骤 6：写 Screen（编排 hook + 渲染 component）**

```tsx
// src/features/<X>/screens/<X>Screen.tsx
import { View, Text, ActivityIndicator } from 'react-native';
import { useXQuery } from '@features/<X>/hooks/use-<X>';

export default function XScreen() {
  const { data, isPending, error } = useXQuery('123');

  if (isPending) return <ActivityIndicator />;
  if (error) return <Text>加载失败</Text>;
  if (!data) return <Text>暂无数据</Text>;

  return (
    <View className="bg-surface dark:bg-surface-dark">
      <Text className="text-text dark:text-text-dark">{data.createdAt}</Text>
    </View>
  );
}
```

**步骤 7：写路由薄壳**

```tsx
// app/<x>.tsx 或 app/(tabs)/<x>.tsx
export { default } from '@features/<X>/screens/<X>Screen';
```

**步骤 8：注册到 `_layout.tsx`（非 tab 路由）**

```tsx
// app/_layout.tsx
<Stack.Screen name="<x>" options={MODAL_OPTIONS} />
```

**步骤 9：质量检查**

```bash
yarn lint
yarn test
yarn arch
```

**步骤 10：提交**

```
feat(<X>): 新增 <X> 模块 + 屏幕骨架

- screens/<X>Screen.tsx 编排 useXQuery
- api/<X>.api.ts 暴露 fetchX
- hooks/use-<X>.ts 包 useQuery + DTO → ViewModel 映射
- app/<x>.tsx 一行 re-export 壳
```

---

## 6. 类型三层

数据从后端到屏幕走三种形态，每一层禁止"越级穿透"：

| 层 | 位置 | 形态 | 命名 |
|---|---|---|---|
| **DTO** | `@features/<f>/api/*.api.ts` 顶部 | 贴后端原样 | 允许 `snake_case`、历史字段名 |
| **View Model** | `@features/<f>/hooks/use-<f>.ts` 中 `select` / `transform` | 剔除冗余、扁平化 | `camelCase`、按 UI 需要展开 |
| **Component Props** | `@features/<f>/components/<X>.tsx` 顶部 interface | 最小必要 | 只接收原子值，不接收整段 DTO |

**铁律**：DTO 字段名（如 `created_at`、`commission_rate_start`）**不允许**出现在 Component props 类型或 JSX 表达式里。出现即说明 hook 没做映射。

---

## 7. 状态三分决策树

```
有数据吗？
├─ 来自服务端（API / WebSocket）→ React Query
└─ 来自客户端
   ├─ 跨页面 / 跨重启需要保留 → Zustand + persist
   └─ 仅当前组件需要        → useState
```

**反例**：
- 表单输入用 Zustand（应当 `useState`，组件卸载即释放）
- 列表筛选只放本地 `useState`，导致返回再进丢状态（跨页面 → Zustand 或 URL params）
- 服务端数据进 Zustand（重复缓存，丢 React Query 的 invalidate 能力）

**配置基线**（在 `@providers/QueryProvider.tsx` 设好，特殊场景才覆盖）：
- `staleTime: 60s`、`gcTime: 5min`
- `mutation` 不重试；4xx 不重试、5xx 重试 1 次
- React Query key 用元组：`['products', filters]`

### 跨 feature invalidation 模式

当 feature A 的 mutation 完成后，需要让 feature B 的 query 失效，**invalidate 操作在 Screen 编排，不在 hook 内**：

❌ **错误**：A 的 hook 内 `invalidateQueries(['<feature-B>', ...])` —— hook 知道了 B 的 query key，违反 bounded context

✅ **正确**：

```tsx
// <FeatureA>SubmitScreen.tsx
const queryClient = useQueryClient();
const doMutate = useFeatureAMutation();

const onSubmit = async () => {
  await doMutate.mutateAsync(form);
  queryClient.invalidateQueries({ queryKey: ['<feature-B-resource-1>'] });
  queryClient.invalidateQueries({ queryKey: ['<feature-B-resource-2>'] });
};
```

Screen 是编排者，可以同时知道 A 与 B 的 query key。

> 当前项目的 store 清单见 `.agents/PROJECT.md` §4。

---

## 8. Cross-cutting 模式

| 关注点 | 归属层 | 范式 |
|---|---|---|
| **错误** | Service throw `ApiError` → Hook 透传 → Screen 捕获 → UI（toast / modal / inline） | Component 不写 `try/catch`，假设数据已就绪 |
| **加载** | `query.isPending` 在 Screen 内决定 skeleton / spinner | Component 默认渲染非加载态 |
| **空态** | Screen 处理 `data?.length === 0`，渲染空态组件 | 不下沉到 Component |
| **401 / 登出** | `@shared/api/http.ts` 通过 `HttpAuthBridge.onUnauthorized` 回调触发；wiring 在 `@providers/AppProvider.tsx` 完成（`logout()` + `router.replace('/login')`） | Hook / Screen 不重写 |
| **Token 刷新** | `http.ts` 检测 401 + `HttpAuthBridge.getRefreshToken()` 自动 refresh 并重试一次 | 业务调用方完全透明 |
| **主题** | className `dark:` 配对；`theme.X` 仅作为 RN prop 颜色；`nwColorScheme.set()` 必须传原始 `mode` | 见 §11.1 |
| **副作用** | mount/unmount 一次性副作用放 Screen 的 `useEffect` | 可复用的副作用抽 `hooks/use-*.ts` |
| **App 级 listener（WebSocket / push）** | 挂载在 `AppProvider` 内（不在 `app/_layout`），与 QueryProvider 同生命周期 | feature listener 通过 Provider 注入 |

---

## 9. Bounded Context

**每个 feature 的 store / hook / service / component 不互相 import**。

跨 feature 数据流动两条合法路径：

1. **上移**：把需要被多 feature 共享的能力放到 `@shared/` 或 `@infra/`
   - 通用 HTTP / OAuth → `@shared/api/`
   - 通用 hook（color-scheme、theme） → `@shared/hooks/`
   - 通用 store（user、theme） → `@shared/stores/`
   - 通用展示组件 → `@shared/ui/`
   - 系统适配（SecureStore、原生桥） → `@infra/`
2. **Screen 编排**：A 的 Screen 同时调 A 的 hook + B 的 hook（UI 层负责编排，hook 之间不互相依赖）。跨 feature query invalidation 也走 Screen，详见 §7。

**反例**：`@features/<A>/hooks/use-<A>.ts` 顶部 `import { ... } from '@features/<B>/api/...'`。A 和 B 是两个 context，应当走"上移"或"Screen 编排"。

**特例（合并 vs 拆分）**：当两个 feature 强耦合到无法用上述两条化解时，说明它们本来就是同一个 context，应当合并。例如发现两个"feature"共享同一组 hook、store、组件，那就是一个 context，PROJECT.md 里只该有一个词根。

---

## 10. 反例速查

依次扫一遍，命中即重构：

1. Component 内出现 `useQuery` / `useMutation` → 上提到 Screen 或包成 `hooks/use-*`
2. Service 文件出现 `import { useXxx } from 'react'` 或带 JSX 的 `react-native` 组件（注：`import type` 与 RN 运行时 API 如 `Platform` / `NativeModules` 允许）
3. JSX 里出现 `data.created_at` / `data.commission_rate_start` 这类 DTO 原名 → Hook 缺映射
4. 一个 hook 返回 10+ 字段 → 拆成 `useXxxQuery` + `useXxxMutation` + `useXxxDerived`
5. 同一 feature 的逻辑分散在多目录但词根不一致 → 重命名对齐
6. Store action 内有 fetch → 移到 hook 的 `useMutation.onSuccess` 触发 store action
7. `@shared/api/http.ts` 顶部出现 `import { useXxxStore } from '@shared/stores/...'` → 走 `HttpAuthBridge` 注入
8. `app/<route>.tsx` 文件超过 1 行 → 应当是 `export { default } from '@features/.../screens/...'` 薄壳
9. Hook 内出现 `queryClient.invalidateQueries(['<其他 feature>', ...])` → 移到 Screen
10. 用 `import { Foo }` 引入只作为类型的导出 → 改为 `import type { Foo }`，避免运行时副作用与循环依赖

---

## 11. 红线规则

### 11.1 样式系统（NativeWind 4 + Tailwind）

- **主题色必须 className 化**，配对 `light` 与 `dark:` 变体，例如 `bg-surface dark:bg-surface-dark`。
  *理由*：让 NativeWind 接管深浅色切换；避免 `style={{ }}` 与 className 双写。

- **`theme.X`（`getEcommerceTheme` 或同类返回值）只能作为 RN 原生 prop 的颜色字符串**，不写进 `style`。
  *适用*：`<Icon color={theme.X}>`、`<TextInput placeholderTextColor={...}>`、`<LinearGradient colors={[...]}>`。
  *理由*：style 主题色一律由 className 表达；只有原生 prop 拿不到 className 才用 `theme.X`。

- **`nwColorScheme.set()` 必须传原始 `mode`**（`'system' | 'light' | 'dark'`），**禁止**传 resolved 的 `'light' / 'dark'`。
  *理由*：底层走 `Appearance.setColorScheme(value)`，传具体值会持久锁死 Appearance，跨重启都丢失系统跟随；传 `'system'` 才会内部调 `setColorScheme('unspecified')` 解锁。

- **允许保留 `style` 的白名单**（不要勉强 className 化）：
  - `shadowColor / shadowOffset / shadowOpacity / shadowRadius / elevation`（RN 阴影 props，无对应 utility）
  - reanimated `useAnimatedStyle()` 返回值（动画 worklet 不走 className）
  - 来自 props / state / `Dimensions.get()` / hook 返回的动态色或尺寸
  - CSS border-trick 三角箭头等少量几何 hack

> 当前项目的 token 清单、品牌色、已废弃 token 见 `.agents/PROJECT.md` §3。

### 11.2 TypeScript

- **`tsconfig` 已 `strict: true`**；新增代码不允许出现 `any`。需要"任意"语义时用 `unknown` + 类型守卫。
  *理由*：`any` 关闭整条调用链的类型检查，是潜在 bug 入口。

- **类型专用 import 必须用 `import type`**：`import type { Foo } from '...'`，而不是 `import { Foo }`。
  *理由*：被 TS 编译期完全擦除，零运行时副作用，避免循环依赖与 tree-shaking 问题。
  *例外*：当一个文件同时需要类型与值时，可写 `import { foo, type Foo } from '...'`。

- **不为类型而类型**：能从函数体 / 默认值推断的，不要显式标注（返回类型、内部变量）。React 组件 props 必须显式 interface。
  *理由*：冗余类型是噪声，且会拖慢类型推断。

- **Props interface 写在文件顶部，默认不 `export`**。只有真正外部消费才 export，避免单向变成 public API。

- **平台特定文件用后缀**：`.ios.tsx` / `.android.tsx` / `.web.ts` / `.native.ts`。Metro 按平台自动解析。

- **不写 JSDoc 大段块注释**；类型即文档。需要说明"为什么"时用 1 行 `//` 注释。

### 11.3 状态管理

- **三类状态分别用三种工具**（见 §7 决策树）。

- **Zustand 必须用 selector 模式**：`useStore((s) => s.x)`，不要 `const store = useStore()` 全量订阅。
  *理由*：全量订阅 = 任何字段变化都 re-render，性能黑洞。

- **Zustand persist store 必须有 `_hasHydrated` 标记**，UI 在未 hydrated 前展示 fallback。
  *理由*：AsyncStorage 异步加载，否则首帧用默认值会闪烁。

- **表单 state 不进 Zustand**。临时输入、modal 草稿、列表筛选这类用 `useState`。
  *理由*：表单生命周期跟组件绑定；进 store 会污染全局快照。

- **React Query key 用元组**：`['products', filters]`、`['order', id]`。
  *理由*：序列化稳定、便于 partial invalidate。

- **不要用 React Context 管 state**：跨页持久用 Zustand，组件树上下文传值用 props / Provider only when really needed（如 ThemeProvider 这种 framework-level）。
  *理由*：Context 全树重渲染，且没有 selector；Zustand 默认就比 Context 更高效。

### 11.4 Bug 修复流程

- **先找根因，禁止盲修症状**。能用一句话描述"哪个变量在哪个 effect 被错误赋值"才能开始动代码。
  *理由*：症状修复留 root cause 在原地，迟早再爆。

- **一次改一个变量**。多个可疑点不要一起改；改一处验一处。
  *理由*：并行改动无法定位真正 work 的那个，回归时找不回。

- **三次失败必停**。同一 bug 第 3 次尝试失败后，**禁止发起第 4 次盲修**，停下来质疑：是否假设错、架构错、复现条件理解错。需要时升级讨论。
  *理由*：3 次失败 ≈ 心智模型与代码现实脱节，再试只是积累乱七八糟的副作用。

- **Bug 修复 commit 只动相关代码**。不要顺手重构、不要顺手改格式、不要顺手 rename 无关变量。
  *理由*：bug fix 要可独立 revert，混入无关改动会破坏 revert 的精度。

- **commit message 必须写清 root cause**，不要只说"修复 xx 问题"。模板：
  ```
  fix(<scope>): <症状的一句话描述>

  Root cause: <哪个变量 / 哪个调用 / 哪个 effect 在什么条件下做错了什么>
  ```
  具体范例见 `.agents/PROJECT.md` §6。

- **找 regression 用 `git log -p <file>`** 加二分回溯，先定位**引入提交**再决定怎么改，不要在最新代码上凭直觉打补丁。

---

## 12. 扩展信号（Scaling Signals）

当前已采用 Feature-Sliced。如果未来出现下列信号，再考虑进一步精细化：

| 信号 | 应对 |
|---|---|
| 单个 feature 文件数 > 30 | 在 feature 内引入 `widgets/` 或 `entities/` 子分层（趋近完整 FSD） |
| 多 feature 都需要某领域规则（如优惠券计算） | 抽 `@shared/domain/<rule>.ts` 纯函数库 |
| 出现真正的领域聚合行为（订单状态机、退款流程） | 引入显式 Use Case 层 `@features/<f>/use-cases/*.ts`（趋近 Clean Architecture） |
| HTTP / SecureStore 等基础设施需要切实现 | 给 `HttpAuthBridge` 那种 Port 模式更多兄弟接口（趋近完整 Hexagonal） |
| 团队规模 > 3 人或 feature 数 > 15 | 评估单 monorepo 是否仍合适 |

每次架构升级前先核对这张表，避免过度设计。

---

## 13. 依赖校验工具（dependency-cruiser）

§1 的依赖矩阵不只是文档——已经翻译成 `.dependency-cruiser.cjs` 的规则，会拦下违规 PR。

**触发**：

```bash
yarn arch            # 校验全部规则（CI 必跑）
yarn arch:graph      # 生成 docs/architecture.svg 依赖图（需本机有 graphviz `dot`）
yarn arch:html       # 生成 docs/architecture-violations.html 报告
```

**当前覆盖的硬规则**（全部 error 级，违反即拦）：

1. `no-circular` — 禁止循环依赖
2. `no-non-package-json` — 禁止使用 package.json 未声明的 npm 包
3. `no-cross-feature-import` — feature A 不能 import feature B（§9 bounded context）
4. `shared-not-from-features` — Shared Kernel 不反向依赖 features（type-only 例外）
5. `shared-not-from-providers` — Shared Kernel 不依赖 composition root
6. `infra-not-from-features` — 系统适配层不依赖业务（type-only 例外）
7. `infra-not-from-shared-business` — 系统适配层不依赖 shared 业务（type-only 例外）
8. `infra-not-from-providers` — 系统适配层不依赖 composition root
9. `http-no-store-runtime` — `http.ts` 不能 runtime-import store / router / feature（§8 依赖反转）
10. `service-no-react-runtime` — Service 不能 runtime-import `react`（type-only 例外，`react-native` 运行时 API 允许）
11. `store-no-business-service` — Store 不能 runtime-import 业务 service
12. `route-only-uses-screens` — `app/<route>.tsx` 只能 re-export Screen，不能直接 import feature 内部模块

**新增规则**：编辑 `.dependency-cruiser.cjs`，runtime/type-only 区分用 `dependencyTypesNot: ['type-only']`。规则编写文档：<https://github.com/sverweij/dependency-cruiser/blob/main/doc/rules-reference.md>

**已知 `info` 级**（不拦，仅提示）：
- `no-orphans` — 未被引用文件（可能是死代码或新建未连接的）

---

## 附录：路径别名速查

| 别名 | 指向 |
|---|---|
| `@shared/*` | `src/shared/*` |
| `@features/*` | `src/features/*` |
| `@infra/*` | `src/infrastructure/*` |
| `@providers/*` | `src/providers/*` |
| `@app/*` | `app/*` |
| `@/*` | `./*`（兼容旧代码，引用 `__mocks__/`、`global.css` 等） |

**命令与 env 变量** 见根目录 `README.md`，本文不重复。
