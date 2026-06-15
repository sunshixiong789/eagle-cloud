# React Native / Expo — 新增业务清单

1. 先判断层级：页面编排进 `pages`，用户动作进 `features`，稳定业务对象进 `entities`，大块组合进 `widgets`，无业务归属进 `shared`。
2. 新 feature 建最小目录：`api`、`queries`，按需补 `components/hooks/stores/lib`。
3. 新 page/screen 放 `src/pages/<page>/`，根 `app/` 路由文件一行 re-export。
4. 原生权限、存储、push、secure token 走 `infrastructure/`。
5. 补测试，并用模拟器/真机验证关键路径。

RN 禁用 barrel；不要为了跨 slice 复用直接 export 一整个 slice。
