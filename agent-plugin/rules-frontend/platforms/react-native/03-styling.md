# React Native — 样式（NativeWind）

## 分工

- `className`：布局、间距、字号、主题色。
- `theme.X`：只传给 RN 原生 prop，例如 StatusBar、Navigation、图表库颜色。
- `style`：仅用于动态数值、transform、absolute 坐标、复杂 RN 专有样式。

## 主题

- 主题模式保存原始 `system / light / dark`。
- `nwColorScheme.set()` 传原始 mode，不传 resolved 后的 light/dark。
- 业务组件不要直接读 `useThemeStore.mode`。

## 禁止清单

- 用 `style={{ backgroundColor: theme.surface }}` 写主题色。
- 同一元素 className + style 双写颜色。
- Tailwind 任意值色 `bg-[#xxx]` 作为业务主题色。
