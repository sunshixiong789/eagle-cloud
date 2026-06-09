# Taro — 路径别名

Taro 推荐**完整别名集**（与 RN 风格一致）。

| 别名 | 指向 |
|---|---|
| `@/*` | `src/*` |
| `@shared/*` | `src/shared/*` |
| `@features/*` | `src/features/*` |
| `@providers/*` | `src/providers/*` |
| `@infra/*` | `src/infrastructure/*` |

## 配置（**必须双向同步**）

Taro 别名分两份配置——**两边漏一处就有红线**（IDE 报红或运行时找不到模块）：

### 1. `tsconfig.json`（TS 编译器 + IDE）

```json
{
  "compilerOptions": {
    "baseUrl": ".",
    "paths": {
      "@/*":          ["src/*"],
      "@shared/*":    ["src/shared/*"],
      "@features/*":  ["src/features/*"],
      "@providers/*": ["src/providers/*"],
      "@infra/*":     ["src/infrastructure/*"]
    }
  }
}
```

### 2. `config/index.ts`（Taro CLI / Webpack 解析）

```ts
import path from 'node:path';

export default defineConfig<'webpack5'>(async (merge, {}) => {
  const baseConfig: UserConfigExport<'webpack5'> = {
    // ...
    alias: {
      '@':          path.resolve(__dirname, '..', 'src'),
      '@shared':    path.resolve(__dirname, '..', 'src/shared'),
      '@features':  path.resolve(__dirname, '..', 'src/features'),
      '@providers': path.resolve(__dirname, '..', 'src/providers'),
      '@infra':     path.resolve(__dirname, '..', 'src/infrastructure'),
    },
    // ...
  };
});
```

**铁律**：两份配置改一处必同步改另一处。建议在 PR checklist 加一项"`tsconfig.json` paths 与 `config/index.ts` alias 已同步"。

## 使用规则

```ts
// ✅ 跨层引用走对应别名
import { api } from '@shared/api/http';
import { useProductQuery } from '@features/product/queries/product.queries';
import { secureStorage } from '@infra/storage/secure-storage';
import { AppProvider } from '@providers/AppProvider';

// ✅ 框架入口引用 feature screen
// src/pages/product/index.tsx
export { default } from '@features/product/screens/ProductListScreen';

// ✅ feature 内部 → 用相对路径
import { fetchProduct } from '../api/product.api';

// ❌ 禁止：feature 内部用绝对路径
import { fetchProduct } from '@features/product/api/product.api';   // 在 features/product/ 内引用自己

// ❌ 禁止：跨 feature 走 barrel（Taro 与 RN 一致，无 barrel）
import { useOrderQuery } from '@features/order';                    // 没有 index.ts

// ✅ 跨 feature 用直接路径
import { useOrderQuery } from '@features/order/queries/order.queries';
```

详见 `core/05-bounded-context.md` Taro barrel 策略。

## 为什么 Taro 用完整别名？

- 与 RN 风格一致（同样的 `@features/<f>/screens/...` 模式可以无缝迁移到 Taro RN 端构建）
- 业务（`@features/`）、共享（`@shared/`）、平台适配（`@infra/`）、Provider（`@providers/`）四种角色在 import 路径上一眼可见
- 多端构建时（H5 / weapp / RN 等）解析器各自工作，别名是构建器解析的关键路径——显式声明每个角色比单 `@/*` 更稳

## 测试 / Jest 配置（如启用）

当前 mini-program 项目未引入 jest/vitest。如未来引入：

```js
// jest.config.js
module.exports = {
  moduleNameMapper: {
    '^@/(.*)$':          '<rootDir>/src/$1',
    '^@shared/(.*)$':    '<rootDir>/src/shared/$1',
    '^@features/(.*)$':  '<rootDir>/src/features/$1',
    '^@providers/(.*)$': '<rootDir>/src/providers/$1',
    '^@infra/(.*)$':     '<rootDir>/src/infrastructure/$1',
  },
};
```

——加入第三份"必须同步"的配置。
