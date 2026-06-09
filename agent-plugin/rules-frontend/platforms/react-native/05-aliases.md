# React Native — 路径别名

RN 项目推荐**完整别名集**——目录分层多（含 Expo 根 `app/` + `src/infrastructure/` + `src/providers/`），多别名让跨层引用更清晰。

| 别名 | 指向 |
|---|---|
| `@shared/*` | `src/shared/*` |
| `@features/*` | `src/features/*` |
| `@infra/*` | `src/infrastructure/*` |
| `@providers/*` | `src/providers/*` |
| `@app/*` | `app/*` |
| `@/*` | `./*`（兼容旧代码，引用 `__mocks__/`、`global.css` 等） |

## 配置

### `tsconfig.json`

```json
{
  "compilerOptions": {
    "baseUrl": ".",
    "paths": {
      "@shared/*":    ["src/shared/*"],
      "@features/*":  ["src/features/*"],
      "@infra/*":     ["src/infrastructure/*"],
      "@providers/*": ["src/providers/*"],
      "@app/*":       ["app/*"],
      "@/*":          ["./*"]
    }
  }
}
```

### Babel (`babel.config.js`)

Metro 在 RN 中是 transpiler——别名需要在 `babel-plugin-module-resolver` 同步：

```js
module.exports = {
  presets: ['babel-preset-expo'],
  plugins: [
    ['module-resolver', {
      root: ['./'],
      alias: {
        '@shared':    './src/shared',
        '@features':  './src/features',
        '@infra':     './src/infrastructure',
        '@providers': './src/providers',
        '@app':       './app',
        '@':          './',
      },
    }],
  ],
};
```

### Jest (`jest.config.js`)

测试 runner 单独的解析配置：

```js
module.exports = {
  moduleNameMapper: {
    '^@shared/(.*)$':    '<rootDir>/src/shared/$1',
    '^@features/(.*)$':  '<rootDir>/src/features/$1',
    '^@infra/(.*)$':     '<rootDir>/src/infrastructure/$1',
    '^@providers/(.*)$': '<rootDir>/src/providers/$1',
    '^@app/(.*)$':       '<rootDir>/app/$1',
    '^@/(.*)$':          '<rootDir>/$1',
  },
};
```

**铁律**：`tsconfig.json`、`babel.config.js`、`jest.config.js` **必须三向同步**。改一处忘改其他会出现"IDE 不报红但运行时找不到模块"或"测试找不到模块"等迷惑性 bug。

## 使用规则

```ts
// ✅ 跨层引用走对应别名
import { api } from '@shared/api/http';
import { useProductQuery } from '@features/product/hooks/use-product';
import { secureStorage } from '@infra/storage/secure-storage';
import { AppProvider } from '@providers/AppProvider';

// ✅ 路由薄壳引用 feature screen
// app/product/[id].tsx
export { default } from '@features/product/screens/ProductDetailScreen';

// ✅ feature 内部 → 用相对路径
import { fetchProduct } from '../api/product.api';

// ❌ 禁止：feature 内部用绝对路径
import { fetchProduct } from '@features/product/api/product.api';   // 在 features/product/ 内引用自己

// ❌ 禁止：跨 feature 走 barrel（RN 无 barrel）
import { useOrderQuery } from '@features/order';                    // 没有 index.ts；必须深路径

// ✅ 跨 feature 用直接路径（RN 平台策略）
import { useOrderQuery } from '@features/order/hooks/use-order';
```

详见 `core/05-bounded-context.md` RN barrel 策略。

## 为什么 RN 用多别名而 Web 用单别名？

- Web 目录扁平（仅 `src/` 下 4-5 个一级目录），单 `@/*` 链路清晰
- RN 有根 `app/`（Expo Router）+ `src/`（业务）双根，单 `@/*` 跨过两根容易混淆
- RN 项目通常有 `src/infrastructure/` 与 `src/features/` 平级，多别名让"角色"在 import 路径上一眼可见
