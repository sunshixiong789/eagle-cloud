# 性能预算（通用）

> 跨平台性能红线 + 各平台特有指标。CI 必跑 size 检查，超预算即拦。

## 跨平台通用指标

| 指标 | 含义 | 推荐预算 |
|---|---|---|
| **单文件行数** | 单 `.ts` / `.tsx` 行数 | ≤ 500 行（超过考虑拆分） |
| **单 hook 返回字段数** | 一个 `use-x()` 暴露字段 | ≤ 8 个（超过拆 `useXQuery` + `useXMutation` + `useXDerived`） |
| **单 feature 文件数** | `src/features/<x>/` 文件总数 | ≤ 30 个（详见 `09-scaling.md`） |
| **循环依赖** | dependency-cruiser 检测 | 必须为 **0** |
| **首屏 JS 解析时间** | 启动到可交互 | 中端机型 ≤ 3 秒 |

## 平台特有指标

### Web

| 指标 | 推荐预算 |
|---|---|
| **主包 (vendor + entry)** | gzip 后 ≤ 300KB |
| **单 feature chunk** | gzip 后 ≤ 100KB |
| **FCP**（First Contentful Paint） | ≤ 1.8s（4G 中端） |
| **LCP**（Largest Contentful Paint） | ≤ 2.5s |
| **TBT**（Total Blocking Time） | ≤ 200ms |
| **CLS**（Cumulative Layout Shift） | ≤ 0.1 |
| **TTI**（Time to Interactive） | ≤ 3.8s |

**首屏关键策略**：

- 路由级 `React.lazy`（详见 `platforms/web/02-routing.md`）
- 仅首屏依赖打入主包，其他走 chunk
- 图片懒加载 + WebP/AVIF 格式
- Font subset / `font-display: swap`

### React Native

| 指标 | 推荐预算 |
|---|---|
| **JS Bundle** | 压缩后 ≤ 3MB |
| **冷启动**（首次打开） | iOS ≤ 2.5s / Android ≤ 3.5s |
| **热启动**（后台切回） | ≤ 500ms |
| **JS 线程帧率** | 稳定 60fps（动画期间） |
| **UI 线程帧率** | 稳定 60fps |
| **APK / IPA 体积** | Android ≤ 30MB（不含资源）/ iOS ≤ 80MB |

**关键策略**：

- 启用 Hermes（Expo 默认）
- 动画用 reanimated 走 UI 线程，不阻塞 JS
- 列表用 `FlashList` 而非 `FlatList`
- 图片用 `expo-image` 走原生缓存

### Taro / 小程序

| 指标 | 推荐预算 |
|---|---|
| **微信主包** | ≤ 1.5MB（上限 2MB） |
| **微信分包** | 单包 ≤ 2MB（上限 20MB） |
| **支付宝主包** | ≤ 2MB（上限 3MB） |
| **首页 setData 时间** | ≤ 200ms |
| **页面切换时间** | ≤ 300ms |
| **首屏渲染时间** | ≤ 2s（含网络请求） |

**关键策略**：

- 主包**仅放首页 + 公共依赖**；其他业务全部分包
- `setData` 数据量 ≤ 256KB / 单次 ≤ 1024（数组长度）
- 长列表用虚拟滚动（`virtual-list` 组件）
- 图片走 CDN + 按设备像素请求合适分辨率
- 跳过不必要的 `useEffect` 触发 `setData`

## 监控点

在 CI 输出或 PR 模板中暴露：

```yaml
# .github/workflows/perf.yml
- name: Bundle Size Check
  run: |
    yarn build
    npx bundlesize       # 或 size-limit / source-map-explorer

- name: Web Vitals (Lighthouse CI)
  run: |
    npx @lhci/cli@latest autorun --collect.numberOfRuns=3
```

```js
// size-limit.json
[
  {
    "path": "dist/assets/index-*.js",
    "limit": "300 KB"
  },
  {
    "path": "dist/assets/feat-*.js",
    "limit": "100 KB"
  }
]
```

超预算阻断合并。

## 性能审计常用工具

| 平台 | 工具 |
|---|---|
| **Web** | Chrome DevTools Performance / Lighthouse / WebPageTest / `source-map-explorer` |
| **React Native** | Flipper / React Native Performance / Hermes Profiler |
| **Taro** | 微信小程序开发者工具的"Audits"面板 / "性能" 面板 / Trace |

## 优化决策树

```
慢在哪？
├─ 启动慢
│  ├─ Web → 主包过大？拆 chunk / 删依赖
│  ├─ RN → JS bundle 过大？启用 Hermes / 拆分懒加载
│  └─ Taro → 主包过大？分包 + 减少首屏依赖
├─ 列表卡
│  ├─ Web → 虚拟列表（react-window / @tanstack/virtual）
│  ├─ RN → FlashList / 拆 cell 避免重渲染
│  └─ Taro → virtual-list 组件 + 减少 setData
├─ 交互卡
│  ├─ Web → React.memo / 减少 re-render / useMemo 重计算
│  ├─ RN → 动画移到 UI 线程（reanimated）
│  └─ Taro → 拆 setData 批次 + 防抖
└─ 网络慢
   └─ React Query staleTime / prefetch / SWR 模式
```

## 禁止清单

- 禁止漏配 CI bundle size 检查（生产事故源头）
- 禁止单文件 > 1000 行不拆分
- 禁止 `import * from 'lodash'`（全量引入；用 `import debounce from 'lodash/debounce'` 或 `lodash-es`）
- 禁止生产代码包含 `console.log` 大量调试输出（用 `babel-plugin-transform-remove-console` 移除）
- 禁止 RN/Taro 项目用大体积包（如 `moment` → 用 `dayjs`；`axios` → 用 fetch wrapper）
- 禁止小程序主包**直接引用**分包内代码（运行时会找不到，且违反包体限制）
- 禁止 Web 项目把 polyfill 全量打入（按 `browserslist` 精确生成）
- 禁止 RN 列表 1000+ 项不用虚拟化（FlashList / FlatList 的 `getItemLayout`）
- 禁止图片直接放 `<img>` 不做懒加载与压缩（用 `loading="lazy"` + WebP）
