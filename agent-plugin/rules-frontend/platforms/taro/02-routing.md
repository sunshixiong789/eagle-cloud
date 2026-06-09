# Taro — 路由

## 三件套

- `src/app.config.ts`：注册所有页面和 tabBar。
- `src/pages/<name>/index.tsx`：一行 re-export feature Screen。
- `src/pages/<name>/index.config.ts`：页面标题、导航栏等页面配置。

## 约定

- Screen 解析路由参数、导航和多 feature 编排。
- Component 不直接调用 `Taro.navigateTo` / `redirectTo`。
- tabBar 页面跳转用 `Taro.switchTab`。
- 分包页面在 `subPackages` 注册，主包不要直接 import 分包内部代码。

## 禁止清单

- 漏注册 `app.config.ts`。
- 页面入口写业务逻辑。
- tabBar 页用 `navigateTo`。
- 跨端路由参数格式不统一。
