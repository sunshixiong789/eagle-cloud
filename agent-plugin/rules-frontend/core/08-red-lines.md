# 红线规则

## TypeScript

- `tsconfig` 必须 `strict: true`；新增代码不用 `any`，需要任意值时用 `unknown` + 类型守卫。
- 类型专用导入使用 `import type`。
- React 组件 props 显式定义 interface；内部变量和返回值能推断则不重复标注。
- 不写大段 JSDoc；必要背景用短注释说明为什么。

## 状态

- 服务端状态进 React Query，不复制到 Zustand。
- React Query key 用元组或 key factory。
- Zustand 用 selector，不全量订阅。
- persist store 有 hydration 标记。
- 表单状态不进全局 store。
- 不用 React Context 管业务状态。

## HTTP / Auth

- HTTP 客户端单例，放 `shared/api/http`。
- token 存取走专门 lib 或 infrastructure adapter，不散落读写 localStorage / AsyncStorage / Taro storage。
- `shared/api` 不反向 import feature；需要 token 等能力时启动期注入。
- 业务代码不直接新建 fetch/axios 实例。

## Bug 修复

- 先复现和定位 root cause，再改代码。
- 一次改一个可疑点，改后验证。
- 同一 bug 三次失败后暂停，重新审视假设或升级讨论。
- bug fix 不夹带无关重构、格式化、rename。

## 样式

- 同一元素同一属性不同时用 className 和 style。
- 主题色走 token / className / 平台主题系统，不硬编码散落。
- `src/styles/global.*` 只放字体和 reset，不放业务样式。

## 包管理

- 一个项目只使用一种包管理器，提交对应 lock 文件。
- 禁止多个 lock 文件并存。
- 新依赖先确认体积、维护状态、平台兼容性和替代方案。
