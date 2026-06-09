# Web SPA — 新增业务清单

1. 先判断层级：页面编排进 `pages`，用户动作进 `features`，稳定业务对象进 `entities`，大块组合进 `widgets`，无业务归属进 `shared`。
2. 新 feature 建最小目录：`api`、`queries`，按需补 `components/hooks/stores/lib`。
3. 新 page 放 `src/pages/<page>/`，在 `src/app/router.tsx` lazy 引入。
4. DTO 放 `api`，ViewModel / UI 类型放 `types.ts` 或由 hook 返回值推断。
5. 如需跨 slice 暴露能力，维护 public API named exports。
6. 补映射、状态、权限、错误、测试和浏览器验证。

禁止复制其他 slice 后大面积改名；先建最小骨架，再按需求补目录。
