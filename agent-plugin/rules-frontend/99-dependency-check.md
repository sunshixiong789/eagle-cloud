# 依赖校验工具（通用 CI 工具链）

> 适用所有前端项目（Web / RN / Taro）。core/ 与 platforms/ 里的规则要部分翻译为机器校验，CI 必跑。

## 三件套

任何前端项目至少要有三种自动检查，串成一个 `yarn lint`（或 `npm run lint`）命令：

| 工具 | 作用 |
|---|---|
| **格式器 + 代码 lint**（Biome / ESLint） | 风格 + 简单规则 |
| **Feature 边界检查**（`dependency-cruiser` / `eslint-plugin-boundaries` / 自写脚本） | bounded context + barrel + 跨 feature 规则 |
| **类型检查**（`tsc --noEmit`） | TS strict + `import type` 等 |

CI 三个都跑；本地 `yarn lint` 自动串。

---

## 方案一：dependency-cruiser（推荐）

最强大，能表达"feature 内部不能被外部直接 import"、"http.ts 不能 import store"等复杂规则。

### 安装

```bash
yarn add -D dependency-cruiser
yarn depcruise --init                    # 生成 .dependency-cruiser.cjs
```

### 关键规则示例

```js
// .dependency-cruiser.cjs
module.exports = {
  forbidden: [
    // 1. 禁止循环依赖
    {
      name: 'no-circular',
      severity: 'error',
      from: {},
      to: { circular: true },
    },
    // 2. 跨 feature 不能 import 内部模块（Web 走 barrel；RN/Taro 走直接路径）
    {
      name: 'no-cross-feature-deep-import',
      severity: 'error',
      from: { path: '^src/features/([^/]+)/' },
      to: {
        path: '^src/features/([^/]+)/.+',
        pathNot: '^src/features/$1/',          // 不限制自己
      },
      // 对 Web 项目额外加：to.path 应为 'src/features/<other>/index'（只允许 barrel）
      // 对 RN/Taro 项目：放开，不限制深度路径（因为禁用 barrel）
    },
    // 3. shared/api/http 不能 import store / router / feature
    {
      name: 'http-no-store-runtime',
      severity: 'error',
      from: { path: '^src/shared/api/http\\.ts$' },
      to: {
        path: '^src/(features|providers|app|shared/stores)/',
        dependencyTypesNot: ['type-only'],
      },
    },
    // 4. Service / API 文件不能 runtime-import React
    {
      name: 'service-no-react-runtime',
      severity: 'error',
      from: { path: '^src/(features/[^/]+/api|shared/api)/' },
      to: {
        path: '^(react|react-dom)$',
        dependencyTypesNot: ['type-only'],
      },
    },
    // 5. Store 不能 runtime-import 业务 service
    {
      name: 'store-no-business-service',
      severity: 'error',
      from: { path: '^src/(features/[^/]+/stores|shared/stores)/' },
      to: {
        path: '^src/features/[^/]+/api/',
        dependencyTypesNot: ['type-only'],
      },
    },
    // 6. Route / 路由文件只能 re-export Screen
    {
      name: 'route-only-uses-screens',
      severity: 'error',
      from: { path: '^(app/|src/pages/)' },
      to: {
        path: '^src/features/[^/]+/(?!screens|pages)',
        dependencyTypesNot: ['type-only'],
      },
    },
  ],
};
```

### 运行

```bash
yarn depcruise src --config .dependency-cruiser.cjs
```

CI 中加 `--validate` 标志严格模式，违规即 fail。

### 可视化

```bash
yarn depcruise --output-type dot src | dot -T svg > docs/architecture.svg
```

需本机有 graphviz（`brew install graphviz` / `apt install graphviz`）。

---

## 方案二：eslint-plugin-boundaries

ESLint 生态原生，规则配置略弱于 dependency-cruiser，但跟 eslint cache 一起跑速度快。

```bash
yarn add -D eslint-plugin-boundaries
```

```js
// eslint.config.js
import boundaries from 'eslint-plugin-boundaries';

export default [
  {
    plugins: { boundaries },
    settings: {
      'boundaries/elements': [
        { type: 'route',   pattern: 'app/**' },
        { type: 'page',    pattern: 'src/features/*/pages/**' },
        { type: 'screen',  pattern: 'src/features/*/screens/**' },
        { type: 'api',     pattern: 'src/features/*/api/**' },
        { type: 'store',   pattern: 'src/features/*/stores/**' },
        { type: 'feature', pattern: 'src/features/*' },
        { type: 'shared',  pattern: 'src/shared/**' },
      ],
    },
    rules: {
      'boundaries/element-types': ['error', {
        default: 'disallow',
        rules: [
          { from: 'route', allow: ['page', 'screen'] },
          { from: 'page', allow: ['screen', 'api', 'store', 'feature', 'shared'] },
          // ...
        ],
      }],
    },
  },
];
```

---

## 方案三：自写脚本（最简）

如果项目小、规则少，写个 50 行的 Node 脚本足够：

```js
// scripts/check-feature-boundaries.mjs
import { readFileSync } from 'node:fs';
import fg from 'fast-glob';

const files = await fg(['src/**/*.{ts,tsx}'], { ignore: ['**/node_modules/**'] });
let errors = 0;

for (const file of files) {
  const code = readFileSync(file, 'utf8');
  const featureMatch = file.match(/^src\/features\/([^/]+)\//);

  // 1. feature 内部用 @features/<self>/... 绝对路径
  if (featureMatch) {
    const selfFeature = featureMatch[1];
    const regex = new RegExp(`from ['"]@/?features/${selfFeature}/`, 'g');
    if (regex.test(code)) {
      console.error(`${file}: feature 内部禁止用 @features/${selfFeature}/...（改相对路径）`);
      errors++;
    }
  }

  // 2. 跨 feature 深 import（Web 项目启用此规则）
  // const crossFeatureDeep = /from ['"]@\/features\/([^'"/]+)\/[^'"]+['"]/g;
  // ...

  // 3. shared/api/http.ts 反向依赖 features
  if (file.endsWith('shared/api/http.ts')) {
    if (/from ['"]@\/?features\//.test(code) || /from ['"]@\/?features\//.test(code)) {
      console.error(`${file}: shared/api/http.ts 禁止 import @features/* (依赖反转)`);
      errors++;
    }
  }
}

if (errors > 0) {
  console.error(`\n${errors} 处违规`);
  process.exit(1);
}
```

```json
// package.json
{
  "scripts": {
    "lint:boundaries": "node scripts/check-feature-boundaries.mjs",
    "lint": "yarn lint:boundaries && eslint src && tsc --noEmit"
  }
}
```

---

## 推荐组合

| 项目规模 | 方案 |
|---|---|
| 小型（< 5 feature） | 自写脚本 + ESLint + tsc |
| 中型（5–15 feature） | dependency-cruiser + ESLint + tsc |
| 大型（> 15 feature） | dependency-cruiser + eslint-plugin-boundaries + ESLint + tsc + 架构图 SVG 自动生成 |

---

## CI 集成

每次 PR 必跑：

```yaml
# .github/workflows/lint.yml （示例）
name: Lint
on: [pull_request]
jobs:
  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: 20 }
      - run: yarn install --frozen-lockfile
      - run: yarn lint              # 串：格式器 + boundaries + tsc
```

任何一项失败即拦截合并。

---

## 平台差异：barrel 规则配置

Web 项目：

```js
// 强制走 barrel：跨 feature 必须 @/features/<other> 顶层路径
{
  name: 'cross-feature-must-use-barrel',
  from: { path: '^src/features/([^/]+)/' },
  to: {
    path: '^src/features/([^/]+)/(?!index\\.ts$).+',
    pathNot: '^src/features/$1/',
  },
},
```

RN / Taro 项目：

```js
// 禁用 barrel：检测到 index.ts 视为违规
{
  name: 'no-feature-index-barrel',
  from: {},
  to: {
    path: '^src/features/[^/]+/index\\.ts$',
  },
},
```

按项目栈选用对应规则。

---

## 未来扩展规则

下列规则没有现成开源插件，需自写或定制：

- Component 不能直接 `useQuery` / `useMutation`（需要扫描 JSX 节点的 hook 调用）
- API 文件不能 import React（自写或 dependency-cruiser 配 `pathNot`）
- 一个 hook 返回字段数 > 10 提示拆分（需要 AST 分析）

可在 `scripts/` 下逐步补充。
