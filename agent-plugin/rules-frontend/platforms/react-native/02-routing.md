# React Native — 路由

## Expo Router

- 根 `app/` 保持文件式路由。
- 路由文件为一行薄壳：`export { default } from '@features/x/screens/XScreen';`
- `_layout.tsx` 放 Provider、Stack/Tabs、全局 deep link 处理。
- Screen 解析 params、导航和编排；Component 不直接导航。

## React Navigation

少数项目可用集中式 React Navigation；仍保持 Screen 编排、Component 不导航、feature 内部不直接暴露路由实现。

## 禁止清单

- 把根 `app/` 迁到 `src/app/`。
- 路由文件 import hook/store/api。
- 跨 Screen 共享导航状态；用 URL params 或持久 store。
