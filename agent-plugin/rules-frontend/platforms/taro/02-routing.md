# Taro — 路由

Taro 路由是**框架强制文件式**——所有页面必须在 `src/app.config.ts` 的 `pages` 数组里注册，且物理文件在 `src/pages/<route>/index.{tsx,ts,js,jsx}`。

## 三件套：app.config.ts + pages/<x>/index.tsx + index.config.ts

### 1. 全局路由表（app.config.ts）

```ts
// src/app.config.ts
export default defineAppConfig({
  pages: [
    'pages/index/index',           // ← 必须放第一个，作为首页
    'pages/product/index',
    'pages/cart/index',
    'pages/order/detail/index',    // 嵌套路径同理
  ],
  window: {
    backgroundTextStyle: 'light',
    navigationBarBackgroundColor: '#fff',
    navigationBarTitleText: 'Eagle',
    navigationBarTextStyle: 'black',
  },
  tabBar: {
    list: [
      { pagePath: 'pages/index/index', text: '首页', iconPath: '...', selectedIconPath: '...' },
      { pagePath: 'pages/cart/index',  text: '购物车', iconPath: '...', selectedIconPath: '...' },
    ],
  },
});
```

- `pages` 数组顺序：**首页必须放第一个**（小程序框架约定）
- 每个路径对应 `src/pages/<...>/index.{tsx,jsx,ts,js}` 物理文件
- 路径**不带斜杠起头**，**不带文件扩展名**

### 2. 页面入口（pages/<x>/index.tsx）

**1 行 re-export，业务在 feature 内的 Screen**：

```tsx
// src/pages/product/index.tsx
export { default } from '@features/product/screens/ProductListScreen';
```

```tsx
// src/pages/order/detail/index.tsx —— 嵌套路径
export { default } from '@features/order/screens/OrderDetailScreen';
```

### 3. 页面级配置（pages/<x>/index.config.ts）

```ts
// src/pages/product/index.config.ts
export default definePageConfig({
  navigationBarTitleText: '商品列表',
  enablePullDownRefresh: true,
  backgroundColor: '#f5f5f5',
});
```

每个页面的标题、下拉刷新、背景色等单独配置。

---

## Screen 内读路由参数

Taro 路由参数走 query string：

```tsx
// src/features/product/screens/ProductDetailScreen.tsx
import { useRouter } from '@tarojs/taro';

export default function ProductDetailScreen() {
  const router = useRouter();
  const id = router.params.id;           // 字符串
  const numericId = Number(id);
  if (!Number.isFinite(numericId)) return <Text>参数错误</Text>;

  const { data, isPending } = useProductQuery(numericId);
  // ...
}
```

## 导航 API

```tsx
import Taro from '@tarojs/taro';

Taro.navigateTo({ url: '/pages/product/index?id=123' });  // 入栈
Taro.redirectTo({ url: '/pages/login/index' });           // 关闭当前 + 跳转
Taro.switchTab({ url: '/pages/index/index' });            // 跳 tabBar 页（必须在 tabBar 列表里）
Taro.navigateBack({ delta: 1 });                          // 返回
Taro.reLaunch({ url: '/pages/index/index' });             // 重启到指定页
```

**导航只在 Screen 内**，Component 不调 `Taro.navigateTo`。

## 分包

小程序主包大小限制（微信 weapp 2MB），大型项目需要分包：

```ts
// app.config.ts
export default defineAppConfig({
  pages: ['pages/index/index'],
  subPackages: [
    {
      root: 'pages-order',           // 分包根目录
      pages: [
        'detail/index',
        'list/index',
      ],
    },
  ],
});
```

物理路径：
```
src/
├── pages/
│   └── index/index.tsx
└── pages-order/                   # 分包目录与主包并列
    ├── detail/index.tsx
    └── list/index.tsx
```

分包内页面 1 行 re-export 规则同主包。

## 跨端路由差异

| 端 | 路由实现 |
|---|---|
| 小程序（weapp/alipay/tt/...） | 框架原生路由栈 |
| H5 | Taro 自带 SPA 路由（基于 history API），URL 形如 `#/pages/product/index?id=1` |
| RN | Taro 编译成 React Navigation Stack |

API 层（`Taro.navigateTo` 等）跨端一致——业务代码无需关心目标端。

## 禁止清单

- 禁止页面入口 `src/pages/<x>/index.tsx` 超过 1 行（必须 `export { default } from '@features/...'`）
- 禁止 `src/pages/<x>/index.tsx` 内 import feature 内部 hook/store/api
- 禁止首页放在 `pages` 数组的非首位
- 禁止跳转 tabBar 页用 `Taro.navigateTo`（必须 `switchTab`）
- 禁止 Component 内调 `Taro.navigateTo` / `Taro.redirectTo`（导航属于 Screen 编排）
- 禁止漏注册 `app.config.ts` 的 `pages` 数组——运行时会找不到页面
