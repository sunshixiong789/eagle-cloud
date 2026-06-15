# Taro — 新增业务清单

1. 先判断层级：页面编排进业务 Page 层，用户动作进 `features`，稳定业务对象进 `entities`，大块组合进 `widgets`，无业务归属进 `shared`。
2. 新 feature 建最小目录：`api`、`queries`，按需补 `components/hooks/stores/lib`。
3. 业务 Page/Screen 放约定位置；`src/pages/<route>/index.tsx` 一行 re-export。
4. 在 `src/pages/<route>/index.config.ts` 写页面配置，并在 `src/app.config.ts` 注册页面。
5. 多端差异放 infrastructure 或条件编译文件。
6. 补测试，并至少用目标小程序/H5 环境手工验证。

常见漏项：忘注册 app.config、tabBar 跳转 API 用错、主包引用分包代码、Taro 配置和 TS alias 不同步。
