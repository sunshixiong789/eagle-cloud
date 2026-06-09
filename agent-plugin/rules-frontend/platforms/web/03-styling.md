# Web SPA — 样式三件套

> 通用样式红线（不双写、style 白名单）见 `core/08-red-lines.md` §5。本文只列 Web 的具体技术分工。

| 技术 | 用途 | 位置 |
|---|---|---|
| **原子工具（Tailwind / UnoCSS）** | **布局类** —— flex / grid / spacing / sizing / typography | className 直接写 |
| **CSS-in-JS**（antd-style / Emotion / styled-components） | **主题感知**样式 —— 需要 token 的色 / 阴影 / 圆角 / 状态变体 | 同名 `*.style.ts` 文件 |
| **CSS Modules**（`*.module.less` / `*.module.css`） | **遗留组件级**样式 —— 新代码尽量避免 | 同名 `*.module.less` 文件 |
| **`src/styles/global.*`** | 仅 `@font-face` 与基础 reset | `src/styles/global.css` 或 `index.css` |

## 选型决策

```
要写样式
├─ 是布局 / 间距 / 尺寸（无主题感知）→ Tailwind className
├─ 需要 token（主题色 / 阴影 / 圆角 / 状态变体）→ CSS-in-JS
└─ 是遗留组件 / 第三方组件覆写 → CSS Modules
```

## Tailwind 用法

```tsx
// ✅ 布局类直接写
<div className="flex items-center justify-between gap-4 px-6 py-4">
  <Avatar />
  <Title>欢迎</Title>
</div>

// ❌ 主题色用 Tailwind 任意值
<div className="bg-[#3b82f6]">   // 硬编码颜色，无主题感知
```

## CSS-in-JS（antd-style 示例）

```tsx
// LoginPage.style.ts
import { createStyles } from 'antd-style';

export const useStyles = createStyles(({ token, css }) => ({
  container: css`
    background: ${token.colorBgContainer};
    color: ${token.colorText};
    border-radius: ${token.borderRadius}px;
    box-shadow: ${token.boxShadow};
  `,
  primary: css`
    color: ${token.colorPrimary};
  `,
}));

// LoginPage.tsx
import { useStyles } from './LoginPage.style';

export default function LoginPage() {
  const { styles } = useStyles();
  return (
    <div className={styles.container}>
      <span className={styles.primary}>登录</span>
    </div>
  );
}
```

约定：
- 配套样式文件 `<Name>Page.style.ts` 或 `<Name>.style.ts`，与组件同目录
- `useStyles` 命名，named export
- token 取自 UI 库（antd 的 `theme.token`），不硬编码品牌色到 style 文件

## CSS Modules

```less
// LegacyWidget.module.less
.widget {
  width: 100%;
  padding: 12px;
}

.title {
  font-weight: 600;
}
```

```tsx
import styles from './LegacyWidget.module.less';

<div className={styles.widget}>
  <h3 className={styles.title}>...</h3>
</div>
```

**新代码尽量不用**——CSS Modules 不响应主题切换；改用 CSS-in-JS。

## 主题切换

通过 UI 库的 `ConfigProvider`（antd / Arco Design）顶层包裹，主题切换走 store + Provider 重渲染。

```tsx
// providers/ThemeProvider.tsx
import { ConfigProvider, theme as antdTheme } from 'antd';
import { useThemeStore } from '@/shared/stores/theme.store';

export function ThemeProvider({ children }: PropsWithChildren) {
  const mode = useThemeStore(s => s.mode);
  return (
    <ConfigProvider theme={{
      algorithm: mode === 'dark' ? antdTheme.darkAlgorithm : antdTheme.defaultAlgorithm,
      token: { colorPrimary: '#1677ff' },
    }}>
      {children}
    </ConfigProvider>
  );
}
```

业务 Component **不直接读 `mode`**——通过 `useStyles()` 中 `token` 已经响应主题切换。

## 禁止清单

- 禁止 `style={{ color: '#xxx', padding: 12 }}` 写静态样式（硬编码无复用）
- 禁止同一元素同时 `className="bg-blue-500"` + `style={{ backgroundColor: 'red' }}`
- 禁止 Tailwind 写主题感知色（用 CSS-in-JS）
- 禁止 `src/styles/global.*` 写业务样式
- 禁止全局 CSS（非 module 的 `*.less` / `*.css`）写在 feature 内
- 禁止业务 Component 直接读 `useThemeStore`（通过 `token` 已响应）
