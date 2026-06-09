# 测试策略（通用）

> 跨平台通用约束。具体 runner（Jest / Vitest / @testing-library / Detox / Maestro）按项目栈选。

## 测试金字塔

```
        E2E（少而精，关键用户路径）
       ─────────────
      集成测试（feature 级，hook + 编排）
     ─────────────────
    单元测试（pure function、reducer、mapper、formatter）
   ────────────────────
```

| 层级 | 数量级 | 速度 | 覆盖目标 |
|---|---|---|---|
| **单元** | 多（百级） | ms 级 | 纯函数、DTO→ViewModel 映射、reducer、formatter、validator |
| **集成** | 中（数十级） | 秒级 | hook 行为、Page 编排、mock API + 真 query client |
| **E2E** | 少（5–20 用例） | 分钟级 | 关键用户路径：登录、下单、支付 |

## 必测范围

强制要求测试的位置：

1. **`api/*.api.ts` 的 DTO 类型 + mapper** —— DTO→ViewModel 转换是核心契约
2. **`lib/*.ts` 纯函数** —— PKCE、token 解码、money 格式化等
3. **`queries/*.queries.ts` 的 `select` 转换** —— 集成测试用 `renderHook + QueryClientProvider`
4. **`stores/*.store.ts` action** —— Zustand 简单单测
5. **跨域逻辑** —— 编排 mutation + invalidate 的 Page 行为

**可选**：

- Page 组件的快照（Snapshot）—— 价值低、维护成本高，不强制
- 私有 hook 的实现细节 —— 通过 Page 集成测覆盖即可

## 文件组织

测试与源码**同目录共存**，不另建 `__tests__/`：

```
src/features/order/
├── api/
│   ├── order.api.ts
│   └── order.api.test.ts        # ✅ 同目录
├── queries/
│   ├── order.queries.ts
│   └── order.queries.test.ts
└── lib/
    ├── format-amount.ts
    └── format-amount.test.ts
```

测试文件命名：`{源文件名}.test.{ts,tsx}`。

E2E 测试单独建：

```
e2e/                              # 项目根目录
├── login.e2e.ts
├── checkout.e2e.ts
└── fixtures/
```

## 平台 runner 选型

| 平台 | 单元/集成 | E2E |
|---|---|---|
| **Web** | Vitest（推荐，与 Vite 同生态）或 Jest + `@testing-library/react` | Playwright（推荐）/ Cypress |
| **React Native** | Jest + `@testing-library/react-native` | Detox / Maestro |
| **Taro** | Jest + `@testing-library/react`（H5 端测试）；小程序端不建议跑 RTL（框架差异大） | 微信小程序开发者工具自动化 / Maestro（RN 端） |

## 单元测试模板

```ts
// src/features/order/lib/format-amount.test.ts
import { describe, it, expect } from 'vitest';   // 或 'jest'
import { formatAmount } from './format-amount';

describe('formatAmount', () => {
  it('正常金额格式化为两位小数 + 千分位', () => {
    expect(formatAmount(1234.5)).toBe('1,234.50');
  });

  it('零值返回 "0.00"', () => {
    expect(formatAmount(0)).toBe('0.00');
  });

  it('负数加 - 前缀', () => {
    expect(formatAmount(-99)).toBe('-99.00');
  });
});
```

## Hook 集成测试模板

```tsx
// src/features/order/queries/order.queries.test.ts
import { describe, it, expect, vi } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useOrderQuery } from './order.queries';
import * as api from '../api/order.api';

vi.mock('../api/order.api');

function wrap(children: React.ReactNode) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  );
}

describe('useOrderQuery', () => {
  it('返回 DTO 映射后的 ViewModel', async () => {
    vi.mocked(api.fetchOrder).mockResolvedValue({
      id: 1,
      order_no: 'A001',
      created_at: '2026-01-01',
    });

    const { result } = renderHook(() => useOrderQuery(1), { wrapper: wrap });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual({
      id: 1,
      orderNo: 'A001',
      createdAt: '2026-01-01',
    });
  });
});
```

## Mock 原则

- **Mock 外部依赖**（API、Storage、原生模块）；**不 mock 被测对象本身**
- **不 mock React Query / Zustand** —— 用真实实例配 mock 的底层 API/Storage
- **Component 测试用 RTL `user-event`** 模拟交互，不用 `fireEvent`（语义低）
- **测试中禁用 retry**：`queries: { retry: false }` 否则失败测试要等多次重试

## 覆盖率目标

| 指标 | 推荐目标 |
|---|---|
| `lib/` 纯函数 | ≥ 95% |
| `api/` DTO 映射 | ≥ 90% |
| `queries/` | ≥ 70% |
| `stores/` | ≥ 70% |
| `components/` Page | ≥ 40%（集成测覆盖）|
| **总体** | ≥ 60%（合理基线）|

**不追求 100% 覆盖率**——剩余 5%–40% 多半是 UI 边缘分支，测试 ROI 低。

## CI 集成

```yaml
# 单元 + 集成测试每次 PR 跑
- run: yarn test --coverage

# E2E 仅在 main 分支或 nightly 跑（耗时长）
- run: yarn test:e2e
  if: github.ref == 'refs/heads/main'
```

## 禁止清单

- 禁止测试单独建 `__tests__/` 目录（与源同目录最易维护）
- 禁止用 `Thread.sleep` / `setTimeout` 做时序控制（用 `waitFor`）
- 禁止真实网络调用（mock API 层）
- 禁止真实 Storage / Keychain 调用（mock `@infra/storage/`）
- 禁止快照测试覆盖业务逻辑（snapshot 仅用于 UI 视觉回归，且配合视觉对比工具）
- 禁止跳过测试不写说明（`it.skip(name, ...)` 必须加 reason 注释）
- 禁止 mock React Query 本身（mock 底层 API，让 query client 跑真实流程）
- 禁止测试代码包含真实凭证 / token
