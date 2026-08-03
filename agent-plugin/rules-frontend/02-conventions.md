# 命名、Import 与类型三层

平台特有的别名形式（`@/*` vs `@features/*`）见 `04-platforms.md`。

## 文件命名

| 类别 | 风格 | 示例 |
|---|---|---|
| Page / Screen 路由级组件 | PascalCase + `Page` / `Screen` 后缀 | `LoginPage.tsx`、`OrderDetailScreen.tsx` |
| Slice 私有组件 | PascalCase | `ProductCard.tsx`、`AssignRoleModal.tsx` |
| Shared 组件 | Web：PascalCase 目录 + `index.tsx`；RN/Taro：kebab-case 文件 | `Footer/index.tsx`；`app-image.tsx` |
| Hook | kebab-case + `use-` 前缀 | `use-catalog.ts` |
| Store | kebab-case + `.store.ts` | `user.store.ts` |
| API | kebab-case + `.api.ts` | `auth.api.ts` |
| Queries / Keys | kebab-case + `.queries.ts` / `.keys.ts` | `user.queries.ts` |
| Schema | kebab-case + `.schema.ts` | `login.schema.ts` |
| Lib | kebab-case | `pkce.ts`、`token.ts` |
| 类型 | kebab-case 或 `types.ts` | `product.types.ts` |
| 测试 | 同源 + `.test.{ts,tsx}` | `auth.api.test.ts` |
| RN 平台特定 | `.ios.tsx` / `.android.tsx` / `.web.ts` / `.native.ts` | `icon-symbol.ios.tsx` |
| Web CSS-in-JS 配套 | 同名 + `.style.ts` | `LoginPage.style.ts` |

Shared 组件 Web 与 RN/Taro 风格不同是各自生态约定的历史结果，**项目内一致即可**，不强求跨平台统一。

## 函数 / 导出

- **Hook**：`useXxx`，named export
- **Zustand store**：`useXxxStore`，named export
- **Component**：PascalCase 函数；Page/Screen 用 **default export**，其余 named export
- **API 函数**：camelCase 动词起头 —— `fetchProducts`、`listBanners`、`createRole`
- **常量**：UPPER_SNAKE_CASE
- **类型 / 接口**：PascalCase

## Slice 词根

同一 slice 的文件共享词根，使 `grep <slice>` 能看到全部相关代码。

- 单数、kebab-case：`catalog`、`services-market`、`user-profile`
- 名词或名词短语，**不用动词**（用 `checkout` 不用 `checkout-flow`）
- 反映**业务语义**而非 UI 位置（不用 `home-tab`）
- 围绕**业务能力**而非技术分层（✅ `billing`；❌ `forms`、❌ `modals`）
- 同层 slice 平级，**不建嵌套 slice**（如 `auth/oauth`）——细分用文件名前缀或 slice 内子目录

### 何时新建 slice

| 需求 | 去处 |
|---|---|
| 新页面编排 | `pages/<page>` |
| 新用户动作 / 业务流程 | `features/<feature>` |
| 长期稳定业务对象 | `entities/<entity>` |
| 跨 feature 的大块业务 UI | `widgets/<widget>` |
| 无业务归属的能力 | `shared/` |

**不要新建**：只是一个私有工具函数或组件 → 放当前 slice 的 `lib/` / `components/`；只是跨 slice 复用的基础工具 → 上移 `shared/`。

## Import 规则

**同 slice 内**用相对路径 `../api/foo`、`./helpers`。
**禁止**用绝对路径自引（`@features/<self>/...`、`@/features/<self>/...`）——边界检查会拦。

**跨 slice** 只走 public API，barrel 策略见 `01-architecture.md`。

### 顺序

1. 外部包 —— `react`、`react-native`、`@tanstack/react-query`、第三方库
2. 共享 / 基础设施 —— `@shared/*`、`@infra/*`、`@app/*`
3. 其他 slice public API —— `@entities/*`、`@features/*`、`@widgets/*`、`@pages/*`
4. 本 slice 相对路径 —— `../api/...`、`./helpers`

类型专用 import 一律 `import type`。

---

# 类型三层

数据从后端到屏幕走三种形态，每层禁止"越级穿透"：

| 层 | 位置 | 形态 | 命名 |
|---|---|---|---|
| **DTO** | `features/<f>/api/*.api.ts` 顶部 | 贴后端原样 | 允许 `snake_case`、历史字段名 |
| **View Model** | `queries/*.queries.ts` 或 `hooks/use-<f>.ts` 的 `select` / `transform` | 剔除冗余、扁平化 | `camelCase`，按 UI 需要展开 |
| **Component Props** | `components/<X>.tsx` 顶部 interface | 最小必要 | 只接收原子值 |

**铁律**：DTO 字段名（`created_at`、`commission_rate_start`）**不允许**出现在 Component props 类型或 JSX 表达式里。出现即说明 hook/query 没做映射。

```ts
// ❌ DTO 字段直接渲染
function ProductCard({ product }: { product: ProductResponse }) {
  return <div>{product.commission_rate_start}%</div>;
}

// ✅ hook 内映射，组件只收原子值
function toViewModel(dto: ProductResponse): ProductViewModel {
  return { id: dto.id, name: dto.name, commissionRate: dto.commission_rate_start };
}

interface ProductCardProps {
  name: string;
  commissionRate: number;
}
```

## DTO 不反向贴 ViewModel 名

**禁止**为"美化"在 DTO 里重命名后端字段 —— DTO 是后端契约的镜像，改名意味着要加一层反向映射。

```ts
// ❌ interface ProductResponse { commissionRate: number }   后端实际叫 commission_rate_start
// ✅ interface ProductResponse { commission_rate_start: number }  转换放 select
```

## Component Props：最小必要

组件不接收整段 DTO 或 ViewModel，**只接收原子值** —— 可独立测试与复用。

例外：列表项渲染可以收整段 ViewModel（避免父组件解包成几十个 props），但**绝不收 DTO**。

## 类型文件组织

- 单 feature 内类型 < 3 个 → 直接放使用它的文件内
- ≥ 3 个跨文件共享 → 抽 feature 根 `types.ts`
- ≥ 10 个或语义分组明显 → 按域拆 `product.types.ts`
- **禁止**在 feature 内建 `types/` 目录，`types.ts` 单文件够了
- 跨 feature 通用类型放 `shared/types/`
