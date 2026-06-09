# Taro — 样式（Tailwind v4 + weapp-tailwindcss）

> 通用样式红线见 `core/08-red-lines.md` §5。

## 技术栈

| 组件 | 用途 |
|---|---|
| **Tailwind v4** | 原子工具类 |
| **`@tailwindcss/postcss`** | PostCSS 插件 |
| **`weapp-tailwindcss`** | 小程序 class 转义 + Webpack 插件 |
| **`weapp-tw patch`**（postinstall 脚本） | 给 Tailwind v4 打补丁让它兼容小程序 |

## 配置三件套

### 1. `src/app.css` —— Tailwind v4 入口

```css
@import url("tailwindcss/theme.css") layer(theme);
@import url("tailwindcss/utilities.css") layer(utilities);

@source "./**/*.{html,js,ts,jsx,tsx}";
```

- **不导入 `preflight`**（Tailwind 的 CSS reset 与小程序内置样式冲突）
- `@source` 告诉 Tailwind 扫描哪些文件提取 class
- 主题色 token 在 `theme` layer，工具类在 `utilities` layer

### 2. `postcss.config.js`

```js
module.exports = {
  plugins: {
    '@tailwindcss/postcss': {},
  },
};
```

### 3. `config/index.ts` —— Webpack 链挂 weapp-tailwindcss

```ts
import { UnifiedWebpackPluginV5 } from 'weapp-tailwindcss/webpack';

export default defineConfig<'webpack5'>(async (merge, {}) => {
  const baseConfig: UserConfigExport<'webpack5'> = {
    // ...
    compiler: {
      type: 'webpack5',           // ← 必须 webpack5，与 babel.config.js compiler 同步
      // ...
    },
    mini: {
      webpackChain(chain) {
        chain.merge({
          plugin: {
            install: {
              plugin: UnifiedWebpackPluginV5,
              args: [{
                appType: 'taro',
                rem2rpx: true,         // ← rem 自动转 rpx（小程序单位）
              }],
            },
          },
        });
      },
    },
  };
});
```

### 4. `package.json` postinstall

```json
{
  "scripts": {
    "postinstall": "weapp-tw patch"
  }
}
```

每次 `yarn install` 后自动给 Tailwind v4 打补丁，让它能跟 weapp-tailwindcss 协作。

---

## 类名书写

```tsx
// ✅ 普通使用，与 Web Tailwind 一致
<View className="flex items-center gap-4 px-6 py-4 bg-white">
  <Image className="w-20 h-20 rounded-lg" src={src} />
  <Text className="text-base text-gray-800">{name}</Text>
</View>

// ✅ 响应深色模式（如启用）
<View className="bg-white dark:bg-gray-900">

// ❌ 任意值色（无主题感知，不复用）
<View className="bg-[#3b82f6]">
```

## px 与 rpx 单位

小程序使用 `rpx`（responsive pixel）。`weapp-tailwindcss` 的 `rem2rpx: true` 让 Tailwind 的 `rem` 单位自动转 `rpx`：

```
Tailwind 类  →  rem 值      →  转 rpx
p-4          →  1rem         →  16rpx？

实际转换比例由 Tailwind base + designWidth 决定。
```

默认 `designWidth: 750`，与小程序设计稿对齐。

## 主题切换（按需）

如启用深色模式：

```tsx
// providers/ThemeProvider.tsx
import { useThemeStore } from '@shared/stores/theme.store';
import Taro from '@tarojs/taro';

export function ThemeProvider({ children }: PropsWithChildren) {
  const mode = useThemeStore(s => s.mode);
  useEffect(() => {
    // 小程序通常用 document.documentElement.classList 加 'dark'（H5 端）
    // 小程序原生端需用 Taro.setTabBarStyle 等单独控制
  }, [mode]);
  return <>{children}</>;
}
```

小程序原生端的深色模式跟 H5 端机制不同：

- **H5**：标准 `dark:` className 配合 `data-theme="dark"` 或 `.dark` class 切换
- **微信小程序原生**：`<page-meta>` + `theme="dark"` 属性 + 系统暗色模式感知（API）
- **支付宝**：类似的页面元数据 API

跨端深色模式实现差异较大；推荐**先在 H5 端先实现**，原生端按业务必要性补齐。

## 跨端样式差异

| 平台 | 限制 |
|---|---|
| weapp / alipay / tt | 不支持 `position: fixed` 的部分场景；不支持 `:hover`；伪类有限；class 转义生效 |
| H5 | 标准 CSS，无限制 |
| RN | 实际跑 React Native StyleSheet，**部分 Tailwind class 不工作**；建议 RN 端单独检查 |

写组件时优先用**最小公约数**的 CSS 属性，避免单端特化。

## 禁止清单

- 禁止导入 Tailwind preflight（与小程序内置样式冲突）
- 禁止漏配 `weapp-tw patch` postinstall（class 转义失败）
- 禁止 `config/index.ts` 的 `compiler` 与 `babel.config.js` 的 `compiler` 不同步（必须都是 `'webpack5'`）
- 禁止业务 Component 直接用 px 单位（用 `rem` 让 weapp-tw 转 rpx；或显式 `rpx` 单位）
- 禁止主题色硬编码 hex（用 Tailwind 自定义 token）
- 禁止 className + `style={{ }}` 双写同一属性
