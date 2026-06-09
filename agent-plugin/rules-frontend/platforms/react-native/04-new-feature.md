# React Native / Expo — 新增 Feature 清单

1. 选 feature 词根，确认不是 shared / infrastructure 能力。
2. 在 `src/features/<feature>/` 建最小目录：`api`、`queries`、`screens`，按需补 `components/hooks/stores/lib`。
3. Screen 放 `screens`，根 `app/` 路由文件一行 re-export。
4. API、Query、Store、ViewModel 按 core 规则分层。
5. 原生权限、存储、push、secure token 走 `infrastructure/`。
6. 补测试，并用模拟器/真机验证关键路径。

RN 禁用 barrel；不要为了跨 feature 复用直接 export 一整个 feature。
