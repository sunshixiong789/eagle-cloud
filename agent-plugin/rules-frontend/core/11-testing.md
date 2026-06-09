# 测试策略

## 必测

- DTO -> ViewModel 映射。
- query hook / mutation 的成功、失败、invalidate。
- Zustand store action、persist hydration。
- 权限、401、错误展示、表单校验。
- 关键 Page/Screen 编排和回归 bug。

## 文件

- 测试与源码同目录：`*.test.ts` / `*.test.tsx`。
- 测试工具、mock server、render helper 放 `src/test/` 或项目既有测试目录。

## Runner

- Web：Vitest + Testing Library。
- RN：Jest + React Native Testing Library。
- Taro：优先纯函数 / hook 单测；页面行为用平台测试能力或手工验证补充。

## Mock 原则

- mock 网络边界，不 mock 被测 hook/store/component 本身。
- API mock 返回后端 DTO 形状，不返回 ViewModel。
- 时间、存储、路由、平台 API 用可控 mock。

## 禁止清单

- 只测快照不测行为。
- 为通过测试修改生产契约。
- E2E 替代单元测试覆盖业务分支。
- `sleep` 等待异步；使用 `waitFor` / fake timers。
