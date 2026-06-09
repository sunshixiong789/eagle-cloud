# 性能预算

## 通用红线

- 循环依赖为 0。
- 首屏只加载必要 feature，路由级懒加载。
- 列表使用分页、虚拟列表或增量加载。
- 图片压缩、懒加载、按尺寸请求。
- 生产代码不保留大量 `console.log`。

## 平台关注点

- Web：bundle size、FCP/LCP、路由 chunk、polyfill 精准性。
- React Native：首屏 JS 执行、列表虚拟化、图片缓存、Hermes profiling。
- Taro：主包体积、分包、`setData` 频率、跨端样式转换体积。

## 必查场景

- 新增大依赖、图表、富文本、地图、视频、支付 SDK。
- 单页面/单文件持续膨胀。
- 页面出现长列表、轮询、WebSocket、复杂表单。
- RN/Taro 引入 Web 生态大包。

## 禁止清单

- Web 全量引入 lodash/moment/polyfill。
- RN 列表 1000+ 项不用虚拟化。
- Taro 主包直接引用分包代码。
- 为性能问题盲目 `memo/useMemo/useCallback`，不先定位热点。
