# Taro — 新增 Feature 清单

1. 选 feature 词根，确认不是 shared / infrastructure 能力。
2. 在 `src/features/<feature>/` 建最小目录：`api`、`queries`、`screens`，按需补 `components/hooks/stores/lib`。
3. Screen 放 `screens`。
4. 在 `src/pages/<route>/index.tsx` 一行 re-export Screen。
5. 在 `src/pages/<route>/index.config.ts` 写页面配置。
6. 在 `src/app.config.ts` 注册页面；tabBar 或分包同步配置。
7. API、Query、Store、ViewModel 按 core 规则分层。
8. 多端差异放 infrastructure 或条件编译文件。
9. 补测试，并至少用目标小程序/H5 环境手工验证。

常见漏项：忘注册 app.config、tabBar 跳转 API 用错、主包引用分包代码、Taro 配置和 TS alias 不同步。
