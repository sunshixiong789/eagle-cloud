# Web SPA — 路径别名

推荐**只保留一个根别名**。

| 别名 | 指向 |
|---|---|
| `@/*` | `./src/*` |

**理由**：别名多了反而模糊层级关系。Web 项目目录扁平（`src/app/`、`src/features/`、`src/shared/`、`src/providers/`），用一个 `@/*` 全部覆盖，链路最清晰。

## 配置

### `tsconfig.json`

```json
{
  "compilerOptions": {
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"]
    }
  }
}
```

### Vite (`vite.config.ts`)

```ts
import { defineConfig } from 'vite';
import path from 'node:path';

export default defineConfig({
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
});
```

### Webpack 5

```js
// webpack.config.js
const path = require('path');

module.exports = {
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
};
```

## 使用规则

```ts
// ✅ 跨 feature / 跨层引用 → 走 @/
import { http } from '@/shared/api/http';
import { useXQuery } from '@/features/x';            // barrel
import { BasicLayout } from '@/app/layouts/BasicLayout';
import { ThemeProvider } from '@/providers/ThemeProvider';

// ✅ feature 内部 → 用相对路径
import { fetchX } from '../api/x.api';
import { useXStore } from './stores/x.store';

// ❌ 禁止：feature 内部用 @/features/<self>/... 绝对路径
import { fetchX } from '@/features/x/api/x.api';     // 在 features/x/ 内引用自己

// ❌ 禁止：跨 feature 用深路径（必须走 barrel）
import { fetchY } from '@/features/y/api/y.api';     // 应走 @/features/y barrel
```

`scripts/check-feature-boundaries.mjs` 会拦上述两条违规（详见 `99-dependency-check.md`）。

## 如果项目坚持多别名

少数大型项目偏好显式别名（`@shared`、`@features`、`@providers`、`@app`）。**可以**，但要付出代价：

- 每个别名都要在 `tsconfig.json` + 构建器配置里**双向同步**（漏一个就有红线）
- 跨别名引用时 IDE 跳转链路更长
- 重构目录时改的地方更多

如果坚持：
```json
{
  "paths": {
    "@/*":          ["src/*"],
    "@shared/*":    ["src/shared/*"],
    "@features/*":  ["src/features/*"],
    "@providers/*": ["src/providers/*"],
    "@app/*":       ["src/app/*"]
  }
}
```

——但**推荐先用 `@/*` 一个别名**，等真的踩到痛点再扩。
