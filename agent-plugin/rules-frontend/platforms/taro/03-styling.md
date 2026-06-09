# Taro — 样式

技术栈：Tailwind v4 + weapp-tailwindcss。通用样式红线见 `core/08-red-lines.md`。

## 必要配置

- `src/app.css`：Tailwind v4 入口。
- `postcss.config.js`：PostCSS 配置。
- `config/index.ts`：Webpack 链挂 weapp-tailwindcss。
- `package.json postinstall`：按项目实际需要处理小程序端补丁。

## 规则

- 布局、间距、字号优先 className。
- 主题模式保存原始 `system / light / dark`。
- H5 可用 `dark:`；小程序端结合 `page-meta` 和平台暗色能力。
- 动态尺寸、transform、平台不支持的样式用 style，但不要和 className 双写同一属性。

## 单位

- UI 设计稿跟随项目既有 px/rpx 转换策略。
- 不在同一组件混用无解释的 px/rpx。

## 禁止清单

- Tailwind 任意值色作为业务主题色。
- 全局 CSS 写业务样式。
- 忘记同步 Taro compiler / PostCSS / weapp-tailwindcss 配置。
