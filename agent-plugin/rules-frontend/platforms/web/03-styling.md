# Web SPA — 样式

## 分工

- Tailwind / UnoCSS：布局、间距、尺寸、排版等无主题感知样式。
- CSS-in-JS：主题 token、颜色、阴影、圆角、状态变体。
- CSS Modules：仅用于遗留组件或第三方覆写，新代码尽量不用。

## 规则

- 主题色不用 Tailwind 任意值；通过 token / CSS-in-JS。
- 组件配套样式文件命名 `<Name>.style.ts`，与组件同目录。
- `src/styles/global.*` 只放字体、reset、Tailwind 入口。
- 不双写同一属性：不要同时 className 和 inline style 管同一件事。

## 禁止清单

- feature 内写全局 CSS。
- 业务 Component 直接读 theme store 拼颜色。
- `style={{ color: '#xxx', padding: 12 }}` 写静态业务样式。
