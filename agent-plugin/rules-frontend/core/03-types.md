# 类型三层（通用）

数据从后端到屏幕走三种形态，每一层禁止"越级穿透"：

| 层 | 位置 | 形态 | 命名 |
|---|---|---|---|
| **DTO** | `@features/<f>/api/*.api.ts` 顶部 | 贴后端原样 | 允许 `snake_case`、历史字段名 |
| **View Model** | `@features/<f>/queries/*.queries.ts` 或 `hooks/use-<f>.ts` 中 `select` / `transform` | 剔除冗余、扁平化 | `camelCase`、按 UI 需要展开 |
| **Component Props** | `@features/<f>/components/<X>.tsx` 顶部 interface | 最小必要 | 只接收原子值，不接收整段 DTO |

**铁律**：DTO 字段名（如 `created_at`、`commission_rate_start`、`user_role_id`）**不允许**出现在 Component props 类型或 JSX 表达式里。出现即说明 hook/query 没做映射。

## 三层穿透反例与改造

```ts
// ❌ 反例：DTO 字段直接渲染
function ProductCard({ product }: { product: ProductResponse }) {
  return <div>{product.commission_rate_start}%</div>;
}

// ✅ 正确：hook 内映射，组件只收原子值
// hooks/use-product.ts
function toViewModel(dto: ProductResponse): ProductViewModel {
  return {
    id: dto.id,
    name: dto.name,
    commissionRate: dto.commission_rate_start,
  };
}

// components/ProductCard.tsx
interface ProductCardProps {
  name: string;
  commissionRate: number;  // 只收原子值
}
```

## DTO 不必反向贴 ViewModel 名

**禁止**为了"美化"在 DTO 里把后端字段重命名。DTO 是后端契约的镜像；改名意味着 hook/query 层加一层"反向映射"，徒增复杂度。

```ts
// ❌ DTO 里改字段名
interface ProductResponse {
  commissionRate: number;   // 后端实际叫 commission_rate_start
}

// ✅ DTO 贴后端字段
interface ProductResponse {
  commission_rate_start: number;
}
// 在 hook 的 select 里转 ViewModel
```

## Component Props：最小必要

组件不接收整段 DTO 或 ViewModel；**只接收原子值**。

```ts
// ❌ 反例：组件耦合到整段 ViewModel
interface ProductCardProps {
  product: ProductViewModel;
}

// ✅ 正确：原子 props，可独立测试与复用
interface ProductCardProps {
  name: string;
  price: number;
  commissionRate?: number;
  onTap?: () => void;
}
```

例外：列表项渲染可以收整段 ViewModel（避免父组件解包成几十个 props 的样板），但**不收 DTO**。

## 类型独立文件 vs `types.ts`

- 单 feature 内若类型 < 3 个 → 直接放需要它的文件内，不抽出
- 类型 ≥ 3 个跨文件共享 → 抽到 feature 根 `types.ts`
- 类型 ≥ 10 个或语义分组明显 → 按域拆 `product.types.ts`、`order.types.ts`
- **禁止**建 `types/` 目录（feature 内）——`types.ts` 单文件就够了
- 跨 feature 通用类型放 `@shared/types/`
