# Web SPA — 新增 Feature 清单

1. 选 feature 词根，确认不是 shared 能力。
2. 在 `src/features/<feature>/` 建约定目录：`api`、`queries`、`screens`、按需 `components/hooks/stores/lib`。
3. DTO 放 `api`，ViewModel / UI 类型放 `types.ts` 或靠 hook 返回值推断。
4. Query key 和 hooks 放 `queries`。
5. Page 放 `screens`，Component 放 `components`。
6. 在 `src/app/router.tsx` lazy 引入 Page。
7. 如需跨 feature 暴露 API，维护顶层 barrel 的 named exports。
8. 补映射、状态、权限、错误、测试和手工页面验证。

禁止复制其他 feature 后大面积改名；先建最小骨架，再按需求补目录。
